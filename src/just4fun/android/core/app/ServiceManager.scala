package just4fun.android.core.app

import just4fun.android.core.utils.BitState
import project.config.logging.Logger
import project.config.logging.Logger._

import scala.util.Try

/* Singleton */

private[app] object ServiceManager {
	var serviceMgr: ServiceManager = _
	var activeContext: AppServiceContext = _
}

/* ServiceManager */

class ServiceManager extends Loggable {
	import just4fun.android.core.app.ServiceManager._
	import just4fun.android.core.app.ServiceState._
	serviceMgr = this
	var app: App = _
	var activityMgr: ActivityManager = _
	lazy protected val contexts = collection.mutable.Set[AppServiceContext]()
	protected var visible = false

	def apply(app: App, aManager: ActivityManager) = {
		this.app = app;
		activityMgr = aManager
	}
	def activeState: BitState[ServiceState.Value] = if (activeContext == null) new BitState else activeContext.state
	def active = activeContext != null
	
	/* LIFE CYCLE */

	def onInit(): Unit = if (!active) {
		KeepAliveService.initialize(app)
		activeContext = new AppServiceContext(this)
		contexts.add(activeContext)
		app.onRegisterServices(activeContext)
		activeContext.init()
	}
	def onStart(): Unit = if (active) {
		if (activeState.hasNoAll(START, FAILED)) activeContext.start()
		else if (activeState.hasNoAll(STOP, FAILED) && !visible) {
			visible = true
			activeContext.onVisible(visible)
		}
	}
	def onHide(): Unit = if (active && activeState.hasNoAll(STOP, FAILED)) {
		visible = false
		activeContext.onVisible(visible)
	}
	def onStop(force: Boolean = false): Unit = if (active && activeState.hasNo(STOP) && (force || activeState.has(FAILED)  || !app.liveAfterStop)) {
		activeContext.stop()
		if (!app.liveAfterStop && !app.singleInstance) activeContext = null
	}
	def onFinalized(implicit context: AppServiceContext): Unit = {
		contexts.remove(context)
		Dependencies.remove(_.garbage)
		if (context == activeContext) activeContext = null
		logv("onFinalize", s"contexts.size = ${contexts.size };  active ? $active")
		if (contexts.isEmpty) {
			Dependencies.clear()
			if (activityMgr.onExited()) KeepAliveService.finalise()
			logw("APP", "            READY TO DIE")
		}
	}
	def isServiceStartFatalError(service: AppService, err: Throwable): Boolean = {
		Try { app.isServiceStartFatalError(service, err) }.getOrElse(true)
		// TODO tell UI
	}
}


// Small Problem: in multi context case if older context is stopping later than new and both context are linked to one (shared) parent service the new context can stuck waiting for old context's child service(s) to stop because parent should wait all children, even from another context.


/* SERVICE DEPENDENCIES */
//TODO weak ref
private[app] object Dependencies {
	private val relations = collection.mutable.Set[(AppService, AppService)]()

	def put(parent: AppService, child: AppService) = relations += (parent -> child)
	def remove(like: AppService => Boolean) =
		relations.retain { case (p, c) => !like(p) && !like(c) }
	def withChildren(parent: AppService, f: AppService => Unit) = 
		relations.foreach { case (p, c) => if (p == parent) f(c) }
	def withParents(child: AppService, f: AppService => Unit) =
		relations.foreach { case (p, c) => if (c == child) f(p) }
	def hasParent(child: AppService, cond: AppService => Boolean) =
		relations.exists { case (p, c) => c == child && cond(p) }
	def hasChild(parent: AppService, cond: AppService => Boolean) =
		relations.exists { case (p, c) => p == parent && cond(c) }
	def clear() = {
		if (relations.nonEmpty) loge(msg = s"Dependencies must be empty, but > ${relations.mkString(", ")}")
		relations.clear()
	}
}
