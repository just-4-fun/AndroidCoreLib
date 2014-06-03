package just4fun.android.core.app

import just4fun.android.core.utils.{Logger, BitState}
import Logger._
import just4fun.android.core.async._
import just4fun.android.core.utils.time._


class AppServiceContext(serviceMgr: ServiceManager, timeoutDelay: Long) extends HandlerContextInitializer with Loggable {self: Initializer =>

	import ServiceState._

	implicit val context: AppServiceContext = this
	lazy protected[core] val services = collection.mutable.Set[AppService]().empty
	protected var startedMs: Long = _
	protected var timeoutMs: Long = _
	lazy protected[core] val state = new BitState[ServiceState.Value] {
		override def set(eVal: ServiceState.Value*): BitState[ServiceState.Value] = {
			logw("STATE", s"${" " * (70 - TAG.name.length) } [INST: ${context.hashCode.toString.takeRight(3) }]:     [$toString]  >+   ${eVal.mkString(",") }")
			super.set(eVal: _*)
		}
		override def clear: BitState[ServiceState.Value] = {
			logw("STATE", s"${" " * (70 - TAG.name.length) } [INST: ${context.hashCode.toString.takeRight(3) }]:     [$toString]  >   CLEAR")
			super.clear
		}
	}


	/* SERVICE MANAGMENT */

	/** Called by [[AppService]] */
	private[app] def registerService(s: AppService): Unit = services add s
	def onServiceStartFailed(service: AppService, err: Throwable): Unit = serviceMgr.onServiceStartFailed(service, err)
	def onServiceFinalized(s: AppService): Unit = if (services remove s) s.onUnregistered
	def findService[S <: AppService](id: String): Option[S] = services.find(s => s.ID == id && s.isInstanceOf[S]).map(_.asInstanceOf[S])


	/* CONTEXT LIFE CYCLE */

	def init(): Unit = {
		preInitialize()
		state.set(INIT, INITED)
		featureFiLo()
		postTik()
		// DEFs of FIRST IN LAST OUT FEATURE
		def featureFiLo() = services.foreach(s => if (isFiLo(s)) assignFiLo(s))
		def isFiLo(s: AppService) = s.isInstanceOf[FirstInLastOutFeature]
		def assignFiLo(up: AppService) = services.foreach(dp => if (!isFiLo(dp)) Dependencies.put(up, dp))
	}
	def start(): Unit = {
		state.set(START)
		startedMs = deviceNow
		postTik()
	}
	def stop(): Unit = {
		if (state.hasNo(STARTED)) cancelStart()
		timeoutMs = deviceNow + timeoutDelay
		state.set(STOP)
		postTik()
		// DEFs
		def cancelStart() = services foreach (s => if (s.context == this) s.cancelStart())
	}
	def onVisible(yes: Boolean): Unit = services foreach (s => if (s.context == this) s.onUiVisible(yes))


	/* INTERNAL API */

	def postTik(delayMs: Long = 0): Unit = post("TIK", delayMs) { nextState() }

	protected def nextState(): Unit = {
		var totalN, startedN = 0
		//
		services foreach { s =>
			if (s.context == this) {
				s.nextState()
				totalN += 1
				s.state match {
					case STARTED => startedN += 1
					case FINALIZED | FAILED => onServiceFinalized(s)
					case _ =>
				}
			}
		}
		//
		val started = startedN == totalN
		val finalized = totalN == 0
		//
		if (started && state.hasNo(STARTED)) onStarted()
		else if (state.has(STOP)) {
			if (finalized && state.hasNo(FINALIZED)) onFinalized()
			else if (isTimeout) onTimeout()
		}
		//
		if (state.hasNo(START)) execContext.clear()
		else if (started && state.hasNo(STOP)) execContext.clear()
		else if (state.has(FINALIZED)) execContext.clear()
		else postTik(tikDelay)
		logv("tikState", s"state=$state; total= $totalN;  started= $startedN")
		//
		/* DEFINITIONS */
		//
		def onStarted() = state.set(STARTED)
		def onFinalized() = {
			state.set(STOPPED, FINALIZED)
			services foreach onServiceFinalized // for shared services if any
			postFinalize()
			serviceMgr.onFinalized
		}
		def isTimeout = if (timeoutMs > 0 && deviceNow > timeoutMs) {timeoutMs = 0; true} else false
		def onTimeout() = services foreach (s => if (s.context == this) s.timeout())
		def tikDelay: Int = {
			if (state.has(STOP)) 1000
			else {
				val ss = deviceNow - startedMs
				if (ss <= 500) 50
				else if (ss <= 5000) 500
				else if (ss <= 50000) 1000
				//			else if (ss <= 50000) 5000
				else 60000
			}
		}
	}

}