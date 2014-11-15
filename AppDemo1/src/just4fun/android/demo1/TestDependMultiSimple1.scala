package just4fun.android.demo1

import project.config.logging.Logger._
import just4fun.android.core.async.Async._

import scala.util.{Success, Try}

object TestDependMultiSimple1 extends Loggable{
	import Test._
	messages = List("S1 started", "S2 started", "S3 started", "S3 stopped", "S2 stopped", "S1 stopped")
	def apply() = {
		val s1 = new SERVICE_1()register()
		new SERVICE_2().dependsOn(s1).register()
		new SERVICE_3().dependsOn(s1).register()
	}

	/*  service 1 */
	class SERVICE_1 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { startedStatus = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { stoppedStatus = Success(true) }
		}
		override protected def isStarted(canceled: Boolean): Try[Boolean] = {if (startedStatus == Success(true)) TestMsg(messages(0)); super.isStarted(startCanceled)  }
		override protected def isStopped(): Try[Boolean] = {if (stoppedStatus == Success(true)) TestMsg(messages(5)); super.isStopped() }
	}

	/*  service 2 */
	class SERVICE_2 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { startedStatus = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { stoppedStatus = Success(true) }
		}
		override protected def isStarted(canceled: Boolean): Try[Boolean] = {if (startedStatus == Success(true)) TestMsg(messages(1)); super.isStarted(startCanceled)  }
		override protected def isStopped(): Try[Boolean] = {if (stoppedStatus == Success(true)) TestMsg(messages(4)); super.isStopped() }
	}

	/*  service 2 */
	class SERVICE_3 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { startedStatus = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { stoppedStatus = Success(true) }
		}
		override protected def isStarted(canceled: Boolean): Try[Boolean] = {if (startedStatus == Success(true)) TestMsg(messages(2)); super.isStarted(startCanceled)  }
		override protected def isStopped(): Try[Boolean] = {if (stoppedStatus == Success(true)) TestMsg(messages(3)); super.isStopped() }
	}

}