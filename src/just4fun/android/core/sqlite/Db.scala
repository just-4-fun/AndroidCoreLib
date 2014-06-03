package just4fun.android.core.sqlite

import android.database.sqlite.{SQLiteException, SQLiteQueryBuilder, SQLiteDatabase}
import scala.util.{Failure, Success, Try}
import android.database.Cursor
import just4fun.android.core.utils._
import just4fun.android.core.utils.Logger._
import android.content.ContentValues
import just4fun.android.core.async._
import just4fun.android.core.async.FutureExt

class Db extends Loggable {self: DbService =>
	var db: SQLiteDatabase = _
	def dbExecContext = execContext
	def dbName = name

	/* ASYNC USAGE */

	/**
	  */
	def execSql(sql: String): FutureExt[Unit] = post("execSql", replace = false) { execSqlSync(sql) }
	def select(sql: String): FutureExt[Cursor] = post("select", replace = false) { selectSync(sql).get }
	def selectNClose[T](sql: String): (FutureExt[Cursor] => T) => T = {
		// DEFs
		def autoCloseCursor[T, U](cursorTry: FutureExt[Cursor])(code: FutureExt[Cursor] => T): T =
			try {code(cursorTry)}
			finally try {cursorTry.foreach(c => if (!c.isClosed) c.close())} catch {case _: Throwable => }
		// EXEC
		val cursorFut = select(sql)
		autoCloseCursor(cursorFut)
	}
	def insert(table: String, values: ContentValues): FutureExt[Long] =
		post("insert", replace = false) { insertSync(table, values).get }
	def update(table: String, values: ContentValues, where: String = null): FutureExt[Int] =
		post("update", replace = false) { updateSync(table, values).get }
	def delete(table: String, where: String = null): FutureExt[Int] =
		post("delete", replace = false) { deleteSync(table, where).get }

	/* SYNC USAGE */

	def execSqlSync(sql: String): Try[Unit] = ifAvailable {
		logv("execSql", sql)
		db.execSQL(sql)
	}
	/**
	  */
	def buildQuery(table: String, columns: Array[String] = null, where: String = null, groupBy: String = null, having: String = null, orderBy: String = null, limit: String = null, distinct: Boolean = false): String = {
		SQLiteQueryBuilder.buildQueryString(distinct, table, columns, where, groupBy, having, orderBy, limit)
	}
	/**
	  */
	def selectSync(sql: String): Try[Cursor] = {
		logv("select", sql)
		ifAvailable { db.rawQuery(sql, null) }
	}
	/**
	 * @example selectNCloseSync("SELECT * FROM Tab") { _.foreach { cursor => val x = cursor.getInt(0) } }
	 * @example val num: Int = selectNCloseSync("SELECT * FROM Tab") { case Success(cursor) => cursor.getInt(0) case Failure(_) => 0 }
	 */
	val num: Int = selectNCloseSync("SELECT * FROM Tab") { case Success(cursor) => cursor.getInt(0) case Failure(_) => 0 }
	def selectNCloseSync[T](sql: String): (Try[Cursor] => T) => T = {
		// DEFs
		def autoCloseCursor[T](cursorTry: Try[Cursor])(code: Try[Cursor] => T): T = try {code(cursorTry)}
		finally try {cursorTry.foreach(c => if (!c.isClosed) c.close())} catch {case _: Throwable => }
		// EXEC
		val cursorTry = selectSync(sql)
		autoCloseCursor(cursorTry)
	}

	/**
	 * @return  [[Success]] of row ID of the newly inserted row, or [[Failure]] otherwise
	 */
	def insertSync(table: String, values: ContentValues): Try[Long] = ifAvailable {
		logv("insert", s"Tab: $table,  values: ${printObject(values) }")
		db.insert(table, null, values) match {
			case -1 => throw new SQLiteException("Insertion failed")
			case id => id
		}
	}
	/**
	  */
	def updateSync(table: String, values: ContentValues, where: String = null): Try[Int] = ifAvailable {
		logv("update", s"Tab: $table,  where: $where,  values: ${printObject(values) }")
		db.update(table, values, where, null)
	}
	/**
	 * @note To remove all rows and get a count pass "1" as the whereClause.
	 */
	def deleteSync(table: String, where: String = null): Try[Int] = ifAvailable {
		logv("delete", s"Tab: $table,  where: $where")
		db.delete(table, where, null)
	}

	def withinTransaction[T](code: => T): T = {
		try {
			db.beginTransaction()
			val res = code
			db.setTransactionSuccessful()
			res
		}
		finally if (db.inTransaction()) db.endTransaction()
	}


}