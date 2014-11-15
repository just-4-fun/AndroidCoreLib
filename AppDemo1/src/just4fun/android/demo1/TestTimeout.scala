package just4fun.android.demo1

import project.config.logging.Logger._
import just4fun.android.core.app.App
import just4fun.android.core.app.App._
import just4fun.android.core.async.Async._

import scala.util.Success

object TestTimeout extends Loggable{
	import App._
	config.singleInstance = true
	config.timeoutDelay = 15000
	def apply() = {
		new SERVICE_1().register()
		new SERVICE_2().register()
	}

	/*  service 1 */
	class SERVICE_1 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { startedStatus = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 25000) { stoppedStatus = Success(true) }// TEST STOP TIMEOUT
		}
	}

	/*  service 2 */
	class SERVICE_2 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { startedStatus = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { stoppedStatus = Success(true) }
		}
	}

}