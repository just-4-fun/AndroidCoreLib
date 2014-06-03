package just4fun.android.core.app

import just4fun.android.core.async.{AsyncContext, HandlerContextInitializer}
import just4fun.android.core.utils._
import Logger._
import scala.Some
import scala.util.{Failure, Success, Try}


/** [[AppService]] that extends it runs its [[AsyncContext]] in new [[Thread]] */
trait ParallelThreadFeature {self: AppService =>}

///** [[AppService]] that extends it can be cooled down if not used for some period */
//trait CoolDownFeature {self: AppService =>}

/** [[AppService]] that extends it starts in first turn and stops last */
trait FirstInLastOutFeature {self: AppService =>}

/** [[AppService]] that extends it ensures that it can start right after init.
That is isStarted is called sequentially right after onInitialize, and if true is returned onStart is called. And service is considered Accessible. */
trait HotStartFeature {self: AppService =>}



trait AppService extends HandlerContextInitializer with AppServiceAccessibilityWatcher with Loggable {self: Initializer =>

	import ServiceState._
	type Config <: AnyRef
	implicit protected[app] var context: AppServiceContext = _
	implicit val thisService: AppService = this
	lazy protected val contexts = collection.mutable.Set[Int]().empty
	lazy protected val watchers = collection.mutable.WeakHashMap[AppServiceAccessibilityWatcher, Boolean]()
	protected var configOpt: Option[Config] = None
	private var _id: String = _
	private var _state = NONE
	protected[app] var startCanceled: Boolean = _
	protected[app] var stopTimeout: Boolean = _


	def state: ServiceState.Value = _state
	protected def state_=(v: ServiceState.Value): Unit = {
		logw("STATE", s"${" " * (90 - TAG.name.length) } [$ID: ${context.hashCode.toString.takeRight(3) }]:  ${_state } >  $v")
		_state = v
	}



	/* OVERRIDE */

	/** Override to define unique registration id for service */
	def ID: String = { if (isEmpty(_id)) _id = getClass.getSimpleName; _id }
	def getStateInfo(): String = ""

	/* OVERRIDE Lifecycle triggers */

	protected def onInitialize(): Unit = ()
	protected def onStart(): Unit = ()
	protected def isStarted: Boolean = true
	protected def onStartCancel(): Unit = ()
	protected def onStop(): Unit = ()
	protected def isStopped: Boolean = true
	protected def onStopTimeout(): Unit = ()
	protected def onFinalize(): Unit = ()

	protected[app] def onUiVisible(yes: Boolean): Unit = ()
	/** Real time availability status. May be not the same as STARTED state. Used in [[ifAvailable]]
	  * @return true  if available right now
	  */
	protected[this] def isAvailable: Boolean = isStarted

	/** Used to handle Start phase accessibility of Service(s) from which this Service is dependent from.
	  * @note implementation from [[AppServiceAccessibilityWatcher]].
	  */
	def onServiceAccessibility(s: AppService, available: Boolean): Boolean = true


	/* INTERNAL API */
	// TODO ALLOW AFTER INIT
	def register(conf: Config = null.asInstanceOf[Config], id: String = null): this.type = {
		val cxt = ServiceManager.current
		if (contexts add cxt.hashCode()) {
			_id = id
			context = cxt
			configOpt = Option(conf)
			state = state match {
				case NONE | FINALIZED | FAILED => INIT // init / reinit
				case STOP | STOPPED => INITED // restart
				case _ => state
			}
			startCanceled = false
			stopTimeout = false
			context.registerService(this)
		}
		this
	}
	def dependsOn(services: AppService*): this.type = { services.foreach(Dependencies.put(_, this)); this }
	//	protected def isShared: Boolean = contexts.size > 1
	protected[app] def cancelStart(): Unit = if (state == START) TryNLog { startCanceled = true; onStartCancel() }
	protected[app] def timeout(): Unit = if (state < STOPPED) TryNLog { stopTimeout = true; onStopTimeout() }
	protected[app] def onUnregistered(implicit cxt: AppServiceContext): Unit = contexts.remove(cxt.hashCode())




	/* STATE MACHINE */

	protected[app] final def nextState() = {
		//
		// Execution
		//
		val prevState = state
		state match {
			case NONE =>
			case INIT => initialise
			case INITED => if (mayStart && canStart) start
			case START => started
			case STARTED => if (mayStop && canStop) stop
			case STOP => stopped
			case STOPPED => finalise
			case FINALIZED =>
			case FAILED =>
			case _ =>
		}
		if (state != prevState) accessibilityChange foreach accessibilityChanged
		if (state >= FINALIZED) recycle
		//
		// Definitions
		//
		def initialise = {
			state = INITED
			trying { preInitialize(); onInitialize() }
			if (state == INITED && isHot) trying { if (isStarted) start }
		}
		def mayStart = context.state.has(START)
		def canStart = !Dependencies.isDependent(this, _.state < STARTED)
		def start = trying { state = START; onStart(); if (state == START) started }
		def started: Boolean = {
			trying { if (isStarted || stopTimeout) state = STARTED }
			state == STARTED
		}
		def mayStop = context.state.hasAll(STARTED, STOP)
		def canStop = !Dependencies.hasDependent(this, _.state < STOPPED)
		def stop = trying { state = STOP; onStop(); if (state == STOP) stopped }
		def stopped: Boolean = {
			trying { if (isStopped || stopTimeout) state = STOPPED }
			state == STOPPED
		}
		def finalise = {
			if (state != FAILED) state = FINALIZED
			trying { onFinalize() }
			trying { postFinalize() }
		}
		def trying(code: => Unit): Unit = try {code} catch {case ex: Throwable => loge(ex); fail(ex) }
		def fail(err: Throwable) = if (state < FAILED) {
			val prevState = state
			state = FAILED
			if (prevState < STARTED) context.onServiceStartFailed(this, err)
			if (prevState < FINALIZED) finalise
		}
		def accessibilityChanged(accessibe: Boolean) {
			fireAccessibilityChage(accessibe)
			if (prevState < STARTED) Dependencies.withDependents(this, _.onServiceAccessibility(this, accessibe))
		}
		def recycle = {
			context = null
			_id = null
			configOpt = None
			startCanceled = false
			stopTimeout = false
			watchers.clear()
			contexts.clear()
			Dependencies.remove(_ == this)
		}
		def isHot = isInstanceOf[HotStartFeature]
	}




	/* USAGE */

	/**
	 */
	protected[this] def ifAvailable[R](code: => R): Try[R] =
		if (isAvailable) try {Success(code)} catch {case e: Throwable => Failure(e) }
		else Failure(UnavailableException)




	/* ACCESSIBILITY CHANGE WATCH */
	/**
	Tracks STARTED / STOP / FAILED states of service. Still service may be not available @see [[isAvailable]]
	  * @param watcher
	  */
	def watchAccessibility(watcher: AppServiceAccessibilityWatcher): Unit = {
		watchers += (watcher -> true)
		accessibilityChange foreach fireAccessibilityChage
	}
	def fireAccessibilityChage(accessibe: Boolean) = watchers foreach { case (w, _) =>
		val keep = w.onServiceAccessibility(this, accessibe)
		if (!keep || !accessibe) watchers -= w
	}
	protected[this] def accessibilityChange: Option[Boolean] = {
		if (state == STARTED) Some(true)
		else if (state >= STOP) Some(false)
		else None
	}

}


