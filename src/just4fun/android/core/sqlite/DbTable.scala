package just4fun.android.core.sqlite

import just4fun.android.core.app.AppService
import scala.util.{Try, Failure, Success}
import android.database.Cursor
import just4fun.android.core.persist.IntVar
import just4fun.android.core.utils.Logger._
import just4fun.android.core.async._
import scala.collection.{GenTraversableOnce => Coll, SortedSet}
import scala.annotation.tailrec



/* SERVICE */
trait DbTableService extends AppService {self: DbTable =>
	var openComplete, closeComplete: Try[Boolean] = Success(false)

	override protected def onStart(): Unit = {
		closeComplete = Success(false)
		open().onComplete {
			case Success(_) => openComplete = Success(true)
			case Failure(e) => openComplete = Failure(e)
		}
	}
	override protected def isStarted: Boolean = openComplete.get
	override protected def onStop(): Unit = {
		openComplete = Success(false)
		close().onComplete {
			case Success(_) => closeComplete = Success(true)
			case Failure(e) => closeComplete = Failure(e)
		}
	}
	override protected def isStopped: Boolean = closeComplete.get
}



/* TABLE */
class DbTable[O <: DbObject[O]](db: Db, name: String, objectClass: Class[O]) {
	implicit val dbTable = this
	var columns = SortedSet[DbColumn[_, O]]()

	/* COLUMNS */
	val _ID = ColumnID[O]() // default sqlite column


	/* OVERRIDE */

	protected def getIndexes: List[DbTableIndex] = List.empty
	protected def getUpgrades: List[String] = List.empty

	//SYNCHRONOUS TABLE LIFECYCLE CALLBACKS

	protected def onCreateSync(): Unit = ()
	protected def onOpenSync(): Unit = ()
	protected def onCloseSync(): Unit = ()


	/* OBJECT */

	def selectSync(where: String = null, cols: Set[DbColumn[_, O]] = null, orderBy: String = null, limit: String = null): Try[Seq[O]] = {
		def prepare(cursor: Cursor) = {

		}
		def newObj(cursor: Cursor): O = {
			val obj = objectClass.newInstance()
			cursor.get
			obj
		}
		@tailrec def next(list: Seq[O], cursor: Cursor): Seq[O] =
			if (cursor.moveToNext()) next(list :+ newObj(cursor), cursor) else list
		// EXEC
		val projection = if (cols == null) columns else cols
		val q = db.buildQuery(name, projection.map(_.name).toArray, where, orderBy = orderBy, limit = limit)
		db.selectNCloseSync(q) {
			case Success(cursor) => prepare(cursor); Success(next(Seq[O](), cursor))
			case Failure(e) => Failure(e)
		}
	}






	/* INTERNAL API */

	def fullName: String = db.dbName + "_" + name

	private[sqlite] def addColumn(col: DbColumn[_, O]) = {
		if (col.order == 0) col.order = columns.size
		columns = columns + col
	}

