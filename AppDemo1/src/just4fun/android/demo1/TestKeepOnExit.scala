package just4fun.android.demo1

import just4fun.android.core.app.{AppService, App}
import just4fun.android.core.async._
import just4fun.android.core.utils.Logger._

object TestKeepOnExit extends Loggable{
	import App._
	app.keepAliveAfterStop = true
	def apply() = {
		new SERVICE_1().register()
		post("EXIT", 20000) { logi("EXITING......."); exit() }
	}

	/*  service 1 */

	class SERVICE_1 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000, false) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("stop", 5000, false) { _stopped = true }
		}
	}

}

