package just4fun.android.demo1

import just4fun.android.core.app.{AppService, ParallelThreadFeature, AppServiceContext, App}
import just4fun.android.core.sqlite.DbService
import just4fun.android.core.utils._
import just4fun.android.core.persist._
import just4fun.android.core.inet.{InetRequest, OnlineStatusListener, InetOptions, InetService}
import just4fun.android.core.{inet, async}
import project.config.logging.Logger._

trait AppTest
object Inet extends InetService with ParallelThreadFeature

class AppDemo extends App {

	//	Test1
	//	TestKeepOnExit
	//	TestTimeout
//	TestSequenceSingleSimple
	//	TestSequenceMultiSimple
	//	TestSharedSingle
	//	TestSharedMulty
	//	TestDependMultiSimple1
	//	TestDependMultiSimple2
	//	TestDependShared2
	//	TestDependFILO1
	TestDbTable1



	override def isServiceStartFatalError(service: AppService, err: Throwable): Boolean = {
		service match {
			case s: DbService => true
			case _ => false
		}
	}
	object TestInet extends OnlineStatusListener {
		override def onlineStatusChanged(isOnline: Boolean): Unit = if (isOnline) {
			val futRes = Inet.loadString(InetOptions("http://www.google.com"))
			futRes.onSuccessInUi {
				case text => logi("RESULT Ok= " + text)
			}
			futRes.onFailureInUi{
				case e: Throwable => loge(msg = s"RESULT Err= $e")
			}
		}
	}

	override def onRegisterServices(implicit sm: AppServiceContext): Unit = {
		logv("onRegisterInstanceServices", "")
		Inet.register()
		Inet.addListener(TestInet)


		//		val v1 = new IntVar("v1", temp = true)
		//		logw("VAR TEST", s"${v1}")
		//		v1.set(88)
		//		logw("VAR TEST", s"${v1}")
		//
		//		val n: Int = v1 + 2
		//		logw("TEST IMPLICIT", s"$n")
		//
		//		val v2 = new DoubleVar("v2", 123456789.0000001)
		//		logw("DOUBLE VAR TEST", s"${v2}")
		//		v2.set(0.0001200034)
		//		logw("DOUBLE VAR TEST", s"${v2}")
		//
		//		val v3 = new StringVar("v311")
		//		v3 ~ "3.554"
		//		logw("STR VAR TEST", s"$v3")
		//		val v33 = new FloatVar("v311")
		//		logw("FLOAT VAR TEST", s"$v33")
		//
		//		val vBoo = new BoolVar("boo1")
		//		logw("BOO VAR TEST", s"$vBoo")
		//		vBoo ~ true
		//		logw("BOO VAR TEST", s"$vBoo")



		//		Test1()
		//		TestKeepOnExit()
		//		TestTimeout()
//		TestSequenceSingleSimple()
		//		TestSequenceMultiSimple()
		//		TestSharedSingle()
		//		TestSharedMulty()
		//		TestDependMultiSimple1()
		//		TestDependMultiSimple2()
		//		TestDependShared2()
		//		TestDependFILO1()
		TestDbTable1()

	}

	override def onExited(): Unit = {
		Test.check()
	}
}

/* TESTING API */

object Test extends Loggable {
	var messages: Seq[Any] = List()
	val generated = collection.mutable.ArrayBuffer[TestMsg]()
	def apply(msg: TestMsg) = generated += msg
	def check() = TryNLog {
		val str = new StringBuilder
		val res = for (n <- 0 until messages.length) {
			val s = if (generated(n).id == messages(n)) "Ok" else s"${messages(n) } # ${generated(n).id }"
			if (str.nonEmpty) str ++= "  :  "
			str ++= s
		}
		loge(msg = "TEST CHECK >>     " + str.toString())
		//		if (generated.length> messages.length)
		loge(msg = "TEST GENERATED >>     " + generated.map(_.id).mkString("  :  "))
		generated.clear()
	}
}

case class TestMsg(id: Any) {
	Test(this)
}
