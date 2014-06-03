package test

object Tmp {
	abstract class Type[T](val clas: Class[T], val default: T, val empty: T) {
		def convert[R](v: T, typ: Type[R]): R = cast(v, typ).asInstanceOf[R]
		def cast(v: T, typ: Type[_]): Any
	}

	class Anymal
	class Monkey extends Anymal

	object Type {
		//		object ANYMAL[T <: Anymal] extends Type[T <: Anymal]

		object STRING extends Type[String](classOf[String], null, "") {
			def cast(v: String, typ: Type[_]): Any  = {
				typ match {
					case STRING => v
					case LONG => v.toLong
					//					case INT => v.toInt
					//					case DOUBLE => v.toDouble
					//					case FLOAT => v.toFloat
					//					case SHORT => v.toShort
					//					case CHAR => val arr = v.toCharArray; if (arr.isEmpty) typ.default else arr(0)
					//					case BYTE => v.toByte
					//					case BOOLEAN => v.toBoolean
					//					case OBJECT => v.toString
					case _ => /**logw("Casr", s"Can't  cast $v to unknown ${typ.clas}. Take default."); */typ.default
				}
			}
		}
		object LONG extends Type[Long](classOf[Long], 0, 0) {
			def cast(v: Long, typ: Type[_]): Any = {
				typ match {
					case STRING => v.toString
					case LONG => v
					//					case INT => v.toInt
					//					case DOUBLE => v.toDouble
					//					case FLOAT => v.toFloat
					//					case SHORT => v.toShort
					//					case CHAR => v.toChar
					//					case BYTE => v.toByte
					//					case BOOLEAN => v.toBoolean
					//					case OBJECT => throw new ClassCastException(s"Can't  cast $v to ${typ.clas}")
					case _ => typ.default
				}
			}

		}
		//		val INT = new Type[Int](classOf[Int], 0, 0)
		//		val SHORT = new Type[Short](classOf[Short], 0, 0)
		//		val CHAR = new Type[Char](classOf[Char], 0, 0)
		//		val BYTE = new Type[Byte](classOf[Byte], 0, 0)
		//		val DOUBLE = new Type[Double](classOf[Double], 0, 0)
		//		val FLOAT = new Type[Float](classOf[Float], 0, 0)
		//		val BOOLEAN = new Type[Boolean](classOf[Boolean], false, false)
		//		val BYTES = new Type[Array[Byte]](classOf[Array[Byte]], null, Array[Byte]())
		//		val OBJECT = new Type[AnyRef](classOf[AnyRef], null, null)
	}


	//	def cast[T](v: Any, cls: Class[T]): T = {
	//		def castStr(v: String): T = cls match {
	//			case cls: String =>
	//		}
	//		def castInt(v: Long): T = {}
	//		def castFlt(v: Double): T = {}
	//		def castBool(v: Boolean): T = {}
	//		def castObject(v: AnyRef): T = {}
	//		def castBytes(v: Array[Byte]): T = {}
	//		//
	//		v match {
	//			case v: T => v
	//			case null => null
	//			case v: String => castStr(v)
	//			case v: Long => castInt(v)
	//			case v: Double => castFlt(v)
	//			case v: Int => castInt(v)
	//			case v: Float => castFlt(v)
	//			case v: Boolean => castBool(v)
	//			case v: Short => castInt(v)
	//			case v: Byte => castInt(v)
	//			case v: Char => castInt(v)
	//			case v: Array[Byte] => castBytes(v)
	//			case v: AnyRef => castObject(v)
	//			case _ => v.asInstanceOf[T]
	//		}
	//	}

	val n = Type.STRING.convert("12", Type.LONG)
	println(n.getClass)

	//	println(castStr("4", Type.INT))
	//	var v: Float = castStr("4.8", Type.FLOAT)
	//	println(v)


}
