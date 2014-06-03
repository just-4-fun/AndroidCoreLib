
object ScalaTest extends App {


	test.Test

/**
	trait Pty {val typ: String; val name: String; def cast(): String}
	trait C1 extends Pty{val c1v1: Int; def cast() = "C1"}
	trait C2 extends Pty{val c2v1: Int; def cast() = "C2"}
	trait C3 extends Pty{val c3v1: Int; def cast() = "C3"}

	class C4(val name: String, val typ: String, val c1v1: Int, val c2v1: Int, val c3v1: Int) extends C1 with C2 with C3 {
		override def cast() = "C4"
	}

	object col1 extends C4("col1", "Int", 1, 2, 3)
	object col2 extends C4("col2", "Long", 2, 3, 4)

	println(col1.c3v1)

	val col3 = new C4("col3","Double", 3, 4, 5)
	val col: C2 = col3
	println(col.name+" "+col.c2v1+" "+col.cast())
*/

	/**
	val t0 = System.currentTimeMillis()
	for(n <- 0 to 1000000) t0 == 10000000
//	for(n <- 0 to 1000000) t0.getClass == classOf[Long]
//	for(n <- 0 to 1000000) t0.isInstanceOf[Long]
	println(System.currentTimeMillis() - t0)
	  */


	/** DBOBJECT
	import dbtest._
	
	class Monkey extends DbObject [Monkey]
	object MonkeyTab extends DbTable[Monkey] {
		val name = ColumnStr[Monkey]("name")
		val age = ColumnInt[Monkey]("age")
	}

	class Doggy extends DbObject [Doggy]
	object DoggyTab extends DbTable[Doggy] {
		val name = ColumnStr[Doggy]("name")
		val age = ColumnInt[Doggy]("age")
	}

	class Faky extends DbObject[Faky]
	object FakyTab extends DbTable[Doggy] {
		val name = ColumnStr[Faky]("name")
		val age = ColumnInt[Faky]("age")
	}

	val faky1 = new Faky
	faky1(FakyTab.name) = "Flaky"
	faky1(FakyTab.age) = 13
	var name: String = faky1(FakyTab.name)
	var age: Int = faky1(FakyTab.age)
	println(name)
	println(age)


	val monkey1 = new Monkey
	monkey1(MonkeyTab.name) = "Monboo"
	monkey1(MonkeyTab.age) = 12
	name = monkey1(MonkeyTab.name)
	age = monkey1(MonkeyTab.age)
	println(name)
	println(age)

	val doggy1 = new Doggy
	doggy1(DoggyTab.name) = "Dogster"
	doggy1(DoggyTab.age) = 11
	name = doggy1(DoggyTab.name)
	age = doggy1(DoggyTab.age)
	println(name)
	println(age)
	  */


	/*  VAR TEST */
	/**
	implicit def var2value[T](v: Var[T]): T = v.get
	implicit def varOrdering[T](implicit ev: Ordering[T]): Ordering[Var[T]] = Ordering.by(v => v.value)

	abstract class Var[T] /*extends Ordered[Var[T]] */{
		var value: T
		override def equals(that: Any): Boolean = that match {
			case v: T => v == value
			case _ => super.equals(that)
		}
		def get: T = value
//		def compare(that: Var[T]): Int = Ordering.by()
	}
	case class IntVar(var value: Int) extends Var[Int] {
//		def compare(that: Int): Int = this.value compare that
	}
	case class FloatVar(var value: Float) extends Var[Float] {
//		def compare(that: Float): Int = this.value compare that
	}
	case class StringVar(var value: String) extends Var[String] {
//		def compare(that: String): Int = this.value compare that
	}

	println(21 > IntVar(22))
	println(22 >= IntVar(22))
	println(23 > IntVar(22))
	println(IntVar(22) > 21.0)
	println(FloatVar(22) > 23)
	println(FloatVar(22) >= 22)
	println(FloatVar(22) < 22)
	println(FloatVar(22) < 2)
	println(FloatVar(22) < 32)
	println(FloatVar(22) < FloatVar(32))
	println(FloatVar(22) > FloatVar(32))
	println(FloatVar(22) <= FloatVar(22))
//	println(IntVar(22) == 22)
//	println(IntVar(22) == 22.0)
//	println(IntVar(22) == IntVar(22))
//	println(FloatVar(22.0f) == 22.0)
//	println(FloatVar(22.0f) == 22)
//	println(FloatVar(22.0f) == FloatVar(22))
//	println(StringVar("22") == "22")
//	println(StringVar("22") == StringVar("22"))
//	println(IntVar(22) == FloatVar(22))
	  */

