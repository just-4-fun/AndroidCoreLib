package just4fun.android.demo1

import project.config.logging.Logger._
import just4fun.android.core.app.{AppServiceContext, App}
import just4fun.android.core.app.App._
import just4fun.android.core.async.Async._

import scala.util.Success

object TestSharedMulty extends Loggable{
	import App._
	var counter = 0
	def apply(implicit scxt: AppServiceContext) = {
		SHARED.register()
		new INSTANCE(counter).register()
		counter += 1
	}

	/*  service */
	object SHARED extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { isStarted = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { isStopped = Success(true) }
		}
	}

	/*  service */
	class INSTANCE(n: Int) extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { isStarted = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop",  if (n%2 == 0) 15000 else 5000) { isStopped = Success(true) }
		}
	}

}