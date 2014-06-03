package just4fun.android.core.inet

import just4fun.android.core.app.{App, ParallelThreadFeature, FirstInLastOutFeature, AppService}
import android.net.{ConnectivityManager => ConnMgr, NetworkInfo}
import android.content.{IntentFilter, Intent, BroadcastReceiver, Context}
import just4fun.android.core.utils.TryNLog
import just4fun.android.core.utils.Logger._
import just4fun.android.core.async._
import just4fun.android.core.async
import scala.util.Try

/* COMPANION */

object InetService {
	private var _online = false
	//
	def online = _online;
}


/* CLASS */

abstract class InetService extends AppService with FirstInLastOutFeature {
	import InetService._
	override def ID: String = "INET"
	private val LONG_SPAN: Int = 60000
	private val SHORT_SPAN: Int = 4000
	private val parallel = isInstanceOf[ParallelThreadFeature]
	lazy private val listeners = collection.mutable.Set[InetStateListener]()
	private var receiver: BroadcastReceiver = _
	private var checkSpan: Int = LONG_SPAN
	private var post: FutureExt[Unit] = _

	/* USAGE */

	/**
	Parallel execution.
	  * @param opts
	  * @param canceled
	  * @return
	  */
	def loadString(opts: InetOptions, canceled: () => Boolean = () => false): FutureExt[String] =
		async.post("loadString") { loadStringSync(opts, canceled).get }(executionContext)
	def loadBytes(opts: InetOptions, canceled: () => Boolean = () => false): FutureExt[Array[Byte]] =
		async.post("loadBytes") { loadBytesSync(opts, canceled).get }(executionContext)
	/**
	Sequential execution.
	  * @param opts
	  * @param canceled
	  * @return
	  */
	def loadStringSync(opts: InetOptions, canceled: () => Boolean = () => false): Try[String] = ifAvailable {
		InetRequest(opts.clone, new StreamToString, canceled).execute().get }
	def loadBytesSync(opts: InetOptions, canceled: () => Boolean = () => false): Try[Array[Byte]] = ifAvailable {
		InetRequest(opts.clone, new StreamToBytes, canceled).execute().get }

	def addListener(lr: InetStateListener, fireNow: Boolean = true) = {
		listeners += lr
		if (fireNow) TryNLog { lr.onlineStatusChanged(_online, false) }
	}
	def removeListener(lr: InetStateListener) = listeners -= lr

	def isTypeAvailable(typ: Int) = { val info = connMgr.getNetworkInfo(typ); info != null && info.isAvailable }
	def isMobileAvailable: Boolean = isTypeAvailable(ConnMgr.TYPE_MOBILE) || isTypeAvailable(ConnMgr.TYPE_WIMAX)
	def getConnectionType: Int = {
		val netInfo: NetworkInfo = connMgr.getActiveNetworkInfo
		if (netInfo != null) netInfo.getType else -1
	}


	/* SERVICE API */
	override protected def onInitialize(): Unit = {
		receiver = new BroadcastReceiver {
			def onReceive(context: Context, intent: Intent) {
				val isOnline = isReallyOnline
				logv("onReceive", s"wasOnline: ${_online};  isOnline: $isOnline")
				if (_online && !isOnline) fireEvent(false, false)
				else if (!_online && isOnline) postCheck(SHORT_SPAN)
			}
		}
		App().registerReceiver(receiver, new IntentFilter(ConnMgr.CONNECTIVITY_ACTION))
		checkOnline()
	}
	override protected def onFinalize(): Unit = {
		_online = false
		listeners.clear()
		TryNLog { App().unregisterReceiver(receiver) }
		receiver = null
		postCheckCancel()
		post = null
	}

	override protected[this] def isAvailable: Boolean = _online
	override def getStateInfo() = s"online = ${_online}"

	/* INTERNAL API */
	private def executionContext = if (parallel) execContext else NewThreadContext
	private def postCheck(span: Int = 0) = {
		checkSpan = if (span == 0) checkSpan else span
		post = postInUI("Check Online", checkSpan) { checkOnline() }
	}
	private def postCheckCancel() = if (post != null) post.cancel()
	private def connMgr = App().getSystemService(Context.CONNECTIVITY_SERVICE).asInstanceOf[ConnMgr]
	private def isReallyOnline: Boolean = {
		val netInfo: NetworkInfo = connMgr.getActiveNetworkInfo
		netInfo != null && netInfo.isConnected
	}
	private def checkOnline() {
		val isOnline = isReallyOnline
		if (isOnline != _online) fireEvent(isOnline, false)
		else if (!isOnline) postCheck()
	}
	private def fireEvent(isOnline: Boolean, byUser: Boolean) {
		_online = isOnline
		logv("fireEvent", s"online: $isOnline,  byUser: $byUser,  listeners size: ${listeners.size }")
		listeners.foreach { s => TryNLog { s.onlineStatusChanged(isOnline, byUser) } }
		if (!_online) postCheck(LONG_SPAN)
		else postCheckCancel()
	}
}
