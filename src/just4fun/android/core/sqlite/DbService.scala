package just4fun.android.core.sqlite

import just4fun.android.core.app.{ParallelThreadFeature, App, AppService}
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}

class DbService(val name: String = "main") extends AppService with ParallelThreadFeature {self: Db =>

	override protected def onInitialize(): Unit = {
		val dbHelper = new SQLiteOpenHelper(App(), name, null, 1) {
			override def onCreate(db: SQLiteDatabase): Unit = {}
			override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit = {}
		}
		db = dbHelper.getWritableDatabase // SQLiteException if the database cannot be opened for writing
	}
	override protected def isStarted: Boolean = db != null && db.isOpen
	override protected def onStop(): Unit = execContext.quit(true)
	override protected def isStopped: Boolean = execContext.isQuit
	override protected def onFinalize(): Unit = { if (isStarted) db.close(); db = null }
}
