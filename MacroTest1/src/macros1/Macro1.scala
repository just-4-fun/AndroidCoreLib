package macros1

import scala.language.experimental.macros

object Macro1 extends App {

//	def getType[T, U <: Obj[_]](col: Column[T, U]): String = macro getClass_impl[T, U]
////	def getType[T, U <: Obj[_]](col: Column[T, U]): String = ???
//
//	def getClass_impl[T: c.WeakTypeTag, U  <: Obj[_]: c.WeakTypeTag](c: Context)(col: c.Expr[Column[T, U]]): c.Expr[String] = {
//		import c.universe._
//		val utp = weakTypeTag[U]
//		val t: Type = implicitly[WeakTypeTag[T]].tpe
//		val ttp = weakTypeOf[T]
//		val str = t.typeSymbol.name match {
//			case x if x == typeOf[String] => c.Expr[String](reify[String]("String").tree)
//			case x if x == typeOf[Int] => c.Expr[String](reify[String]("Int").tree)
//			case x if x == typeOf[Double] => c.Expr[String](reify[String]("Double").tree)
//			case _ => c.Expr[String](reify[String]("Other2").tree)
//		}
//		println("UTP= "+utp)
//		println("TTP= "+ttp)
//		println("TTP= "+ttp.erasure)
//		println("TTP= "+ttp.termSymbol)
//		println("TTP= "+ttp.typeSymbol)
//		println("TTP= "+ttp.typeArgs)
//		println("TTP= "+ttp.dealias.typeArgs)
//		println("Tpe= "+t)
//		println("RES2= "+t.termSymbol)
//		println("RES3= "+t.typeSymbol)
//		println("RES4= "+str)
//		str
	}



//	def getClass_impl[T: c.WeakTypeTag](c: Context): c.Expr[String] = {
//		import c.universe._
//		val t: Type = implicitly[WeakTypeTag[T]].tpe
//		val tp = weakTypeOf[T]
//		val str = t.typeSymbol.name match {
//			case x if x == typeOf[String] => c.Expr[String](reify[String]("String").tree)
//			case x if x == typeOf[Int] => c.Expr[String](reify[String]("Int").tree)
//			case x if x == typeOf[Double] => c.Expr[String](reify[String]("Double").tree)
//			case _ => c.Expr[String](reify[String]("Other2").tree)
//		}
//		println("TP= "+tp)
//		println("Tpe= "+t)
//		println("RES2= "+t.termSymbol)
//		println("RES3= "+t.typeSymbol)
//		println("RES4= "+str)
//		str
//	}
	//	def getType[T]: String = macro getClass_impl[T]
	//	def getClass_impl[T: c.TypeTag](c: Context): c.Expr[String] = {
	//		import c.universe._
	//		val t: Type = implicitly[TypeTag[T]].tpe
	//		t == typeOf[String]
	//		q"classOf[T]"
	//	}
//}
