package just4fun.android.demo1

import just4fun.android.core.app.{AppServiceContext, AppService, App}
import just4fun.android.core.async.Async._
import project.config.logging.Logger._

import scala.util.Success

object TestKeepOnExit extends Loggable{
	import App._
	config.liveAfterStop = true
	def apply(implicit scxt: AppServiceContext) = {
		new SERVICE_1().register()
		post("EXIT", 20000) { logi("EXITING......."); exit() }
	}

	/*  service 1 */

	class SERVICE_1 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000, false) { isStarted = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("stop", 5000, false) { isStopped = Success(true) }
		}
	}

}

