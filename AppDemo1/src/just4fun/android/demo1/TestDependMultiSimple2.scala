package just4fun.android.demo1

import just4fun.android.core.app.AppServiceContext
import project.config.logging.Logger._
import just4fun.android.core.async.Async._

import scala.util.{Success, Try}

object TestDependMultiSimple2 extends Loggable{
	import Test._
	messages = List("S1 started", "S2 started", "S3 started", "S3 stopped", "S2 stopped", "S1 stopped")
	def apply(implicit scxt: AppServiceContext) = {
		val s1 = new SERVICE_1()register()
		val s2 = new SERVICE_2()register()
		new SERVICE_3().dependsOn(s1).dependsOn(s2).register()
	}

	/*  service 1 */
	class SERVICE_1 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 3000) { isStarted = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { isStopped = Success(true) }
		}
		override protected def isStarted: Try[Boolean] = {if (isStarted == Success(true)) TestMsg(messages(0)); super.isStarted  }
		override protected def isStopped: Try[Boolean] = {if (isStopped == Success(true)) TestMsg(messages(5)); super.isStopped }
	}

	/*  service 2 */
	class SERVICE_2 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { isStarted = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { isStopped = Success(true) }
		}
		override protected def isStarted: Try[Boolean] = {if (isStarted == Success(true)) TestMsg(messages(1)); super.isStarted  }
		override protected def isStopped: Try[Boolean] = {if (isStopped == Success(true)) TestMsg(messages(4)); super.isStopped }
	}

	/*  service 2 */
	class SERVICE_3 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { isStarted = Success(true) }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { isStopped = Success(true) }
		}
		override protected def isStarted: Try[Boolean] = {if (isStarted == Success(true)) TestMsg(messages(2)); super.isStarted  }
		override protected def isStopped: Try[Boolean] = {if (isStopped == Success(true)) TestMsg(messages(3)); super.isStopped }
	}

}