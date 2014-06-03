package just4fun.android.demo1

import just4fun.android.core.utils.Logger.Loggable
import just4fun.android.core.async._

object TestDependShared1 extends Loggable{
	import Test._
	val msgs = List("S1 started", "S2 started", "S3 started", "S3 stopped", "S2 stopped", "S1 stopped")
	// start - stop
//	messages = List("S1 started", "S2 started", "S3 started", "S3 stopped", "S2 stopped", "S1 stopped")
	// start - stop - start - stop
	messages = List("S1 started", "S2 started", "S3 started", "S3 stopped", "S2 stopped",
		"S2 started", "S3 started", "S3 stopped", "S2 stopped", "S1 stopped")
	def apply() = {
		SERVICE_1.register()
		new SERVICE_2().register()
		new SERVICE_3().dependsOn(SERVICE_1).register()
	}

	/*  service 1 */
	object SERVICE_1 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 3000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(msgs(0)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(msgs(5)); super.isStopped }
	}

	/*  service 2 */
	class SERVICE_2 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(msgs(1)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(msgs(4)); super.isStopped }
	}

	/*  service 2 */
	class SERVICE_3 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(msgs(2)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(msgs(3)); super.isStopped }
	}

}