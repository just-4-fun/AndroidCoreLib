package just4fun.android.core.sqlite


trait DbObject[O <: DbObject[O]] {
	private[this] var updates = Map[DbColumn[_, O], Any]()
	private[this] var values: Map[String, Any] = _

	def putAll(vals: Map[String, Any]) = values = vals
	def getNew = updates.map { case (col, v) => col.name -> cast(v, col.storeCls) }
	def getAll = if (values == null) getNew else values ++ getNew

	def apply[T](column: DbColumn[T, O]): T = get(column)
	def get[T](column: DbColumn[T, O]): T = {
		val v = if (updates.contains(column)) updates(column)
		else if (values != null) values(column.name)
		else null
		cast(v, column.usageCls)
	}

	def update[T](column: DbColumn[T, O], v: T) = set(column, v)
	def set[T](column: DbColumn[T, O], v: T): T = {updates += (column -> v); v}


	def cast[T](v: Any, cls: Class[T]): T = {
		def castStr(v: String): T = cls match {
			case cls: String =>
		}
		def castInt(v: Long): T = {}
		def castFlt(v: Double): T = {}
		def castBool(v: Boolean): T = {}
		def castObject(v: AnyRef): T = {}
		def castBytes(v: Array[Byte]): T = {}
		//
		v match {
			case v: T => v
			case null => null
			case v: String => castStr(v)
			case v: Long => castInt(v)
			case v: Double => castFlt(v)
			case v: Int => castInt(v)
			case v: Float => castFlt(v)
			case v: Boolean => castBool(v)
			case v: Short => castInt(v)
			case v: Byte => castInt(v)
			case v: Char => castInt(v)
			case v: Array[Byte] => castBytes(v)
			case v: AnyRef => castObject(v)
			case _ => v.asInstanceOf[T]
		}
	}
}
