package just4fun.android.core.app

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import scala.ref.WeakReference
import just4fun.android.core.utils._
import Logger._
import just4fun.android.core.async._


class ActivityManager extends ActivityLifecycleCallbacks with Loggable {
	import ActivityState._
	var app: App = _
	var serviceMgr: ServiceManager = _
	protected var activity: WeakRefActivity = WeakRefActivity(null)
	protected val activities = collection.mutable.WeakHashMap[Activity, Boolean]()
	protected var state: ActivityState.Value = NONE
	var reconfiguring = false

	def apply(app: App, sManager: ServiceManager) = {
		this.app = app; serviceMgr = sManager
	}
	def exit() {
		serviceMgr.setExiting()
		val a = activity.get.getOrElse(null)
		activities.foreach {case (_a, b) => if (_a != a && !_a.isFinishing && !_a.isDestroyed) _a.finish()}
		if (a != null && !a.isFinishing && !a.isDestroyed) activity().finish()
		else serviceMgr.onStop()
	}
	/** All instances have finalized */
	def onExited(): Unit = {
		activities.clear()
		if (activity.notEmpty) activities += (activity() -> true)
		app.onExited()
		//Start new instance if activity is visible
		if (state >= CREATED && state <= RESUMED) {
			serviceMgr.onInit()
			if (state == RESUMED) post("Start Ui") {serviceMgr.onStart()}(UiThreadContext)
		}
	}


	override protected def onActivityCreated(a: Activity, savedState: Bundle): Unit = onStateChange(a, CREATED)
	override protected def onActivityStarted(a: Activity): Unit = onStateChange(a, STARTED)
	override protected def onActivityResumed(a: Activity): Unit = onStateChange(a, RESUMED)
	override protected def onActivityPaused(a: Activity): Unit = onStateChange(a, PAUSED)
	override protected def onActivityStopped(a: Activity): Unit = onStateChange(a, STOPPED)
	override protected def onActivityDestroyed(a: Activity): Unit = onStateChange(a, DESTROYED)
	override protected def onActivitySaveInstanceState(a: Activity, savedState: Bundle): Unit = ()

	protected def onStateChange(a: Activity, newStt: ActivityState.Value): Unit = {
		newStt match {
			case CREATED => activity = WeakRefActivity(a)
				activities += (a -> true)
				serviceMgr.onInit()
			case STARTED => activity = WeakRefActivity(a)
			case RESUMED => activity = WeakRefActivity(a)
				reconfiguring = false
				serviceMgr.onStart()
			case PAUSED => if (a.isChangingConfigurations) reconfiguring = true
			case STOPPED => if (!reconfiguring && !a.isFinishing && activity =? a) serviceMgr.onHide()
			case DESTROYED => if (activity =? a && a.isFinishing) serviceMgr.onStop()
				if (activity =? a) activity = WeakRefActivity(null)
				activities -= a
			case _ =>
		}
		val isCurrent = activity.isEmpty || activity =? a
		if (isCurrent) state = newStt
		val reason = if (a.isFinishing) "finishing" else if (reconfiguring) "reconfiguring" else ""
		logv("onActivityStateChange", s"Activity= ${if (isCurrent) "CURR" else "       " };  state= ${newStt.toString };  reason= ${if (nonEmpty(reason)) reason }")
	}


	/* WeakRefActivity HELPER */
	case class WeakRefActivity(var a: Activity) extends WeakReference[Activity](a) {
		a = null
		def =?(v: Activity): Boolean = { if (get.isEmpty) false else get.get == v }
		def isEmpty = get.isEmpty
		def notEmpty = !isEmpty
	}

}
