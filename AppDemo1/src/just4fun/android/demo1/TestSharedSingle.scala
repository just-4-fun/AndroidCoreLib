package just4fun.android.demo1

import just4fun.android.core.utils.Logger.Loggable
import just4fun.android.core.app.App
import just4fun.android.core.app.App._
import just4fun.android.core.async._

object TestSharedSingle extends Loggable{
	import App._
	app.singleInstance = true
//	app.singleInstance = false
	def apply() = {
		SHARED.register()
		new INSTANCE().register()
	}

	/*  service */
	object SHARED extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { _stopped = true }
		}
	}

	/*  service */
	class INSTANCE extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { _stopped = true }
		}
	}

}