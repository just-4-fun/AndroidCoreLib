package just4fun.android.demo1

import android.app.Activity
import project.config.logging.Logger._
import android.os.Bundle
import android.widget.Button
import android.view.View
import android.view.View.OnClickListener
import android.content.Intent

object Implicits {
	implicit def onClick(code: => Unit): View.OnClickListener = new OnClickListener {
		override def onClick(v: View): Unit = code
	}
}

class MyActivity extends Activity with Loggable {
	import Implicits._

	override def onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.main)
		//
		val b = findViewById(R.id.button).asInstanceOf[Button]
		b.setOnClickListener {
			val intent = new Intent(this, classOf[OtherActivity])
			startActivity(intent)
		}
	}
}





class OtherActivity extends Activity {
	import Implicits._

	override def onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.main)
		//
		val b = findViewById(R.id.button).asInstanceOf[Button]
		b.setOnClickListener {
			val intent = new Intent(this, classOf[MyActivity])
			startActivity(intent)
		}
	}
}