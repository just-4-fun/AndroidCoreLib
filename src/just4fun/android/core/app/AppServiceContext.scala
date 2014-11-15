package just4fun.android.core.app

import just4fun.android.core.utils.{BitState}
import project.config.logging.Logger._
import just4fun.android.core.async._
import just4fun.android.core.async.Async._
import just4fun.android.core.utils.time._


class AppServiceContext(serviceMgr: ServiceManager) extends AsyncExecContextInitializer with Loggable {

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

	/** TODO does not start tiking if registers in silent time. Need manage tiker. */
	private[app] def registerService(s: AppService): Unit = services add s
	def onServiceStartFailed(service: AppService, err: Throwable): Unit = {
		if (serviceMgr.isServiceStartFatalError(service, err))  state.set(FAILED)
	}
	def unregisterService(s: AppService): Unit = if (services remove s) s.onUnregistered
	def findService[S <: AppService : Manifest](id: String): Option[S] = services.collectFirst {
		case s: S if s.ID == id => s
	}


	/* CONTEXT LIFE CYCLE */

	def init(): Unit = {
		preInitialize()
		state.set(INIT)
		services.foreach(s => if (isFiLo(s)) assignFiLo(s))
		postTik()
		// DEFs
		def isFiLo(s: AppService) = s.isInstanceOf[FirstInLastOutFeature]
		def assignFiLo(up: AppService) = services.foreach(dp => if (!isFiLo(dp)) Dependencies.put(up, dp))
	}
	def start(): Unit = {
		state.set(START)
		startedMs = deviceNow
		postTik()
	}
	def stop(): Unit = {
		state.set(STOP)
		if (state.hasNo(ACTIVE)) cancelStart()
		timeoutMs = deviceNow + App.config.timeoutDelay
		postTik()
		// DEFs
		def cancelStart() = services foreach (s => if (s.context == this) s.cancelStart())
	}
	def onVisible(yes: Boolean): Unit = services foreach (s => if (s.context == this) s.onUiVisible(yes))


	/* INTERNAL API */

	def postTik(delayMs: Long = 0): Unit = post("TIK", delayMs) { nextState() }
	def clearTik(): Unit = asyncExecContext.clear()

	protected def nextState(): Unit = {
		var totalN, startedN = 0
		//
		services foreach { s =>
			if (s.context == this) {
				s.nextState()
				totalN += 1
				s.state match {
					case ACTIVE => startedN += 1
					case FINALIZED | FAILED => unregisterService(s)
					case _ =>
				}
			}
		}
		//
		val started = startedN == totalN
		val finalized = totalN == 0
		//
		if (started && state.hasNo(ACTIVE)) onStarted()
		else if (state.has(STOP)) {
			if (finalized && state.hasNo(FINALIZED)) onFinalized()
			else if (isTimeout) onTimeout()
		}
		//
		if (state.hasNo(START) || (started && state.hasNo(STOP)) || state.has(FINALIZED)) clearTik()
		else postTik(tikDelay)
		logv("tikState", s"state=$state; total= $totalN;  started= $startedN")
		//
		// DEFs
		//
		def onStarted() = state.set(ACTIVE)
		def onFinalized() = {
			state.set(FINALIZED)
			services foreach unregisterService // for shared services if any
			postFinalize()
			serviceMgr.onFinalized
		}
		def isTimeout = if (timeoutMs > 0 && deviceNow > timeoutMs) {timeoutMs = 0; true } else false
		def onTimeout() = services foreach (s => if (s.context == this) s.timeout())
		// TODO move values to AppConfig
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