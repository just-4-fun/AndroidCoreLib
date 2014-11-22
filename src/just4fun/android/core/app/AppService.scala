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



trait AppService extends AsyncExecContextInitializer with ActiveStateWatcher with Loggable {
	
	import ServiceState._
	type Config <: AnyRef
	implicit protected[app] var context: AppServiceContext = _
	implicit val thisService: AppService = this
	lazy protected[app] val contexts = collection.mutable.Set[Int]().empty
	protected var configOpt: Option[Config] = None
	var ID: String = getClass.getSimpleName
	private var _state = NONE
	private[this] var _started, _stopped: Try[Boolean] = Success(false)
	protected[app] var startCanceled: Boolean = _
	protected[app] var stopTimeout: Boolean = _
	protected[app] var failureOpt: Option[Throwable] = None
	lazy protected[app] val activeWatchers = collection.mutable.WeakHashMap[ActiveStateWatcher, Boolean]()
	protected[app] var weight: Int = 0
	protected[app] var unregistering = false
	
	
	def state: ServiceState.Value = _state
	protected def state_=(v: ServiceState.Value): Unit = {
		logw("STATE", s"${" " * (90 - TAG.name.length) } [$ID: ${context.hashCode.toString.takeRight(3) }]:  ${_state } >  $v")
		_state = v
	}
	
	
	/* OVERRIDE */
	
	def stateInfo(): String = ""
	
	/* OVERRIDE Lifecycle triggers */
	
	protected def onInitialize(): Unit = ()
	protected def onStart(): Unit = isStarted = Success(true)
	protected def onStartCancel(): Unit = ()
	protected def isStarted: Try[Boolean] = _started
	protected def isStarted_=(value: Try[Boolean]): Unit = _started = value
	protected[app] def onUiVisible(yes: Boolean): Unit = ()
	protected def onStop(): Unit = isStopped = Success(true)
	protected def onStopTimeout(): Unit = ()
	protected def isStopped: Try[Boolean] = _stopped
	protected def isStopped_=(value: Try[Boolean]): Unit = _stopped = value
	protected def onFinalize(): Unit = ()
	
	
	
	/* INTERNAL API */
	def register(id: String = null)(implicit _context: AppServiceContext): this.type = {
		if (contexts add _context.hashCode()) {
			val oldContext = context
			context = _context
			// CASE: repeated registration of same service instance.
			// CAUSE: parallel service context started
			if (oldContext != null) {
				// reset state in case of repeated registration
				state = _state match {
					case NONE | FINALIZED | FAILED => INIT // reinit
					case STOP => TryNLog {
						onStopTimeout()
						isStopped match {
							case Success(true) => INITED // restart
							case _ => onFinalize(); INIT // reinit
						}
					}.getOrElse { TryNLog { onFinalize() }; INIT }
					case STOPPED => INITED // restart
					case _ => _state
				}
				//
				// remove child dependencies to let old parent stop
				// WARN: the problem can arise when parent is not shared and service is started. After switch context it tries to use parent from new context which may be not yet started.
				Dependencies.remove((parent, child) => child == this)
			}
			else state = INIT
			//
			startCanceled = false
			stopTimeout = false
			failureOpt = None
			_started = Success(false)
			_stopped = Success(false)
			unregistering = false
			context.registerService(this)
		}
		if (id != null) ID = id
		this
	}
	def config(conf: Config): this.type = {
		configOpt = Option(conf)
		this
	}
	def dependsOn(services: AppService*)(implicit context: AppServiceContext): this.type = {
		services.foreach { s =>
			s.register() // no way to forget register
			Dependencies.add(s, this)
		}
		this
	}
	def watch(services: AppService*)(implicit context: AppServiceContext): this.type = {
		services.foreach { s =>
			s.register() // no way to forget register
			activeWatchers += (s -> true)
		}
		this
	}
	def unregister() = {
		unregistering = true
		context.continue()
	}
	protected[app] def cancelStart(): Unit = if (state == START) TryNLog { startCanceled = true; onStartCancel() }
	protected[app] def timeout(): Unit = if (state < STOPPED) TryNLog { stopTimeout = true; onStopTimeout() }
	protected[app] def onUnregistered(implicit cxt: AppServiceContext): Unit = {
		contexts.remove(cxt.hashCode())
		Dependencies.remove((parent, child) => parent == this || child == this)
	}
	//	protected def isShared: Boolean = contexts.size > 1
	//	protected[app] def garbage = contexts.isEmpty
	override def toString: String = s"[$ID]"




