package test

import test.Test.Monkey.NAME


object Test {

	/** */
	case class Address(street: String)


	object Monkey extends PropsHolder[Monkey] {
		object NAME extends StrProp[Monkey]("name")
		object AGE extends LngProp[Monkey]("age")
		object HEIGHT extends DblProp[Monkey]("height")
		object ADDR extends ObjProp[Address, Monkey]("address")
	}

	class Monkey extends Anything[Monkey]

	val m1 = new Monkey
	/**
	m1(Monkey.NAME) = "Mooky"
	m1(Monkey.AGE) = 12L
	m1(Monkey.HEIGHT) = 120.8
	m1(Monkey.ADDR) = Address("NY")
	  */
	val vals = Map("name" -> 10000, "age" -> 12, "height" -> "a- 123,98iuy87", "address" -> Address("NY"))
	m1.assign(vals)

	var n: String = m1(Monkey.NAME)
	println(n)
	var a: Long = m1(Monkey.AGE)
	println(a)
	var h: Double = m1(Monkey.HEIGHT)
	println(h)
	var adr: Address = m1(Monkey.ADDR)
	println(adr)
	println(m1.values)

	object Doggy extends PropsHolder[Doggy] {
		object NAME extends StrProp[Doggy]("name")
		object AGE extends LngProp[Doggy]("age")
		object HEIGHT extends DblProp[Doggy]("height")
		object ADDR extends ObjProp[Address, Doggy]("address")
	}

	class Doggy extends Anything[Doggy]

	val d1 = new Doggy

/**
	d1(Doggy.NAME) = "Gavki"
	d1(Doggy.AGE) = 11L
	d1(Doggy.HEIGHT) = 77.6
	d1(Doggy.ADDR) = Address("LA")
	println(Doggy.props)
*/

	d1.assign(Map("name" -> 10001, "age" -> "11.8", "height" -> 77.6, "address" -> Address("LA")))
	n = d1(Doggy.NAME)
	println(n)
	a = d1(Doggy.AGE)
	println(a)
	h = d1(Doggy.HEIGHT)
	println(h)
	adr = d1(Doggy.ADDR)
	println(adr)
	println{
		d1.values.collect{case (key, v) =>
			Doggy.props.find(_.name == key) match {
				case Some(prop: SqlProp[_, _, _]) => key -> prop.sqlTyp.cast(v)
				case _ => key -> v
			}
		}
	}


}
