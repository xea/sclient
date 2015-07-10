package io.soshite.sclient.log

/**
 *
 */
object Logger {

	val NONE = -1
	val ERROR = 0
	val VERBOSE = 1
	val PROTOCOL = 2
	val DEBUG = 3
	val ALL   = 99

	var loglevel:Int = ALL

	def error(msg: String):Unit = {
		if (loglevel > ERROR) {
			println(s"ERROR: $msg")
		}
	}

	def verbose(msg: String):Unit = {
		if (loglevel > VERBOSE) {
			println(s"$msg")
		}
	}

	def protocol(msg: String):Unit = {
		if (loglevel > PROTOCOL) {
			println(s"$msg")
		}
	}

	def debug(msg: String):Unit = {
		if (loglevel > DEBUG ) {
			println(s"$msg")
		}
	}
}