	/* STATE MACHINE */

	protected[app] final def nextState() = {
		//
		// EXECUTE
		//
		val prevState = state
		state match {
			case NONE => loge(msg = s"AppService $ID is not registered: ")
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
		if (state >= FINALIZED) recycle // order is important
		//
		// DEFs
		//
		def initialise = trying {
				state = INITED
				preInitialize()
				onInitialize()
				if (isInstanceOf[HotStartFeature]) {
					isStarted match {
						case Success(true) => start
						case Failure(ex) => fail(ex)
						case _ =>
					}
				}
			}
		def mayStart = context.state.has(START)
		def canStart = Dependencies.hasNoParent { parent =>
			if (parent.state > ACTIVE) fail(DependencyException(parent.ID, ID))
			parent.state != ACTIVE
		}
		def start = if (mayStart && canStart) trying {
			state = START
			onStart()
			started
		}
		def started: Boolean = trying {
			isStarted match {
				case Success(true) => state = ACTIVE
				case Success(false) if stopTimeout => fail(TimeoutException)
				case Failure(ex) => fail(ex)
				case _ =>
			}
			if (state == FAILED) _started = Failure(failureOpt.getOrElse(new Exception))
			state == ACTIVE
		}
		def mayStop = context.state.hasAll(ACTIVE, STOP) || unregistering
		def canStop = Dependencies.hasNoChild { child => child.state < STOPPED }
		def stop = if (mayStop && canStop) trying {
			state = STOP
			onStop()
			stopped
		}
		def stopped: Boolean = trying {
			isStopped match {
				case Success(true) => state = STOPPED
				case Success(false) if stopTimeout => fail(TimeoutException)
				case Failure(ex) => fail(ex)
				case _ =>
			}
			state == STOPPED
		}
		def finalise = trying {
			state = FINALIZED
			onFinalize()
		}
		def fail(err: Throwable) = {
			loge(err, s"AppService [$ID] in state [$state] failed with error.")
			if (state < FAILED) {
				failureOpt = Option(err)
				val prevState = state
				state = FAILED
				if (prevState == START || prevState == ACTIVE) trying(onStop())
				if (prevState < FINALIZED) trying(onFinalize())
				if (prevState < ACTIVE) context.onServiceStartFailed(this, err)
			}
		}
		def recycle = trying {
			context = null
			configOpt = None
			postFinalize()
		}
		def trying[T](code: => T): T = try {code} catch {case err: Throwable => fail(err); null.asInstanceOf[T] }
	}
	
	
	
	
	/* ACTIVE STATE WATCHING */

	/** Override if watch any service. */
	override def onActiveStateChanged(service: AppService, active: Boolean): Boolean = true
	/** Service functionality should be wrapped in this method to avoid access to service while it is not started. */
	protected[this] def ifActive[R](code: => R): Try[R] =
		if (state == ACTIVE) try {Success(code)} catch {case e: Throwable => Failure(e) }
		else Failure(ServiceNotActiveException(ID, state.toString))
	/** Registers watcher of [[ACTIVE]] state change of service.
	@param watcher receives onServiceStarted event
	  */
	def watchActiveStateChange(watcher: ActiveStateWatcher): Unit = {
		activeWatchers += (watcher -> true)
		triggerActiveStateChange(watcher)
	}
	/** Triggers [[ACTIVE]] state change of service. */
	protected def triggerActiveStateChange(specific: ActiveStateWatcher = null): Unit = if (state >= ACTIVE) {
		val active = state == ACTIVE
		val watchers = if (specific == null) activeWatchers.map(_._1) else List(specific)
		watchers foreach { w =>
			val keepWatching = Try { w.onActiveStateChanged(this, active) }.getOrElse(false)
			if (!keepWatching || !active) activeWatchers -= w
		}
	}

}





/* SERVICE AVAILABILITY WATCHER */
trait ActiveStateWatcher {
	/** Is called when Service started (state = STARTED) or inaccessible (state >= STOP).
	  * @param service Service that state is watched
	  * @param active true - if service is started; false otherwise
	  * @return  true - to keep watching; false - to stop watching
	  */
	def onActiveStateChanged(service: AppService, active: Boolean): Boolean
}
