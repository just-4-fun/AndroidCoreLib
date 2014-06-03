package just4fun.android.core

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Failure, Success}
import scala.util.control.NonFatal
import android.os.{Message, Handler, HandlerThread, Looper}
import just4fun.android.core.async.ThreadPoolContext
import java.util.concurrent.ThreadPoolExecutor
import just4fun.android.core.utils.Logger
import Logger._
import just4fun.android.core.app.AppService

package object async {

	/* IMPLICITS */
	implicit def ext2future[T](f: FutureExt[T]): Future[T] = f.promise.future
	implicit def ext2promise[T](f: FutureExt[T]): Promise[T] = f.promise

	/* DEFAULT CONTEXTS */
	/** Re-posts runnable if UI is reconfiguring */
	object UiThreadContext extends HandlerContext("Main", Looper.getMainLooper) {
		override def handle(runnable: Runnable): Unit = {
			super.handle(runnable)
			// TODO make this code available
//			if (App.isReconfiguring) handler.post(runnable)
//			else super.handle(runnable)
		}
	}
	/** Can be overridden for more specific ThreadPoolExecutor*/
	var threadPoolExecutor: ThreadPoolExecutor = _
	object NewThreadContext extends ThreadPoolContext(threadPoolExecutor)


	/* USAGE */

	def post[T](id: Any, delayMs: Long = 0, replace: Boolean = true)(body: => T)(implicit cxt: AsyncContext = null) = {
		val _cxt = if (cxt == null) UiThreadContext else cxt
		_cxt.execute[T](id, delayMs, replace)(body)
	}
	def postCancellable[T](id: Any, delayMs: Long = 0, replace: Boolean = true)(body: (() => Unit) => T)(implicit cxt: AsyncContext = null) = {
		val _cxt = if (cxt == null) UiThreadContext else cxt
		_cxt.executeCancellable[T](id, delayMs, replace)(body)
	}
	def postInUI[T](id: Any, delayMs: Long = 0, replace: Boolean = true) = UiThreadContext.execute[T](id, delayMs, replace)_
	def postInUICancellable[T](id: Any, delayMs: Long = 0, replace: Boolean = true) = UiThreadContext.executeCancellable[T](id, delayMs, replace)_
	def fork[T](id: Any, delayMs: Long = 0, replace: Boolean = true) = NewThreadContext.execute[T](id, delayMs, replace)_
	def forkCancellable[T](id: Any, delayMs: Long = 0, replace: Boolean = true) = NewThreadContext.executeCancellable[T](id, delayMs, replace)_


	/* ASYNC RUNNABLE */
	abstract class AsyncRunnable(val id: Any) extends Runnable

}