package just4fun.android.demo1

import just4fun.android.core.app
import just4fun.android.core.app.{ServiceState, AppServiceContext, AppService}
import just4fun.android.core.utils.Logger._

trait Service4test extends AppService {
	override type Config = Null
	var _started, _stopped = false
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
	override protected def isStarted: Boolean = { _started }
	override protected def isStopped: Boolean = { _stopped }
	override protected def onStopTimeout(): Unit = {
		logi("onStopTimeout", s"${" " * (90 - TAG.name.length) } [$name]:  TIMEOUT")
	}

	override protected def onFinalize(): Unit = {
		_started = false
		_stopped = false
	}
	override def onUnregistered(implicit cxt: AppServiceContext): Unit = {
		val name = s"$ID: ${cxt.hashCode.toString.takeRight(3) }"
		loge(s"${" " *  (30 - TAG.name.length)} [$name]:  ${msg.toString()}")
		msg.clear()
		super.onUnregistered(cxt)
	}
}
