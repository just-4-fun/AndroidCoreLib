package just4fun.android.core.app

import just4fun.android.core.async.{AsyncExecContext, AsyncExecContextInitializer}
import just4fun.android.core.utils._
import project.config.logging.Logger._
import scala.Some
import scala.util.{Failure, Success, Try}


/** [[AppService]] that extends it runs its async operations in allocated parallel [[Thread]]. */
trait ParallelThreadFeature {
	self: AppService =>
}

/** [[AppService]] that extends it runs its async operations each in new [[Thread]] from pool. */
trait NewThreadFeature {
	self: AppService =>
}

///** [[AppService]] that extends it can be cooled down if not used for some period. */
//trait CoolDownFeature {self: AppService =>}

/** [[AppService]] that extends it starts in first turn and stops last. */
trait FirstInLastOutFeature {
	self: AppService =>
}

/** [[AppService]] that extends it ensures that it can start right after init.
That is isStarted is called sequentially right after onInitialize, and if true is returned onStart is called. And service is considered Accessible. */
trait HotStartFeature {
	self: AppService =>
}



trait AppService extends AsyncExecContextInitializer with Loggable {

	import ServiceState._
	type Config <: AnyRef
	implicit protected[app] var context: AppServiceContext = _
	implicit val thisService: AppService = this
	lazy protected[app] val contexts = collection.mutable.Set[Int]().empty
	protected var configOpt: Option[Config] = None
	private var _serviceId: String = _
	private var _state = NONE
	protected var startedStatus, stoppedStatus: Try[Boolean] = Success(false)
	protected[app] var startCanceled: Boolean = _
	protected[app] var stopTimeout: Boolean = _
	lazy protected val activeWatchers = collection.mutable.WeakHashMap[ActiveStateWatcher, Boolean]()


	def state: ServiceState.Value = _state
	protected def state_=(v: ServiceState.Value): Unit = {
		logw("STATE", s"${" " * (90 - TAG.name.length) } [$ID: ${context.hashCode.toString.takeRight(3) }]:  ${_state } >  $v")
		_state = v
	}



	/* OVERRIDE */

	/** Override to define unique registration id for service */
	def ID: String = { if (isEmpty(_serviceId)) _serviceId = getClass.getSimpleName; _serviceId }
	def stateInfo(): String = ""

	/* OVERRIDE Lifecycle triggers */

	protected def onInitialize(): Unit = ()
	protected def onStart(): Unit = startedStatus = Success(true)
	protected def onStartCancel(): Unit = ()
	protected def isStarted(canceled: Boolean): Try[Boolean] = startedStatus
	protected def onStop(): Unit = stoppedStatus = Success(true)
	protected def onStopTimeout(): Unit = ()
	protected def isStopped(): Try[Boolean] = stoppedStatus
	protected def onFinalize(): Unit = ()

	protected[app] def onUiVisible(yes: Boolean): Unit = ()


	/* INTERNAL API */
	// TODO ALLOW AFTER INIT
	def register(conf: Config = null.asInstanceOf[Config], id: String = null): this.type = {
		if (ServiceManager.activeContext == null) loge(msg = s"AppService $id can not be registered without context. ")
		else if (contexts add ServiceManager.activeContext.hashCode()) {
			_serviceId = id
			context = ServiceManager.activeContext
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
	protected[app] def garbage = contexts.isEmpty




	/* STATE MACHINE */

	protected[app] final def nextState() = {
		//
		// EXECUTE
		//
		val prevState = state
		state match {
			case NONE =>
			case INIT => initialise
			case INITED => start
			case START => started
			case ACTIVE => stop
			case STOP => stopped
			case STOPPED => finalise
			case FINALIZED =>
			case FAILED =>
			case _ =>
		}
		if (state != prevState) triggerActiveStateChange()
		if (state >= FINALIZED) recycle
		//
		// DEFs
		//
		def initialise = {
			state = INITED
			trying { preInitialize(); onInitialize() }
			if (state == INITED && isInstanceOf[HotStartFeature]) trying {
				isStarted(startCanceled) match {
					case Success(true) => start
					case Failure(ex) => fail(ex)
					case _ =>
				}
			}
		}
		def mayStart = context.state.has(START)
		def canStart = !Dependencies.hasParent(this, { parent =>
			if (parent.state < ACTIVE) true
			else if (parent.state > ACTIVE) {fail(DependencyException); true }
			else false
		})
		def start = if (mayStart && canStart) trying {
			state = START
			onStart()
			if (state == START) started
		}
		def started: Boolean = {
			trying {
				isStarted(startCanceled) match {
					case Success(true) => state = ACTIVE
					case Success(false) if stopTimeout => fail(TimeoutException)
					case Failure(ex) => fail(ex)
					case _ =>
				}
			}
			state == ACTIVE
		}
		def mayStop = context.state.hasAll(ACTIVE, STOP)
		def canStop = !Dependencies.hasChild(this, _.state < STOPPED)
		def stop = if (mayStop && canStop) trying {
			if (state != FAILED) state = STOP
			onStop()
			if (state == STOP) stopped
		}
		def stopped: Boolean = {
			trying {
				isStopped() match {
					case Success(true) => state = STOPPED
					case Success(false) if stopTimeout => fail(TimeoutException)
					case Failure(ex) => fail(ex)
					case _ =>
				}
			}
			state == STOPPED
		}
		def finalise = {
			if (state != FAILED) state = FINALIZED
			trying { onFinalize() }
			trying { postFinalize() }
		}
		def trying(code: => Unit): Unit = try {code} catch {case err: Throwable => fail(err) }
		def fail(err: Throwable) = {
			loge(err, s"AppService $ID failed with error: ")
			if (state < FAILED) {
				val prevState = state
				state = FAILED
				if (prevState == START || prevState == ACTIVE) stop
				if (prevState < FINALIZED) finalise
				if (prevState < ACTIVE) context.onServiceStartFailed(this, err)
			}
		}
		def recycle = {
			context = null
			_serviceId = null
			configOpt = None
			startedStatus = Success(false)
			stoppedStatus = Success(false)
			startCanceled = false
			stopTimeout = false
			activeWatchers.clear()
		}
	}




	/* ACTIVE STATE WATCHING */
	/** Service functionality should be wrapped in this method to avoid access to service while it is not started. */
	protected[this] def ifActive[R](code: => R): Try[R] =
		if (state == ACTIVE) try {Success(code)} catch {case e: Throwable => Failure(e) }
		else Failure(ServiceNotActiveException)
	/** Registers watcher of [[ACTIVE]] state change of service.
	@param watcher receives onServiceStarted event
	  */
	def watchActiveStateChange(watcher: ActiveStateWatcher): Unit = {
		activeWatchers += (watcher -> true)
		triggerActiveStateChange(watcher)
	}
	/** Triggers [[ACTIVE]] state change of service. */
	protected def triggerActiveStateChange(specific: ActiveStateWatcher = null) = if (state >= ACTIVE) {
		val active = state == ACTIVE
		val watchers = if (specific == null) activeWatchers.map(_._1) else List(specific)
		watchers foreach { w =>
			val keepWatching = w.onActiveStateChanged(this, active)
			if (!keepWatching || !active) activeWatchers -= w
		}
	}

}
