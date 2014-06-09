package test

import android.util.{JsonToken, JsonReader}


object Test {

//	JsonReader

	/** */
	case class Address(street: String) extends ObjectMap[Address] {
		override implicit val schema: PropsSchema[Address] = null
	}


	object Monkey extends PropsSchema[Monkey] {
		object NAME extends StrProp[Monkey]("name")
		object AGE extends LngProp[Monkey]("age")
		object HEIGHT extends DblProp[Monkey]("height")
		object ADDR extends ObjProp[Monkey, Address]("address")(classOf[Address])
	}

	case class Monkey(name: String, var age: Int) extends ObjectMap[Monkey] {
		override implicit val schema: PropsSchema[Monkey] = Monkey
	}

	val m1 = Monkey("Mooky", 18)
	val m2 = Monkey("Mooky2", 14)


	/**
	m1(Monkey.NAME) = "Mooky"
	m1(Monkey.AGE) = 12L
	m1(Monkey.HEIGHT) = 120.8
	m1(Monkey.ADDR) = Address("NY")
	  */
	val vals = Map("name" -> 10000, "age" -> 12, "height" -> "a-123,98iuy87", "address" -> Address("NY"))
	m1.values(vals)
	m2.values(Map("name" -> 20000, "age" -> 9, "height" -> "a- 123,98iuy87", "address" -> Address("CH")))

	{
		import Monkey._
		val n: String = m1(NAME)
		println(n)
		val a: Long = m1(AGE)
		println(a)
		val h: Double = m1(HEIGHT)
		println(h)
		val adr: Address = m1(ADDR)
		println(adr)
	}
	val t0 = System.currentTimeMillis()
	println(m1.values)
	println("Time= "+(System.currentTimeMillis() - t0))
	val t1 = System.currentTimeMillis()
	println(m1.values)
	println("Time= "+(System.currentTimeMillis() - t1))
	val t2 = System.currentTimeMillis()
	println(m2.values)
	println("Time= "+(System.currentTimeMillis() - t2))
	val t3 = System.currentTimeMillis()
	println(m2.values)
	println("Time= "+(System.currentTimeMillis() - t3))

	object Doggy extends PropsSchema[Doggy] {
		object _NAME extends StrProp[Doggy]("name")
		object _AGE extends LngProp[Doggy]("age")
		object _HEIGHT extends DblProp[Doggy]("height")
		object _ADDR extends ObjProp[Doggy, Address]("address")(classOf[Address])
	}

	class Doggy extends ObjectMap[Doggy] {
		override val schema: PropsSchema[Doggy] = Doggy
	}

	val d1 = new Doggy

	/**
	d1(Doggy.NAME) = "Gavki"
	d1(Doggy.AGE) = 11L
	d1(Doggy.HEIGHT) = 77.6
	d1(Doggy.ADDR) = Address("LA")
	println(Doggy.props)
	  */

	d1.values(Map("name" -> 10001, "age" -> "wer- 11,8", "height" -> "-.", "address" -> Address("LA")))

	{
		import Doggy._
		val n: String = d1(_NAME)
		println(n)
		val a: Long = d1(_AGE)
		println(a)
		val h: Double = d1(_HEIGHT)
		println(h)
		val adr: Address = d1(_ADDR)
		println(adr)
	}
	println {
		d1.values.collect { case (key, v) =>
			Doggy.props.find(_.name == key) match {
				case Some(prop) => key -> prop.outType.cast(v)
				case _ => key -> v
			}
		}
	}


}
