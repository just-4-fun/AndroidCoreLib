package just4fun.android.demo1

import just4fun.android.core.utils.Logger.Loggable
import just4fun.android.core.async._

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
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(messages(0)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(messages(5)); super.isStopped }
	}

	/*  service 2 */
	class SERVICE_2 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(messages(1)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(messages(4)); super.isStopped }
	}

	/*  service 2 */
	class SERVICE_3 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(messages(2)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(messages(3)); super.isStopped }
	}

}