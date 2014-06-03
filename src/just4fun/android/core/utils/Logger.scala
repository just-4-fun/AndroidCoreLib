package just4fun.android.core.utils

import just4fun.android.core.BuildConfig
import android.util.Log

object Logger {

	val tagRoot = "just4fun"
	val debug = BuildConfig.DEBUG

	trait Loggable {
		implicit lazy val TAG = LogTag(this)
	}
	// TODO change to more universal
	case class LogTag(thisOrName: AnyRef) {
		val name = thisOrName match {
			case s: String => s
			case _ => thisOrName.getClass.getSimpleName
		}
	}


	def logv(method: String, msgs: => String = null)(implicit tag: LogTag) {
		if (debug) Log.v(tagRoot + ":" + tag.name, "[" + method + "]" + (if (msgs == null) "" else " :: " + msgs))
	}
	def logd(method: String, msgs: => String = null)(implicit tag: LogTag) {
		if (debug) Log.d(tagRoot + ":" + tag.name, "[" + method + "] :: " + (if (msgs == null) "" else " :: " + msgs))
	}
	def logi(method: String, msgs: => String = null)(implicit tag: LogTag) {
		if (debug) Log.i(tagRoot + ":" + tag.name, "[" + method + "] :: " + (if (msgs == null) "" else " :: " + msgs))
	}
	def logw(method: String, msgs: => String = null)(implicit tag: LogTag) {
		if (debug) Log.w(tagRoot + ":" + tag.name, "[" + method + "] :: " + (if (msgs == null) "" else " :: " + msgs))
	}
	def loge(msgs: => String)(implicit tag: LogTag) {
		if (debug) Log.e(s"$tagRoot:${tag.name}", msgs)
	}
	def loge(ex: => Throwable, msgs: => String = null)(implicit tag: LogTag) {
		if (debug) Log.e(tagRoot + ":" + tag.name, (if (msgs == null) "" else msgs + "\n") + Log.getStackTraceString(ex))
	}


}
