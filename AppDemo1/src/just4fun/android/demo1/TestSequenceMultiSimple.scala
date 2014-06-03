package just4fun.android.demo1

import just4fun.android.core.async._
import just4fun.android.core.utils.Logger.Loggable

object TestSequenceMultiSimple extends Loggable{
	def apply() = {
		new SERVICE_1().register()
		new SERVICE_2().register()
	}

	/*  service 1 */
	class SERVICE_1 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 5000) { _stopped = true }
		}
	}

	/*  service 2 */
	class SERVICE_2 extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 5000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { _stopped = true }
		}
	}

}