	/*MACROS*/
	//	HelloWorld1Macro.helloWorld()


	/* TRY    CATCH    FINALLY */
	/**
	// no exception
	//start 1 3 return:1 end
	// throw exception
	//start 1 2 3 return:2 end
	def test(): String = {
		try {
			println(1)
			throw new Exception
			"1"
		}
		catch {
			case e: Throwable => println(2); "2"
		}
		finally {
			println(3)
			"3"
		}
	}

	println("start")
	println("return:"+test())
	println("end")
	  */





	/*REFLECTION*/
	/**
	import scala.reflect.runtime.universe._
	class B[T: TypeTag] {
		val tpe = typeOf[T]
	}
	println("true vs " + (new B[String].tpe == typeOf[String])) // true
	println("false vs " + (new B[String].tpe == typeOf[Int])) // false
	  */



	/* LINEARIZATION*/
	/**
	trait Root {
		def run() {fun()}
		def fun(): Unit//= print("Root") //WARN: if that def is not abstract the [abstract override def] is not required
	}
	trait R1 extends Root {
		override def fun(): Unit = print("R1 : ")
	}
	trait R2 extends Root {
		override def fun(): Unit = print("R2 : ")
	}
	trait S1 extends Root {
		abstract override def fun(): Unit = { print("S1 : "); super.fun() }
	}
	trait Sp2 extends Root {
		abstract override def fun(): Unit = { print("SP2 : "); super.fun() }
	}
	trait S2 extends Sp2 {
		abstract override def fun(): Unit = { print("S2 : "); super.fun() }
	}

	class Bottom  {this: Root =>
		def apply() { fun(); println() }
	}

//	(new Bottom )()//Bottom :
//	(new Bottom with S1)()//S1 : Bottom : Root
	(new Bottom with R1)()//R1 :
	(new Bottom with R1 with R2)()//R2 :
	(new Bottom with R1 with R2 with S1)()//S1 : R2 :
	(new Bottom with R1 with R2 with S2 with S1)()//S1 : S2 : SP2 : R2 :
	(new Bottom with R1 with R2 with S1 with S2)()//S2 : SP2 : S1 : R2
	(new Bottom with R1 with R2 with S1 with Sp2 with S2)()//S2 : SP2 : S1 : R2
	(new Bottom with R1 with R2 with S1 with S2 with Sp2)()//S2 : SP2 : S1 : R2
	(new Bottom with R1 with S1 with S2 with R2)()//R2 :

	object Bottom2 extends S1 with R1 {
		def apply() { println(); run();  }
//		abstract override def fun(): Unit = { print("Bottom2 : ");super.fun()  }
	}
	Bottom2()//Bottom2 : S2 : SP2 : Root
	  */




	/**
	import scala.concurrent._
	import scala.concurrent.duration._
	import scala.concurrent.impl._
	val pr = promise //new Promise.DefaultPromise[T]()

//	new android.os.AsyncTask(){
//		override def doInBackground(params: Nothing*): Unit = println("doInBackground")
//	}.execute()

	implicit object ExCxt extends ExecutionContext {
		var itr= 0
		override def reportFailure(t: Throwable): Unit = {println("Ooops... "+t.getMessage)}
		override def execute(runnable: Runnable): Unit = {
			itr+=1
			new Thread(runnable, s"custom THR $itr").start()
		}
	}
	object ExCxtMain extends ExecutionContext{
		override def reportFailure(t: Throwable): Unit = {println("Main Ooops... "+t.getMessage)}
		override def execute(runnable: Runnable): Unit = {
			runnable.run()
		}
	}
	val futr: Future[Int] = future{
		println("Run:  "+Thread.currentThread().getName)
		420
	}
	futr.onSuccess{case v => println(s"onSUCcESS: $v  :: "+Thread.currentThread().getName); 1/0}(ExCxt)
	futr.onFailure{case ex => println(s"onFailure: $ex  :: "+Thread.currentThread().getName)}(ExCxt)
	Await.result(futr, 0 nanos)
	  */



