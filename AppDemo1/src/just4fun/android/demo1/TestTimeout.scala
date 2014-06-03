package just4fun.android.demo1

import just4fun.android.core.utils.Logger.Loggable
import just4fun.android.core.app.App
import just4fun.android.core.app.App._
import just4fun.android.core.async._

object TestTimeout extends Loggable{
	import App._
	app.singleInstance = true
	app.timeoutDelay = 15000
	def apply() = {
		new SERVICE_1().register()
		new SERVICE_2().register()
	}

	/*  service 1 */
	class SERVICE_1 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 25000) { _stopped = true }// TEST STOP TIMEOUT
		}
	}

	/*  service 2 */
	class SERVICE_2 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { _stopped = true }
		}
	}

}