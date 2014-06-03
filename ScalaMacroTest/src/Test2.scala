

/*
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object HelloWorld1Macro {
	def helloWorld() = macro helloWorld_impl

	def helloWorld_impl(c: Context)(): c.Expr[Unit] = {
		import c.universe._

		reify {
			println("Hello world")
		}
	}
}
*/
