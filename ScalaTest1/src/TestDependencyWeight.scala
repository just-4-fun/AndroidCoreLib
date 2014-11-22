

object TestDependencyWeight {
	val a = AppService("A")
	val b = AppService("B")
	val c = AppService("C")
	val d = AppService("D")
	val e = AppService("E")
	val list = a :: b :: c :: d :: e :: Nil

	e.dependsOn(a)
	d.dependsOn(c, b, e)
	c.dependsOn(b)
	b.dependsOn(a, e)
	//	a.dependsOn(d)

	println(">> "+toString())

	override def toString(): String = {
		list.mkString(", ")
	}

	case class AppService(name: String) {
		var weight = 0
		def dependsOn(services: AppService*): Unit = {
			services.foreach(Dependencies.add(_, this))
		}
		override def toString(): String = s"[$name : $weight]"
	}

	object Dependencies extends collection.mutable.HashSet[(AppService, AppService)] {
		def add(parent: AppService, child: AppService) = {
			//
			// DEFs
			def assign(p: AppService, c: AppService, recalc: Boolean = true) = {
				if (p == child) throw new Exception(s"${p.name } > ${child.name }")
				if (p.weight <= c.weight) {
					p.weight = c.weight + 1
					if (recalc) {
						println(s"recalc > ${p.name} : ${c.name}")
						recalcParent(p)
					}
					else println(s"skip 0 > ${p.name} : ${c.name}")
				}else println(s"skip 1 > ${p.name} : ${c.name}")
			}
			def recalcParent(_p: AppService): Unit = foreach { case (p, c) =>
				if (c == _p) assign(p, c)
			}
			//
			//EXEC
			if (child.weight == 0) child.weight = 1
			assign(parent, child, parent.weight > 0)
			+=(parent -> child)
		}
	}


}
