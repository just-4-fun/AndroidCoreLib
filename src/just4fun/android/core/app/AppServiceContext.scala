package just4fun.android.core.app

import just4fun.android.core.app.ServiceState._
import just4fun.android.core.utils.{BitState}
import project.config.logging.Logger._
import just4fun.android.core.async._
import just4fun.android.core.async.Async._
import just4fun.android.core.utils.time._

import scala.collection.mutable
import scala.util.Try


class AppServiceContext(serviceMgr: ServiceManager) extends AsyncExecContextInitializer with Loggable {

	import ServiceState._

	implicit val context: AppServiceContext = this
	protected[app] var services = mutable.LinkedHashSet[AppService]().empty
	protected var timeoutMs: Long = _
	protected val nextDelay = App.config.tikDelay
	lazy protected[app] val state = new BitState[ServiceState.Value] {
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
	
	def findService[S <: AppService : Manifest](id: String): Option[S] = services.collectFirst {
		case s: S if s.ID == id => s
	}

	/** TODO does not start tiking if registers in silent time. Need manage tiker. */
	private[app] def registerService(s: AppService): Unit = {
		services add s
		continue()
	}
	private[app] def unregisterService(s: AppService): Unit = if (services remove s) s.onUnregistered
	protected[app] def onServiceStartFailed(service: AppService, err: Throwable): Unit = {
		// TODO ? stop instance or hint user
		if (serviceMgr.isServiceStartFatalError(service, err)) state.set(FAILED)
	}


	/* CONTEXT LIFE CYCLE */

	def start(): Unit = {
		preInitialize()
		state.set(START)
		// reorder services in order of dependencies (parents before children)
		services = mutable.LinkedHashSet[AppService]() ++ services.toSeq.sortBy(-_.weight)
		// ASSIGN FiLo dependencies
		services.withFilter(isFiLo).foreach { parent =>
			services.withFilter(!isFiLo(_)).foreach(child => Dependencies.add(parent, child))
		}
		
		postTik()
		// DEFs
		def isFiLo(s: AppService) = s.isInstanceOf[FirstInLastOutFeature]
	}
	def continue() = if (state.has(ACTIVE) && state.hasNo(STOP)) postTik()
	def stop(): Unit = {
		state.set(STOP)
		// reorder services in order of dependencies (parents after children)
		services = mutable.LinkedHashSet[AppService]() ++ services.toSeq.sortBy(_.weight)
		// mark state STOP but wait all services to be ACTIVE
		if (state.hasNo(ACTIVE)) cancelStart()
		timeoutMs = deviceNow + App.config.timeoutDelay
		postTik()
		// DEFs
		def cancelStart() = services foreach (s => if (s.context == this) s.cancelStart())
	}
	def onVisible(yes: Boolean): Unit = services foreach (s => if (s.context == this) Try { s.onUiVisible(yes) })


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
				if (s.state == ACTIVE && !s.unregistering) startedN += 1
			}
		}
		services foreach { s =>
			if (s.context == this && (s.state == FINALIZED || s.state == FAILED)) unregisterService(s)
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
		else postTik(nextDelay(state))
		logv("tikState", s"state=$state; all= $totalN;  total= $totalN;  started= $startedN")
		//
		// DEFs
		//
		def onStarted() = state.set(ACTIVE)
		def onFinalized() = {
			state.set(FINALIZED)
			// for shared (with other context) services if any
			services foreach unregisterService
			postFinalize()
			serviceMgr.onFinalized
		}
		def isTimeout = if (timeoutMs > 0 && deviceNow > timeoutMs) {timeoutMs = 0; true } else false
		def onTimeout() = services foreach (s => if (s.context == this) s.timeout())
	}

}





/* TIC DELAY */
/** Returns delay in milliseconds to next call of nextState */
abstract class TikDelay extends (BitState[ServiceState.Value] => Long)


/* TEST IMPLEMENTATION */
class TikDelayTest extends TikDelay {
	val startedMs = deviceNow
	def apply(state: BitState[ServiceState.Value]): Long = {
		if (state.has(STOP)) 1000
		else if (state.has(ACTIVE)) 1000
		else {
			val ss = deviceNow - startedMs
			1000
		}
	}
}
/* REAL IMPLEMENTATION */
class TikDelayDefault extends TikDelay {
	val startedMs = deviceNow
	def apply(state: BitState[ServiceState.Value]): Long = {
		if (state.has(STOP)) 1000
		else if (state.has(ACTIVE)) 1000
		else {
			val ss = deviceNow - startedMs
			if (ss <= 500) 50
			else if (ss <= 5000) 500
			else if (ss <= 50000) 1000
			else 60000
		}
	}
}
