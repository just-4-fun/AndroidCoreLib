package dbtest


trait DbObject[O <: DbObject[O]] {
	var values = collection.immutable.Map[String, Any]()

	def apply[T](column: DbColumn[T, O]): T = {
		values(column.name).asInstanceOf[T]
	}
	def update[T](column: DbColumn[T, O], v: T) = values += (column.name -> v)

}

class DbColumn[T, O <: DbObject[O]](val name: String) { }

case class ColumnStr[O <: DbObject[O]](nm: String) extends DbColumn[String, O](nm)
case class ColumnInt[O <: DbObject[O]](nm: String) extends DbColumn[Int, O](nm)


class DbTable[O <: DbObject[O]] {
}

/* COLUMN TYPES */

//case class ColumnStr[O <: DbObject](var name: String) extends DbColumn[String, O]
//case class ColumnInt[O <: DbObject](var name: String) extends DbColumn[Long, O]
