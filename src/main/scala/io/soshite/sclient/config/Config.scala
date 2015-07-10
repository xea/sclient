package io.soshite.sclient.config

import java.net.InetAddress

import org.rogach.scallop.ScallopConf

/**
 *
 */
class Config(args: Array[String]) extends ScallopConf(args) {

	val from = opt[String]("from",        short = 'f', default = Some("sender@test.com"))
	val to = opt[String]("to",            short = 't', default = Some("recipient@test.com"))
	val cc = opt[String]("cc",            short = 'c', default = None)
	val bcc = opt[String]("bcc",          short = 'b', default = None)
	val eml = opt[String]("eml",          short = 'e', default = None)
	val server = opt[String]("server",    short = 's', default = Some("127.0.0.1"))
	val port = opt[Int]("port",           short = 'p', default = Some(25))
	val user = opt[String]("user",				short = 'u', default = None)
	val password = opt[String]("password",short = 'P', default = None)
	val ehloname = opt[String]("ehloname",short = 'E', default = Some(InetAddress.getLocalHost.getHostName))
	val verbose = tally("verbose",        short = 'v')
	val crlf = opt[Boolean]("crlf", noshort = true, default = Some(false))
	val starttls = opt[Boolean]("starttls", noshort = true, default = Some(false))

}
