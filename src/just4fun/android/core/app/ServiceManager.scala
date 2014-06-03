package just4fun.android.core.app

import just4fun.android.core.utils._
import Logger._
import just4fun.android.core.utils.BitState
import scala.util.Try

/* Singleton */

private[app] object ServiceManager {
	var serviceMgr: ServiceManager = _
	var current: AppServiceContext = _
	//	def isActiveContext(context: AppServiceContext, service: AppService) = serviceMgr.isActiveContext(context, service)
}

/* ServiceManager */

class ServiceManager extends Loggable {
	import AppState._
	import ServiceManager._
	ServiceManager.serviceMgr = this
	var app: App = _
	var activityMgr: ActivityManager = _
	lazy protected val contexts = collection.mutable.Set[AppServiceContext]()
	protected var visible = false
	// App state
	val state = new BitState[AppState.Value] {
		override def set(eVal: AppState.Value*): BitState[AppState.Value] = {
			logw("STATE", s"${" " * (60 - TAG.name.length) } [APP]:     [$toString]  >+   ${eVal.mkString(",") }")
			super.set(eVal: _*)
		}
		override def clear: BitState[AppState.Value] = {
			logw("STATE", s"${" " * (60 - TAG.name.length) } [APP]:     [$toString]  >   CLEAR")
			super.clear
		}
	}

	def apply(app: App, aManager: ActivityManager) = {
		this.app = app;
		activityMgr = aManager
	}
	def isSingleInstance = app.keepAliveAfterStop || app.singleInstance

	/* LIFE CYCLE */

	def onInit(): Unit = if (state.hasNoAll(INIT, FAILED)) {
		state.set(INIT)
		KeepAliveService.initialize(app)
		current = new AppServiceContext(this, app.timeoutDelay)
		contexts.add(current)
		app.onRegisterServices(current)
		current.init()
	}
	def onStart(): Unit = if (state.has(INIT)) {
		if (state.hasNo(START)) {
			state.set(START)
			current.start()
		}
		else if (!visible) {
			visible = true
			current.onVisible(visible)
		}
	}
	def onHide(): Unit = if (state.has(START)) {
		visible = false
		current.onVisible(visible)
	}
	def onStop(): Unit = {
		val nonStop = app.keepAliveAfterStop && state.hasNoAll(FAILED, EXITING)
		if (nonStop || state.hasNo(START) || state.has(STOP)) return
		if (isSingleInstance) state.set(STOP) else state.clear()
		current.stop()
	}
	def setExiting(): Unit = state.set(EXITING)

	def onFinalized(implicit inst: AppServiceContext): Unit = {
		contexts.remove(inst)
		Dependencies.remove(_.state >= ServiceState.STOPPED)
		if (inst == current) current = null
		logv("onFinalize", s"stoppingInstances= ${contexts.size };  current ? ${current == null };   appStt= $state")
		if (contexts.isEmpty && current == null) {
			Dependencies.clear()
			if (state.has(FAILED)) state.setOnly(FAILED) else state.clear()
			activityMgr.onExited() // May call onInit on some condition
			if (state.isZero || state.hasOnly(FAILED)) KeepAliveService.finalise()
		}
	}

	def onServiceStartFailed(service: AppService, err: Throwable): Unit = {
		if (Try { app.onServiceStartFailed(service, err) }.getOrElse(true)) {
			state.set(FAILED)
			onStop()
			// TODO tell UI
		}
	}
	// TODO Problem: in multi context case if older context is stopping later than new and both context are linked to shared service(s) the new context can stuck waiting old context's dependent service to stop
}


/* SERVICE DEPENDENCIES */
//TODO weak ref
private[app] object Dependencies {
	private val relations = collection.mutable.Set[(AppService, AppService)]()

	def put(up: AppService, down: AppService) = relations += (up -> down)
	def remove(p: AppService => Boolean) = relations.retain { case (u, d) => !p(u) && !p(d) }
	def withDependents(s: AppService, f: AppService => Unit) = relations.foreach { case (u, d) => if (u == s) f(d) }
	def isDependent(s: AppService, p: AppService => Boolean) = relations.exists { case (u, d) => d == s && p(u) }
	def hasDependent(s: AppService, p: AppService => Boolean) = relations.exists { case (u, d) => u == s && p(d) }
	def clear() = relations.clear()
}
