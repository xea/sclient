package io.soshite.sclient.protocol

sealed trait EhloFeatures {

	def features: String

	def serverFeatures: Array[String] = features.split("\r\n").map(x => { x.split("^[0-9]+[- ]")(1) })
}

abstract class SMTPMessage {}
abstract class SMTPServerMessage extends SMTPMessage {}
abstract class SMTPClientMessage extends SMTPMessage {}

case class SMTPServerGreet(value: String) extends SMTPServerMessage
case object SMTPServerUnavailable extends SMTPServerMessage
case class SMTPClientEhlo(name: String) extends SMTPClientMessage
case class SMTPServerEhlo(features: String) extends SMTPServerMessage with EhloFeatures

case class SMTPClientMail(from: String) extends SMTPClientMessage
case object SMTPServerMailOK extends SMTPServerMessage

case class SMTPClientRecipient(to: String) extends SMTPClientMessage
case object SMTPServerRecipientOK extends SMTPServerMessage

case class SMTPClientCarbonCopy(to: String) extends SMTPClientMessage
case object SMTPServerCarbonCopyOK extends SMTPServerMessage

case class SMTPClientBlindCarbonCopy(to: String) extends SMTPClientMessage
case object SMTPServerBlindCarbonCopyOK extends SMTPServerMessage

case object SMTPClientData extends SMTPClientMessage
case object SMTPServerDataOK extends SMTPServerMessage

case class SMTPClientDataBody(body: String) extends SMTPClientMessage
case object SMTPServerDataBodyOK extends SMTPServerMessage
case class SMTPServerDataBodyError(error: String) extends SMTPServerMessage

case object SMTPClientQuit extends SMTPClientMessage
case object SMTPServerQuit extends SMTPServerMessage

case class SMTPServerError(code: String, error: String) extends SMTPServerMessage
case object SMTPServerSyntaxError extends SMTPServerMessage
case object SMTPUnknownMessage extends SMTPMessage
case class SMTPLocalError(error: String) extends SMTPServerMessage

case object SMTPClientStartTLSRequest extends SMTPClientMessage
case object SMTPClientStartTLS extends SMTPClientMessage
case object SMTPServerStartTLSOK extends SMTPServerMessage

case class SMTPClientAuthRequest(authType: String) extends SMTPClientMessage
case class SMTPServerAuthError(error: String) extends SMTPServerMessage
case class SMTPServerAuthChallenge(challenge: String) extends SMTPServerMessage
case class SMTPClientAuthResponse(response: String) extends SMTPClientMessage
case object SMTPServerAuthOK extends SMTPServerMessage

object SMTPPattern {

	val serverGreet = "(?s)^220[- ]([^;]+).*$".r
	val serverUnavailable = "(?s)^421 .*$".r
	val serverEhlo = "(?s)^(250-.*250 .*)$".r
	val serverDataOk = "(?s)^354 (.*)$".r
	val serverStarttlsOk = "(?s)^220 (.*)$".r
	val serverLocalError = "(?s)^451 (.*)$".r
	val serverSyntaxError = "(?s)^(500|501) .*$".r
	val serverQuit = "(?s)221 .*$".r
	val serverOk = "(?s)^250 (.*)$".r
	val serverError = "(?s)^([0-9]+) (.*)$".r
  val serverAuthChallenge = "(?s)^334[ ]?(.*)$".r
  val serverAuthOk = "(?s)^235[ ]?(.*)$".r
  val authTypes = "(?s).*AUTH[ =](.*)$".r
}