package just4fun.android.core.sqlite

import just4fun.android.core.sqlite.DbTable

/** @see [[android.database.Cursor]].getType */
object SqlType {
	val NONE = -1
	val NULL = 0
	val INTEGER = 1
	val FLOAT = 2
	val STRING = 3
	val BLOB = 4

	def name(cls: Class[_]): String = cls match {
		case cls: Long => "INTEGER"
		case cls: Double => "FLOAT"
		case cls: Array[Byte] => "BLOB"
		case _ => "STRING"
	}
	def index(typ: String): Class[_] = typ.toUpperCase match {
		case "INTEGER" => classOf[Long]
		case "FLOAT" => classOf[Double]
		case "BLOB" => classOf[Array[Byte]]
		case _ =>  classOf[String]
	}
}

abstract class DbColumn[T, O <: DbObject[O]](implicit dbTable: DbTable[O]) extends Ordered[DbColumn[_, O]] {
	var order: Int
	var name: String
	var usageCls: Class[T]
	var storeCls: Class[_]
	var constraint: String

	// CONSTRUCT
	if (dbTable != null) dbTable.addColumn(this)

	// Ordering impl
	def compare(that: DbColumn[_, O]): Int = this.order compare that.order

	override def toString: String = s"${getClass.getSimpleName }::$name;  type= $storeCls;  order= $order"
	def createSql = s"$name ${SqlType.name(storeCls) }${if (constraint != null) " " + constraint }"

}


/* COLUMN TYPES */

case class ColumnStr[O <: DbObject[O]]
(var name: String,
  var order: Int = 0,
  var constraint: String = null)
  (implicit dbTable: DbTable[O])
  extends DbColumn[String, O] {
	var usageCls = classOf[String]
	var storeCls = classOf[String]
}

case class ColumnInt[O <: DbObject[O]]
(var name: String,
  var order: Int = 0,
  var constraint: String = null)
  (implicit dbTable: DbTable[O])
  extends DbColumn[Long, O] {
	var usageCls = classOf[Long]
	var storeCls = classOf[Long]
}

case class ColumnFlt[O <: DbObject[O]]
(var name: String,
  var order: Int = 0,
  var constraint: String = null)
  (implicit dbTable: DbTable[O])
  extends DbColumn[Double, O] {
	var usageCls = classOf[Double]
	var storeCls = classOf[Double]
}

case class ColumnBlob[O <: DbObject[O]]
(var name: String,
  var order: Int = 0,
  var constraint: String = null)
  (implicit dbTable: DbTable[O])
  extends DbColumn[Array[Byte], O] {
	var usageCls = classOf[Array[Byte]]
	var storeCls = classOf[Array[Byte]]
}

//
case class ColumnID[O <: DbObject[O]]
(var order: Int = 0, var constraint: String = "PRIMARY KEY AUTOINCREMENT")
  (implicit dbTable: DbTable[O])
  extends DbColumn[Long, O] {
	var name = "_id"
	var usageCls = classOf[Long]
	var storeCls = classOf[Long]
}
