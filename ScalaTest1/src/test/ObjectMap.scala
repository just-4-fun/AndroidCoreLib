package test

import scala.collection.immutable.TreeSet
import scala.util.Try
import java.lang.reflect.Field


/* ANYTHING */

trait ObjectMap[O <: ObjectMap[O]] {
	val schema: PropsSchema[O]
	private[this] var _values: Map[String, Any] = _
	private[this] var _updates = Map[String, Any]()

	def values(vals: Map[String, Any]): this.type = { _values = vals; this }
	def values = if (_values == null) updates else _values ++ updates
	def updates = {
		def fromField(fieldName: String) = Try {
			val f = getClass.getDeclaredField(fieldName)
			f.setAccessible(true)
			val v = f.get(this)
			if (v != null) _updates += fieldName -> v
		}
		//
		if (schema != null) schema.props.foreach { prop => fromField(prop.name) }
		_updates
	}

	def update[T](prop: Prop[T, _, O], v: T): Unit = set(prop, v)
	def set[S, T](prop: Prop[T, _, O], v: T): Unit = _updates += (prop.name -> v)

	def apply[T](prop: Prop[T, _, O]): T = get(prop)
	def get[T](prop: Prop[T, _, O]): T = prop.inType.cast {
		if (_updates.contains(prop.name)) _updates(prop.name)
		else if (_values != null) _values(prop.name)
		else null
	}
	override def toString: String = "???" // TODO JSON
}


/* SCHEMA */

trait PropsSchema[O <: ObjectMap[O]] {
	implicit val schema: PropsSchema[O] = this
	var props = TreeSet[Prop[_, _, O]]()
	def add(prop: Prop[_, _, O]) { if (prop.order == 0) prop.order = props.size + 1; props += prop }
}


/* PROPERTY */

trait Prop[TIN, TOUT, O <: ObjectMap[O]] extends Ordered[Prop[_, _, O]] {
	implicit val schema: PropsSchema[O]
	val name: String
	val inType: PropType[TIN]
	val outType: PropType[TOUT]
	var order: Int
	val extra: String

	// ADD  self to props collection
	if (schema != null) schema.add(this)

	override def compare(that: Prop[_, _, O]): Int = this.order compare that.order
}




case class StrProp[O <: ObjectMap[O]](name: String, extra: String = null, var order: Int = 0)(implicit val schema: PropsSchema[O]) extends Prop[String, String, O] {
	override val inType, outType: PropType[String] = StrType
}
case class LngProp[O <: ObjectMap[O]](name: String, extra: String = null, var order: Int = 0)(implicit val schema: PropsSchema[O]) extends Prop[Long, Long, O] {
	override val inType, outType: PropType[Long] = LngType
}
case class DblProp[O <: ObjectMap[O]](name: String, extra: String = null, var order: Int = 0)(implicit val schema: PropsSchema[O]) extends Prop[Double, Double, O] {
	override val inType, outType: PropType[Double] = DblType
}
case class ObjProp[O <: ObjectMap[O], T <: ObjectMap[_]](name: String, extra: String = null, var order: Int = 0)(objClass: Class[T])(implicit val schema: PropsSchema[O]) extends Prop[T, String, O] {
	override val inType: ObjType[T] = new ObjType[T](objClass)
	override val outType: PropType[String] = StrType
}
case class SeqProp[O <: ObjectMap[O], T](name: String, extra: String = null, var order: Int = 0)(objClass: Class[T])(implicit val schema: PropsSchema[O]) extends Prop[Seq[T], String, O] {
	override val inType: SeqType[T] = new SeqType[T](objClass)
	override val outType: PropType[String] = StrType
}

/* TYPES */

object PropType {
	val NumPattern = """[\D&&[^\.,\-]]*(\-* *)(\d*)[\.,]?(\d*)\D+.*""".r
	def toNumber[N](v: String, typ: PropType[N])(castCode: => N): N = {
		def toDouble(v: String): Double = try {
			var NumPattern(sig, rl, fl) = v
			val mult = if (sig.length == 1) -1 else 1
			if (rl.length == 0) rl = "0"
			if (fl.length == 0) fl = "0"
			s"$rl.$fl".toDouble * mult
		} catch {case _: Throwable => loge(s"toDouble format error in $v"); 0 }
		//
		try {castCode} catch {case _: NumberFormatException => typ.cast(toDouble(v)) }
	}
	// TODO replace to loge
	def loge(msg: String) { println(msg) }
}
trait PropType[T] {
	import PropType._
	def default: T
	protected[this] def castInternal(v: Any): T
	def cast(v: Any): T = if (v == null) default
	else try castInternal(v) catch {case _: Throwable => loge(s"Can't cast value $v"); default }
}

class ObjType[T <: ObjectMap[_]](objClass: Class[T]) extends PropType[T] {
	def default = null.asInstanceOf[T]
	def castInternal(v: Any): T = v match {
		case v: T => v
		case v: String => objectFromString(v)
		case _ => loge(s"Unknown type of $v"); default
	}
	def objectFromString(s: String): T = Try {
		val obj = objClass.newInstance
		val vals = Map[String, Any]()
		obj.values(vals)
		obj
	}.getOrElse(default)
}

class SeqType[T](elemClass: Class[T]) extends PropType[List[T]] {
	def default = null.asInstanceOf[List[T]]
	def castInternal(v: Any): List[T] = v match {
		case v: List[T] => v
		case v: String => listFromString(v)
		case _ => loge(s"Unknown type of $v"); default
	}
	def listFromString(s: String): List[T] = Try {
		val list = List[T]()
		//		val obj = elemClass.newInstance
		//		val vals = Map[String, Any]()
		//		obj.assign(vals)
		//		obj
		list
	}.getOrElse(default)
}

object StrType extends PropType[String] {
	def default = null
	def castInternal(v: Any): String = v.toString
}
object LngType extends PropType[Long] {
	def default = 0
	def castInternal(v: Any): Long = v match {
		case v: Long => v
		case v: Double => v.toLong
		case v: Int => v.toLong
		case v: Float => v.toLong
		case v: String => toNumber(v, this)(v.toLong)
		case _ => loge(s"Unknown type of $v"); default
	}
}
object DblType extends PropType[Double] {
	def default = 0
	def castInternal(v: Any): Double = v match {
		case v: Double => v
		case v: Long => v.toDouble
		case v: Int => v.toDouble
		case v: Float => v.toDouble
		case v: String => toNumber(v, this)(v.toDouble)
		case _ => loge(s"Unknown type of $v"); default
	}
}
object BytesType extends PropType[Array[Byte]] {
	def default = null
	def castInternal(v: Any): Array[Byte] = v match {
		case v: Array[Byte] => v
		case v: String => v.getBytes
		case _ => loge(s"Unknown type of $v"); default
	}
}

