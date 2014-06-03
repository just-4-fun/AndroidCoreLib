package just4fun.android.demo1

import just4fun.android.core.async._
import just4fun.android.core.utils.Logger.Loggable
import just4fun.android.core.async._
import just4fun.android.core.utils.Logger.Loggable
import just4fun.android.core.app.FirstInLastOutFeature

object TestDependFILO1 extends Loggable{
	import Test._
	val msgs = List("FILO started", "SHD started", "S3 started", "S4 started", "S4 stopped", "S3 stopped", "SHD stopped", "FILO stopped")
	// start - stop
	messages = List(msgs(0), msgs(2), msgs(1), msgs(3), msgs(4), msgs(5), msgs(6), msgs(7) )
	// start - stop - start - stop
//	messages = List(msgs(0), msgs(1), msgs(2), msgs(3), msgs(4), msgs(1), msgs(2), msgs(3), msgs(4), msgs(5) )
	def apply() = {
		FILO.register()
		SHARED.register()
		new SERVICE_3().register()
		new SERVICE_4().dependsOn(SHARED).register()
	}

	/*  service 1 */
	object FILO extends Service4test with FirstInLastOutFeature {
		override protected def onStart(): Unit = {
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 3000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(msgs(0)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(msgs(7)); super.isStopped }
	}

	/*  service 2 */
	object SHARED extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(msgs(1)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(msgs(6)); super.isStopped }
	}

	/*  service 3 */
	class SERVICE_3 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(msgs(2)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(msgs(4)); super.isStopped }
	}

	/*  service 4 */
	class SERVICE_4 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { _stopped = true }
		}
		override protected def isStarted: Boolean = {if (_started) TestMsg(msgs(3)); super.isStarted  }
		override protected def isStopped: Boolean = {if (_stopped) TestMsg(msgs(5)); super.isStopped }
	}

}