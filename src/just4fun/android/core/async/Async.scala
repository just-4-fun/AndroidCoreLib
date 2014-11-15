package just4fun.android.core.async

import scala.concurrent.{Promise, Future}

object Async {


	/* IMPLICITS */
	implicit def ext2future[T](f: FutureExt[T]): Future[T] = f.promise.future
	implicit def ext2promise[T](f: FutureExt[T]): Promise[T] = f.promise





	/* USAGE */

	def post[T](id: Any, delayMs: Long = 0, replace: Boolean = true)(body: => T)(implicit cxt: AsyncExecContext = null) = {
		val _cxt = if (cxt == null) UiThreadContext else cxt
		_cxt.execute[T](id, delayMs, replace)(body)
	}
	def postCancellable[T](id: Any, delayMs: Long = 0, replace: Boolean = true)(body: (() => Unit) => T)(implicit cxt: AsyncExecContext = null) = {
		val _cxt = if (cxt == null) UiThreadContext else cxt
		_cxt.executeCancellable[T](id, delayMs, replace)(body)
	}
	def postInUI[T](id: Any, delayMs: Long = 0, replace: Boolean = true) = UiThreadContext.execute[T](id, delayMs, replace) _
	def postInUICancellable[T](id: Any, delayMs: Long = 0, replace: Boolean = true) = UiThreadContext.executeCancellable[T](id, delayMs, replace) _
	def fork[T](id: Any, delayMs: Long = 0, replace: Boolean = true) = NewThreadContext.execute[T](id, delayMs, replace) _
	def forkCancellable[T](id: Any, delayMs: Long = 0, replace: Boolean = true) = NewThreadContext.executeCancellable[T](id, delayMs, replace) _

}
