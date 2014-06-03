package just4fun.android.demo1

import just4fun.android.core.app.{ParallelThreadFeature, AppServiceContext, ServiceState, AppService, App}
import just4fun.android.core.async._
import android.os.HandlerThread
import just4fun.android.core.async.FutureExt
import just4fun.android.core.utils.Logger._
import just4fun.android.core.async.FutureExt
import scala.util.Try

object Test1 {
	import App._
	app.singleInstance = false
	def apply() = {
		TestService_2.register()
		val ts1: TestService_1 = new TestService_1().register() //.dependsOn(TestService_2)
	}
}


/*  service 1 */

object TestService_1 {
	val ID = "SERVICE 1"
}

class TestService_1 extends Service4test {
	override def ID: String = TestService_1.ID

	override protected def onStart(): Unit = {
		TestService_2.watchAccessibility(this)
		post("Start", 5000, false) { _started = true;  callService2()}
		callService2()
	}
	override protected def onStop(): Unit = {
		post("stop", 5000, false) { _stopped = true }
	}
	override def onServiceAccessibility(s: AppService, available: Boolean) = {
		s match {
			case s: TestService_2.type => logi(s"Expected  ${s.ID }  availability =  ${available }")
			case _ =>  logi(s"Unexpected  ${s.ID }  availability =  ${available }")
		}
		true
	}
	def callService2() {
		App.withService[TestService_2.type](TestService_2.ID) { s =>
			s.useAsync("Use 3").onComplete { v =>
				logi("useAsync.onComplete", s"THR= ${Thread.currentThread.getName };  FUTURE complete with $v")
			}
		}
	}
}


/*  service 2 */

object TestService_2 extends Service4test with ParallelThreadFeature {
	import ServiceState._
	override def ID: String = "SERVICE 2"

	override protected def onStart(): Unit = {
		post("start", 2000, false) { _started = true }
	}
	override protected def onStop(): Unit = {
		post("stop", 2000, false) { _stopped = true }
	}
	def use()(implicit context: AppServiceContext): Try[String] = ifAvailable { "USED" }
	//	def useAsync()(implicit context: AppServiceContext): FutureExt[Int] = post("Test", 0) {
	//		logw("use", s"THR= ${Thread.currentThread.getName};  FUTURE USE started")
	//		111111111
	//	}
	def useAsync(name: String): FutureExt[String] = {
		post(name) {
			val res = use()
			logi("useAsync.post", s"Available ? ${state == STARTED };  Res = $res")
			res.get
		}
	}
}

