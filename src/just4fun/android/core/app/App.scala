package just4fun.android.core.app

import android.app.Application
import just4fun.android.core.utils._
import Logger._
import android.content.Context

object App {

	var app: App = _
	val activityMgr: ActivityManager = new ActivityManager
	val serviceMgr: ServiceManager = new ServiceManager

	private def apply(a: App) {
		app = a
		serviceMgr(app, activityMgr)
		activityMgr(app, serviceMgr)
		app.registerActivityLifecycleCallbacks(activityMgr)
	}

	/* PUBLIC USAGE */

	def apply(): Context = app

	def exit() = activityMgr.exit()

	def findService[S <: AppService](id: String)(implicit cxt: AppServiceContext = null): Option[S] = {
		val _cxt = if (cxt == null) ServiceManager.current else cxt
		cxt.findService(id)
	}
	def withService[S <: AppService](id: String)(f: S => Unit)(implicit cxt: AppServiceContext = null): Unit = {
		findService[S](id)(cxt).foreach(s => f(s))
	}
}


abstract class App extends Application with AppConfig with Loggable /*TODO with Inet with Db etc*/ {
	App(this)
	// TODO ? move to onCreate to avoid usage before Application instance ready
//	override def onCreate(): Unit = App(this)


	/** The good place to register services */
	def onRegisterServices(implicit sm: AppServiceContext): Unit
	/**
	 * @param service
	 * @return true - if App should come into FAILED state; false - if App should be considered working
	 */
	def onServiceStartFailed(service: AppService, err: Throwable): Boolean = {
		logv("onServiceStartFailed", s"${service.ID};  Error: ${err.getMessage}")
		service match {
			case _ => false // decide fail App or not
		}
	}
	/**

	 */
	def onExited(): Unit = ()
}
