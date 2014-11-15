package just4fun.android.core

package object app {


	/* STATES */
	object ActivityState extends Enumeration {
		val NONE, CREATED, STARTED, RESUMED, PAUSED, STOPPED, DESTROYED = Value
	}

	object ServiceState extends Enumeration {
		val NONE, INIT, INITED, START, ACTIVE, STOP, STOPPED, FINALIZED, FAILED = Value
	}



	/** TODO SYNCHRONOUS RESULT
	sealed abstract class Result[+A] {
		def isEmptyOrNull: Boolean
		def get: A
		final def getOrElse[B >: A](default: => B): B = if (isEmptyOrNull) default else this.get
		final def ifDefined[U](f: A => U): Unit = if (!isEmptyOrNull) f(this.get)
	}

	case class ResultOK[+A](x: A) extends Result[A] {
		def isEmptyOrNull = x == null
		def get: A = x
	}

	class ResultVal extends Result[Nothing] {
		def isEmptyOrNull = true
		def get = throw UnavailableException
	}

	case object Unavailable extends ResultVal
	case object Failed extends ResultVal
*/



	/* EXCEPTIONS */
	case object ServiceNotActiveException extends Exception
	case object DependencyException extends Exception
	case object NoConfigException extends Exception
	case object TimeoutException extends Exception




	/* SERVICE AVAILABILITY WATCHER */
	trait ActiveStateWatcher {
		/** Is called when Service started (state = STARTED) or inaccessible (state >= STOP).
		  * @param service Service that state is watched
		  * @param active true - if service is started; false otherwise
		  * @return  true - to keep watching; false - to stop watching
		  */
		def onActiveStateChanged(service: AppService, active: Boolean): Boolean
	}



}
