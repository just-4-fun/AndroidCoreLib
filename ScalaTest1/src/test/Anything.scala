package test

import scala.collection.immutable.TreeSet


/* ANYTHING */

class Anything[O <: Anything[O]] {

	private[this] var _values: Map[String, Any] = _
	private[this] var _updates = Map[String, Any]()

	def assign(vals: Map[String, Any]) = _values = vals
	def values = if (_values == null) updates else _values ++ updates
	def updates = _updates

	def update[T](prop: Prop[T, O], v: T): Unit = set(prop, v)
	def set[T](prop: Prop[T, O], v: T): Unit = _updates += (prop.name -> v)

	def apply[T](prop: Prop[T, O]): T = get(prop)
	def get[T](prop: Prop[T, O]): T = prop.typ.cast {
		if (_updates.contains(prop.name)) _updates(prop.name)
		else if (_values != null) _values(prop.name)
		else null
	}
}


/* HOLDLER */

trait PropsHolder[O <: Anything[O]] {
	implicit val propsHolder: PropsHolder[O] = this
	var props = TreeSet[Prop[_, O]]()
	def add(prop: Prop[_, O]) { if (prop.order == 0) prop.order = props.size + 1; props += prop }
}


/* PROPERTY */

trait Prop[T, O <: Anything[O]] extends Ordered[Prop[_, O]] {
	val name: String
	val typ: PropTyp[T]
	var order: Int
	implicit val props: PropsHolder[O]
	// ADD  self to props collection
	if (props != null) props.add(this)

	override def compare(that: Prop[_, O]): Int = this.order compare that.order
}


trait SqlProp[S, T, O <: Anything[O]] extends Prop[T, O] {
	val sqlTyp: PropTyp[S]
	val constraint: String
}


case class StrProp[O <: Anything[O]](name: String, constraint: String = null, var order: Int = 0)(implicit val props: PropsHolder[O]) extends SqlProp[String, String, O] {
	override val typ, sqlTyp: PropTyp[String] = StrTyp
}
case class LngProp[O <: Anything[O]](name: String, constraint: String = null, var order: Int = 0)(implicit val props: PropsHolder[O]) extends SqlProp[Long, Long, O] {
	override val typ, sqlTyp: PropTyp[Long] = LngTyp
}
case class DblProp[O <: Anything[O]](name: String, constraint: String = null, var order: Int = 0)(implicit val props: PropsHolder[O]) extends SqlProp[Double, Double, O] {
	override val typ, sqlTyp: PropTyp[Double] = DblTyp
}
case class ObjProp[T <: Object, O <: Anything[O]](name: String, constraint: String = null, var order: Int = 0)(implicit val props: PropsHolder[O]) extends SqlProp[String, T, O] {
	override val typ: ObjTyp[T] = new ObjTyp[T]
	override val sqlTyp: PropTyp[String] = StrTyp
}

/* TYPES */

object PropTyp {
	val NumPattern = """[\D&&[^\.,\-]]*(\-* *)(\d*[\.,]?\d*)\D+.*""".r
}
trait PropTyp[T] {
	import PropTyp._
	def default: T
	protected[this] def castInternal(v: Any): T
	def cast(v: Any): T = if (v == null) default
	else try castInternal(v) catch {case _: Throwable => loge(s"Can't cast value $v"); default }
	// TODO replace to loge
	def loge(msg: String) { println(msg) }
	def toNumber[N](v: String, typ: PropTyp[N])(castCode: => N): N = {
		def toDouble(v: String): Double = try {
			val NumPattern(sig, n) = v
			val mult = if (sig.length == 1) -1 else 1
			n.replace(',', '.').toDouble * mult
		} catch {case _: Throwable => loge(s"toDouble format error in $v"); 0 }
		//
		try {castCode} catch {case _: NumberFormatException => typ.cast(toDouble(v)) }
	}
}

class ObjTyp[T <: Object] extends PropTyp[T] {
	def default = null.asInstanceOf[T]
	def castInternal(v: Any): T = v match {
		case v: T => v
		case v: String => objectFromString(v)
		case _ => loge(s"Unknown type of $v"); default
	}
	def objectFromString(s: String): T = ??? // TODO Json
}

object StrTyp extends PropTyp[String] {
	def default = null
	def castInternal(v: Any): String = v.toString
}
object LngTyp extends PropTyp[Long] {
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
object DblTyp extends PropTyp[Double] {
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
object BytesTyp extends PropTyp[Array[Byte]] {
	def default = null
	def castInternal(v: Any): Array[Byte] = v match {
		case v: Array[Byte] => v
		case v: String => v.getBytes
		case _ => loge(s"Unknown type of $v"); default
	}
}

