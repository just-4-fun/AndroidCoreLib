package just4fun.android.core

import java.io.{ByteArrayOutputStream, BufferedInputStream, InputStreamReader, BufferedReader, InputStream, IOException}
import scala.util.Try
import just4fun.android.core.utils.TryNClose

package object inet {


	/* MISC */
	val UTF8 = "UTF-8"

	object Method extends Enumeration {val GET, POST, PUT, DELETE = Value}
	//
	object ContentType {
		val Form = "application/x-www-form-urlencoded"
		val Json = "application/json; charset=utf-8"
	}
	//
	object HttpCode {
		val OFFLINE = 1
		val CANCELED = 2
	}


	/* EXCEPTIONS */
	case class InetRequestException(code: Int) extends Exception

	object InetRequestCancelled extends InetRequestException(HttpCode.CANCELED)

	object OfflineException extends InetRequestException(HttpCode.OFFLINE)


	/* ERROR HANDLER   */
	trait ErrorHandler {
		def handleErrorForRetry(httpCode: Int, httpMessage: String, exception: Throwable, opts: InetOptions): Option[Boolean]
	}


	/* INET AUTHENTICATOR   */
	trait InetAuthenticator {
		val retryCode: Int = 401
		var scope: String = _
		def getToken: String
		def requestToken: String
		def onPrepareRequest(opts: InetOptions)
		def checkRetry(httpCode: Int): Option[Boolean] = {
			if (httpCode != retryCode) None
			else Some(requestToken != null)
		}
	}


	/*   INET LISTENER */
	trait InetStateListener {
		def onlineStatusChanged(isOnline: Boolean, byUser: Boolean)
	}



	/* STREAM CONSUMER */
	trait StreamToResult[T] {
		def consume(in: InputStream): Try[T]
	}

	class StreamToString extends StreamToResult[String] {
		def consume(in: InputStream): Try[String] = {
			TryNClose(new BufferedReader(new InputStreamReader(in, UTF8))) { bufRd =>
				val res = new StringBuilder
				Stream.continually(bufRd.readLine()).takeWhile(_ != null).foreach(res.append)
				//			var line = bufRd.readLine()
				//			while (line != null) {res.append(line); line = bufRd.readLine() }
				res.toString()
			}
		}
	}

	class StreamToBytes extends StreamToResult[Array[Byte]] {
		def consume(in: InputStream): Try[Array[Byte]] = {
			TryNClose(new BufferedInputStream(in)) { bufIn =>
				TryNClose(new ByteArrayOutputStream(1024)) { out =>
					val buf: Array[Byte] = new Array[Byte](1024)
					Stream.continually(bufIn.read(buf)).takeWhile(_ != -1).foreach(out.write(buf, 0, _))
					//				var count = bufIn.read(buf)
					//				while (count != -1) {out.write(buf, 0, count); count = bufIn.read(buf) }
					out.toByteArray
				}
			}.flatten
		}
	}


}
