import math._

object BitState {
	/** Lets using Enumeration values in methods params */
	implicit def enum2int(position: Enumeration#Value): Int = position.id
	//	implicit def enum2dbl(position: Enumeration#Value): Double = position.id
	def apply[EV <: Enumeration#Value](value: Long = 0): BitState[EV] = { val bs = new BitState[EV] {}; bs.value = value; bs }
	def firstDiff(oldStt: BitState[_], newStt: BitState[_], enum: Enumeration): Option[Enumeration#Value] = {
		def countPow(n: Double, sum: Int): Int = {
			val res = n / 2
			if (res < 1) sum + 1 else countPow(res, sum + 1)
		}
		val max = math.max(oldStt.value, newStt.value)
		var len = countPow(max, 0)
		if (max > 64) len = 64
		for (i <- 0 until len) {if (oldStt.has(i) != newStt.has(i)) return Some(enum(i))}
		None
	}
}

trait BitState[EV <: Enumeration#Value] {

	import BitState._

	private[this] var _value: Long = 0
	def value = _value
	def value_=(v: Long): Unit = _value = v

	def isZero: Boolean = _value == 0

	def setOnly(eVal: EV*): BitState[EV] = {
		_value = 0
		for (v <- eVal) _value |= bitVal(v)
		println(s"SETONLY:   val=$eVal;   VALUE= $value;  BITS= ${this.toString}")
		this
	}

	def set(eVal: EV, yes: Boolean): BitState[EV] = {
		if (yes) _value |= bitVal(eVal) else _value = ~bitVal(eVal) & _value
		println(s"SET:   val= $eVal;   VALUE= $value;  BITS= ${this.toString}")
		this
	}

	def set(eVal: EV*): BitState[EV] = {
		for (v <- eVal) _value |= bitVal(v)
		println(s"SET:   val= $eVal;   VALUE= $value;  BITS= ${this.toString}")
		this
	}

	def clear(eVal: EV*): BitState[EV] = {
		if (eVal.isEmpty) _value = 0
		else for (v <- eVal) _value = ~bitVal(v) & _value
		println(s"CLEAR:   val= $eVal;   VALUE= $value;  BITS= ${this.toString}")
		this
	}

	def toggle(eVal: EV*): BitState[EV] = {
		for (v <- eVal) _value ^= bitVal(v)
		println(s"TOGGLE:    val= $eVal;   VALUE= $value;  BITS= ${this.toString}")
		this
	}

	def has(eVal: EV*): Boolean = {
		for (v <- eVal) {if ((_value & bitVal(v)) != 0) return true}
		false
	}

	def has(pos: Int): Boolean = {
		(_value & bitVal(pos)) != 0
	}

	def hasNo(eVal: EV*): Boolean = {
		for (v <- eVal) {if ((_value & bitVal(v)) == 0) return true}
		false
	}

	def hasAll(eVal: EV*): Boolean = {
		for (v <- eVal) {if ((_value & bitVal(v)) == 0) return false}
		true
	}

	def hasNoAll(eVal: EV*): Boolean = {
		for (v <- eVal) {if ((_value & bitVal(v)) != 0) return false}
		true
	}

	def hasOnly(eVal: EV*): Boolean = {
		var _val: Long = 0
		for (v <- eVal) _val |= bitVal(v)
		_value == _val
	}

	def hasNoOnly(eVal: EV*): Boolean = {
		var _val: Long = _value
		for (v <- eVal) _val = ~bitVal(v) & _val
		_value == _val
	}

	private def bitVal(pos: Int): Long = pow(2, pos).toLong


	override def toString: String = {
		val text: StringBuilder = new StringBuilder
		var res: Float = _value / 2f
		var $: Int = 0
		do {
			res = res / 2f
			text.append(if (text.isEmpty) "" else " ").append(if (has($)) "1" else "0")
			$ += 1
		} while (res >= 0.5)
		text.toString()
	}
	override def clone(): BitState[EV] = BitState(value)


	/*
		def toString(values: Enumeration#ValueSet): String = {
		  val text: StringBuilder = new StringBuilder
		  for (v: Float <- _values) {
			if (has(v)) text.append(if (text.isEmpty) "" else ", ").append(v.toString)
		  }
		  text.toString()
		}
	  */
}
