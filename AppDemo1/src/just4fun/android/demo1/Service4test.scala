package just4fun.android.demo1

import just4fun.android.core.app
import just4fun.android.core.app.{ServiceState, AppServiceContext, AppService}
import project.config.logging.Logger._

import scala.util.{Success, Try}

trait Service4test extends AppService {
	override type Config = Null
	startedStatus = Success(false)
	stoppedStatus = Success(false)
	val msg = new StringBuilder
	var name: String = _

	override def register(conf: Config, id: String): this.type = {
		super.register(conf, id)
		name = s"$ID: ${context.hashCode.toString.takeRight(3) }"
		this
	}

	override protected def state_=(v: ServiceState.Value): Unit = {
		if (msg.isEmpty) msg ++= s"$state"
		if (state.id + 1 != v.id) msg ++= s"  >  ### $v ###"
		else msg ++= s"  >  $v"
		super.state_=(v)
	}
	override protected def isStarted(canceled: Boolean): Try[Boolean] = startedStatus
	override protected def isStopped(): Try[Boolean] = stoppedStatus
	override protected def onStopTimeout(): Unit = {
		logi("onStopTimeout", s"${" " * (90 - TAG.name.length) } [$name]:  TIMEOUT")
	}

	override protected def onFinalize(): Unit = {
		startedStatus = Success(false)
		stoppedStatus = Success(false)
	}
	override def onUnregistered(implicit cxt: AppServiceContext): Unit = {
		val name = s"$ID: ${cxt.hashCode.toString.takeRight(3) }"
		loge(msg = s"${" " *  (30 - TAG.name.length)} [$name]:  ${msg.toString()}")
		msg.clear()
		super.onUnregistered(cxt)
	}
}
