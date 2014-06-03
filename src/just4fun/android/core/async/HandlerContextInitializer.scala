package just4fun.android.core.async

import just4fun.android.core.utils.Logger.Loggable
import android.os.{HandlerThread, Looper}
import just4fun.android.core.app.{ParallelThreadFeature, Initializer}


trait HandlerContextInitializer extends Initializer {self: Loggable =>
	implicit var execContext: AsyncContext = _
	abstract override def preInitialize() = {
		execContext = this match {
			case _: ParallelThreadFeature => ParallelThreadContextFactory(TAG.name)
			case _ => new HandlerContext(TAG.name, Looper.getMainLooper)
		}
		super.preInitialize()
	}
	abstract override def postFinalize() = {
		execContext.quit()
		execContext = null
		super.postFinalize()
	}


	/**/

	object ParallelThreadContextFactory {
		def apply(name: String): HandlerContext = {
			val thread = new HandlerThread(name)
			thread.start()
			new HandlerContext(name, thread.getLooper)
		}
	}

}