	/**
	println("["+" "*20+"]")
	  */


	/**
	class Wrapper[A](v: A) {
		def in(compVal: A*): Boolean = {println(compVal); compVal.contains(v)}
	}
	implicit def convert[A](v: A) = new Wrapper[A](v)
	val v1 = 1
	println(v1.in(2, 5, 8, 10, 0))
	Seq(2, 5, 9, 1, 55).contains(1)
	  */


	/**
	case class A(n: String)
	lazy protected val id2serviceMap = collection.mutable.WeakHashMap[A,String]((A("a"), "1"), (A("B"), "2"), (A("c"), "3"))

	def findService(id: String): Option[A] = {
		val r: Option[(A, String)] = id2serviceMap.find{case (s, sid) => sid == id}
		val rr:(A, String) = r.getOrElse((null, ""))
		Option(rr._1)
	}

	println(findService("3"))
	  */



	/*DEPENDANCY INJECTION EXAMPLE */
	/**
	trait DbAbstract {def use(): String}

	trait Db extends DbAbstract {def use() = "DB USED"}

	trait DbUser {
		this: DbAbstract =>
		def callDb = {println("callingDB by use > "+use())}
	}

	val dbUser = new DbUser with Db
	dbUser.callDb
	  */

	/*DEPENDANCY INJECTION EXAMPLE */
	/**
	// service interfaces
	trait OnOffDeviceComponent {
		val onOff: OnOffDevice
		trait OnOffDevice {
			def on: Unit
			def off: Unit
		}
	}
	trait SensorDeviceComponent {
		val sensor: SensorDevice
		trait SensorDevice {
			def isCoffeePresent: Boolean
		}
	}

	// =======================
	// service implementations
	trait OnOffDeviceComponentImpl extends OnOffDeviceComponent {
		class Heater extends OnOffDevice {
			def on = println("heater.on")
			def off = println("heater.off")
		}
	}
	trait SensorDeviceComponentImpl extends SensorDeviceComponent {
		class PotSensor extends SensorDevice {
			def isCoffeePresent = true
		}
	}
	// =======================
	// service declaring two dependencies that it wants injected
	trait WarmerComponentImpl {
		this: SensorDeviceComponent with OnOffDeviceComponent =>
		class Warmer {
			def trigger = {
				if (sensor.isCoffeePresent) onOff.on
				else onOff.off
			}
		}
	}
	// =======================
	// instantiate the services in a module
	object ComponentRegistry extends
	OnOffDeviceComponentImpl with
	SensorDeviceComponentImpl with
	WarmerComponentImpl {

		val onOff = new Heater
		val sensor = new PotSensor
		val warmer = new Warmer
	}
	// =======================
	val warmer = ComponentRegistry.warmer
	warmer.trigger
	  */










	/** // TEST modifiers
		class X {
			private def fun() {println("X")}
		}

		class Y extends X {
			def fun() {super.fun(); println("Y")}
		}

		val y = new Y
		y.fun
	  */


	/**
	def exec(func: () => Unit): Unit = {
		new Thread(new Runnable {
			override def run(): Unit = {
				println("<")
				func()
				println(">")
			}
		}).start()
	}
	def execCode(code: => Unit): Unit = {
		new Thread(new Runnable {
			override def run(): Unit = {
				println("<")
				code
				println(">")
			}
		}).start()
	}
	def func(str: String): Unit = {
		println("inside func: "+str)
	}

	execCode {
		func("text")
	}
	println("***")

	//	exec(func)
	Thread.sleep(1000)
	  */

	/**
	val itr = Iterator(1, 2, 3)
		val A, B, C = itr.next()
		println(s1"$A  $B  $C")

		val next: () => Int = {
			var n = 0
			() => {n = n + 1; n}
		}
		val D, E, F, G, H = next()
		println(s1"$D  $E  $F")
	  */

}