	def open(): FutureExt[Unit] = {
		type Column = (String, Class[_])
		val indexes = getIndexes
		val upgrades = getUpgrades
		val version = IntVar(s"${fullName }_version", -1)
		//
		/* DEFs */
		//
		def createTableNIndexes(): Unit = {
			val cols = columns.withFilter(col => col.storeCls >= 0).map(_.createSql).mkString(",")
			execQuery(s"CREATE TABLE IF NOT EXISTS $name ($cols)", false)
			// INDEXES
			getIndexes.foreach { index =>
				val cols = index.columns.mkString(",")
				execQuery(s"CREATE INDEX IF NOT EXISTS ${index.name } ON $name ($cols)")
			}
		}
		def applyUpgrades() = upgrades.zipWithIndex.foreach { case (upgrade, ix) =>
			if (ix >= version) db.execSqlSync(upgrade) match {case Failure(e) => loge(e) case _ => }
		}
		def adaptSchema(): Unit = {
			val oldColumns = loadColumns()
			val oldIndexes = loadIndexes()
			var newCols = columns.map(col => (col.name, col.storeCls)).toSet
			var mutualCols = List[String]()
			var recreate = false
			// Collect column info
			oldColumns.foreach { oldCol =>
				newCols.find(_._1 == oldCol._1) match {
					case None => recreate = true
					case Some(newCol) => if (newCol._2 != oldCol._2) recreate = true
						newCols = newCols - newCol
						mutualCols = oldCol._1 :: mutualCols
				}
			}
			// APPLY
			if (recreate) {
				dropIndexes(oldIndexes)
				recreateTable(mutualCols)
			}
			else {
				addColumns(newCols)
//				reorderColumns(oldColumns ++ newCols)
				val newIndexes = indexes.map(_.name).toSet
				dropIndexes(oldIndexes diff newIndexes)
				createIndexes(newIndexes diff oldIndexes)
			}
		}
		def recreateTable(cols: Coll[String]) = {
			// rename old table
			val oldTable = "_" + name
			execQuery(s"ALTER TABLE $name RENAME TO $oldTable")
			// create new table and indexes
			createTableNIndexes()
			// copy old values to new table
			val colStr = cols.mkString(",")
			execQuery(s"INSERT INTO $name ($colStr) SELECT $colStr FROM  $oldTable")
			// drop old table
			execQuery(s"DROP TABLE IF EXISTS $oldTable")
		}
		def dropIndexes(dropIndexes: Coll[String]) = dropIndexes.foreach { index =>
			execQuery(s"DROP INDEX IF EXISTS $index")
		}
		def createIndexes(addIndexes: Coll[String]) = addIndexes.foreach { index =>
			val dbIndex = indexes.find(_.name == index).get
			execQuery(s"CREATE INDEX IF NOT EXISTS $index ON $name (${dbIndex.columns.mkString(",") })")
		}
		def addColumns(cols: Coll[Column]) = {
			cols.foreach { colPair =>
				val col = columns.find(_.name == colPair._1).get
				execQuery(s"ALTER TABLE $name ADD COLUMN ${col.createSql }")
			}
		}
		def loadColumns(): Seq[Column] = {
			def next(cols: Seq[Column], cursor: Cursor, nameIx: Int, typeIx: Int): Seq[Column] = if (cursor.moveToNext) {
				val name = cursor.getString(nameIx)
				val typ = SqlType.index(cursor.getString(typeIx))
				next(cols :+ (name, typ), cursor, nameIx, typeIx)
			} else cols
			//
			db.selectNCloseSync(s"PRAGMA table_info($name)") { cursorTry =>
				val cursor = cursorTry.get
				val nameIx = cursor.getColumnIndex("name")
				val typeIx = cursor.getColumnIndex("type")
				next(Seq[Column](), cursor, nameIx, typeIx)
			}
		}
		def loadIndexes(): Set[String] = {
			def next(indexes: Set[String], cursor: Cursor, nameIx: Int): Set[String] = if (cursor.moveToNext) {
				val name = cursor.getString(nameIx)
				next(indexes + name, cursor, nameIx)
			} else indexes
			//
			db.selectNCloseSync(s"PRAGMA index_list($name)") { cursorTry =>
				val cursor = cursorTry.get
				val nameIx = cursor.getColumnIndex("name")
				next(Set[String](), cursor, nameIx)
			}
		}
		def reorderColumns(realCols: Seq[Column]) = {
			var n = 1
			realCols.foreach { col =>
				val dbCol = columns.find{dbCol => dbCol.name == col._1}.get
				dbCol.order = n
				n += 1
			}
		}
		def execQuery(q: String, silent: Boolean = true) = db.execSqlSync(q) match {
			case Failure(e) => if (silent) loge(e) else throw e
			case _ =>
		}
		//
		/* EXECUTION */
		//
		post(s"open $fullName") {
			if (version == -1) {
				createTableNIndexes()
				onCreateSync()
			}
			else {
				applyUpgrades()
				adaptSchema()
			}
			version ~ upgrades.size
			onOpenSync()
		}(db.dbExecContext)
	}

	def close(): FutureExt[Unit] = post(s"close $fullName") { onCloseSync() }(db.dbExecContext)



	/* INDEX */
	case class DbTableIndex(name: String, columns: String)


}



