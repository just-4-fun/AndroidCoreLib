package just4fun.android.demo1

import just4fun.android.core.app
import just4fun.android.core.app.{ServiceState, AppServiceContext, AppService}
import project.config.logging.Logger._

import scala.util.{Success, Try}

trait Service4test extends AppService {
	override type Config = Null
	isStarted = Success(false)
	isStopped = Success(false)
	val msg = new StringBuilder
	var name: String = _

	override def register(id: String)(implicit _context: AppServiceContext): this.type = {
		super.register(id)(_context)
		name = s"$ID: ${context.hashCode.toString.takeRight(3) }"
		this
	}

	override protected def state_=(v: ServiceState.Value): Unit = {
		if (msg.isEmpty) msg ++= s"$state"
		if (state.id + 1 != v.id) msg ++= s"  >  ### $v ###"
		else msg ++= s"  >  $v"
		super.state_=(v)
	}
	override protected def isStarted: Try[Boolean] = isStarted
	override protected def isStopped: Try[Boolean] = isStopped
	override protected def onStopTimeout(): Unit = {
		logi("onStopTimeout", s"${" " * (90 - TAG.name.length) } [$name]:  TIMEOUT")
	}

	override protected def onFinalize(): Unit = {
		isStarted = Success(false)
		isStopped = Success(false)
	}
	override def onUnregistered(implicit cxt: AppServiceContext): Unit = {
		val name = s"$ID: ${cxt.hashCode.toString.takeRight(3) }"
		loge(msg = s"${" " *  (30 - TAG.name.length)} [$name]:  ${msg.toString()}")
		msg.clear()
		super.onUnregistered(cxt)
	}
}
