package just4fun.android.demo1

import just4fun.android.core.app.{AppService, ActiveStateWatcher, App}
import just4fun.android.core.app.App._
import just4fun.android.core.async.Async._
import just4fun.android.core.sqlite.{DbService, Db, DbSchema}
import project.config.logging.Logger._

import scala.util.{Failure, Success}

object TestDbTable1 extends Loggable with ActiveStateWatcher{
	import App._
	config.singleInstance = true
	def apply() = {
		val dbsvc = new DbService().register()
		val tabA = new TabA(dbsvc).dependsOn(dbsvc).register()
		val tabB = new TabB(dbsvc).dependsOn(dbsvc).register()
		dbsvc.watchActiveStateChange(this)
		tabA.watchActiveStateChange(this)
		tabB.watchActiveStateChange(this)

//		new SERVICE_1().register()
//		new SERVICE_2().register()

	}

	class TabA(db: Db) extends DbSchema[TabA](db, "TabA") {
		val col1 = Str_prop()("col1")
		val col2 = Int_prop()("col2")
		val col3 = Boo_prop()("col3")
	}

	class TabB(db: Db) extends DbSchema[TabB](db, "TabB") {
		val col1 = Str_prop()("col1")
		val col2 = Int_prop()("col2")
		val col3 = Boo_prop()("col3")
	}



//	/*  service 1 */
//	class SERVICE_1 extends Service4test {
//		override protected def onStart(): Unit = {
//			post("Start", 2000) { _started = Success(true) }
//		}
//		override protected def onStop(): Unit = {
//			post("Stop", 5000) { _stopped = Success(true) }
//		}
//	}
//
//	/*  service 2 */
//	class SERVICE_2 extends Service4test {
//		override protected def onStart(): Unit = {
//			post("Start", 5000) { _started = Success(true) }
//		}
//		override protected def onStop(): Unit = {
//			post("Stop", 2000) { _stopped = Success(true) }
//		}
//	}
	/** Is called when Service s becomes available for use (STARTED) or unavailable (FAILED or stopped)
	  * @param service Service to watch
	  * @param active true - if s can be used; false otherwise
	  * @return  true - to keep watching; false - to stop watching
	  */
	override def onActiveStateChanged(service: AppService, active: Boolean): Boolean = {
		logw("onServiceAccessibility", s"srvc= ${service.ID};  aval= $active")
		if (active) service match {

			case s: TabA =>
				s.select().onCompleteInUi {
					case Success(list) => logw("TabA list= "+list.mkString("\n"))
					case Failure(ex) => loge(ex)
				}
				val obj = s().setValuesArray(List(null, "A object", 1, true))
				s.save(obj)

			case s: TabB =>
				s.select().onCompleteInUi {
					case Success(list) => logw("TabB list= "+list.mkString("\n"))
					case Failure(ex) => loge(ex)
				}
				val obj = s().setValuesArray(List(null, "B object", 1, true))
				s.save(obj)

			case s: DbService =>
		}
		true
	}
}
