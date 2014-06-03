package just4fun.android.demo1

import just4fun.android.core.utils.Logger.Loggable
import just4fun.android.core.app.App
import just4fun.android.core.app.App._
import just4fun.android.core.async._

object TestSharedMulty extends Loggable{
	import App._
	var counter = 0
	def apply() = {
		SHARED.register()
		new INSTANCE(counter).register()
		counter += 1
	}

	/*  service */
	object SHARED extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop", 2000) { _stopped = true }
		}
	}

	/*  service */
	class INSTANCE(n: Int) extends Service4test {
		override protected def onStart(): Unit = {
			post("Start", 2000) { _started = true }
		}
		override protected def onStop(): Unit = {
			post("Stop",  if (n%2 == 0) 15000 else 5000) { _stopped = true }
		}
	}

}