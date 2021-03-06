package just4fun.android.demo1

import just4fun.android.core.app.AppServiceContext
import project.config.logging.Logger._
import just4fun.android.core.async.Async._

import scala.util.{Try, Success}

object TestDependShared2 extends Loggable{
	import Test._
	val msgs = List("S1 started", "S2 started", "S3 started", "S3 stopped", "S2 stopped", "S1 stopped")
	// start - stop
		messages = List(msgs(0), msgs(1), msgs(2), msgs(3), msgs(4), msgs(5) )
	// start - stop - start - stop
	messages = List(msgs(0), msgs(1), msgs(2), msgs(3), msgs(4), msgs(1), msgs(2), msgs(3), msgs(4), msgs(5) )
	def apply(implicit scxt: AppServiceContext) = {
		SERVICE_1.register()
		val s1 = new SERVICE_2().dependsOn(SERVICE_1).register()
		new SERVICE_3().dependsOn(SERVICE_1).dependsOn( s1).register()
	}

	/*  service 1 */
	object SERVICE_1 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { isStarted = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 3000) { isStopped = Success(true) }
		}
		override protected def isStarted: Try[Boolean] = {if (isStarted == Success(true)) TestMsg(msgs(0)); super.isStarted  }
		override protected def isStopped: Try[Boolean] = {if (isStopped == Success(true)) TestMsg(msgs(5)); super.isStopped }
	}

	/*  service 2 */
	class SERVICE_2 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { isStarted = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { isStopped = Success(true) }
		}
		override protected def isStarted: Try[Boolean] = {if (isStarted == Success(true)) TestMsg(msgs(1)); super.isStarted  }
		override protected def isStopped: Try[Boolean] = {if (isStopped == Success(true)) TestMsg(msgs(4)); super.isStopped }
	}

	/*  service 2 */
	class SERVICE_3 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { isStarted = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { isStopped = Success(true) }
		}
		override protected def isStarted: Try[Boolean] = {if (isStarted == Success(true)) TestMsg(msgs(2)); super.isStarted  }
		override protected def isStopped: Try[Boolean] = {if (isStopped == Success(true)) TestMsg(msgs(3)); super.isStopped }
	}

}