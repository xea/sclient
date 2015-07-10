package io.soshite.sclient

import java.io._
import java.net.{InetSocketAddress, Socket}
import java.security.SecureRandom
import java.util.Base64
import javax.net.ssl.{TrustManager, SSLContext, SSLSocket}
import io.soshite.sclient.config.Config
import io.soshite.sclient.generator.{RandomContentGenerator, EMLContentGenerator, ContentGenerator}
import io.soshite.sclient.log.Logger
import io.soshite.sclient.protocol._
import io.soshite.sclient.tls.NaiveTrustManager

import scala.annotation.tailrec

/**
 *
 */
class Client(config: Config) {

	var reader: BufferedReader = null
	val generator = initGenerator(config)
	val socket = new Socket()
	var secureSocket: SSLSocket = null
	var currentSocket: Socket = null

  def initGenerator(config: Config): ContentGenerator = {
    if (config.eml.isEmpty) {
      new RandomContentGenerator()
    } else {
      new EMLContentGenerator(config)
    }
  }

	def process() = {
		connect

		loop(readMessage(null))

		disconnect
	}

	@tailrec
	final def loop(msg: SMTPMessage):SMTPMessage = {
		val continue = msg match {
			case SMTPServerQuit => false
			case SMTPServerError(code, message) => { Logger.error(s"Error code received: $code $message"); false }
			case x:SMTPServerMessage => true
			case SMTPUnknownMessage => { Logger.error("Unknown message received, aborting loop"); false }
		}

		if (continue) loop(readMessage(sendMessage(strategy(msg)))) else null
	}

	def strategy(msg: SMTPMessage):SMTPClientMessage = {
		msg match {
			case SMTPServerEhlo(f) => {
				if (config.user.isSupplied) {
          trySmtpAuth(f)
				} else {
          tryStartTls
        }
			}
      case SMTPServerAuthOK => tryStartTls
      case SMTPServerAuthChallenge(challenge) => trySmtpAuthSecondPass
			case SMTPServerStartTLSOK => SMTPClientStartTLS
			case SMTPServerMailOK => SMTPClientRecipient("<" + config.to() + ">")
			case SMTPServerRecipientOK => {
				if (!config.cc.isEmpty) {
					SMTPClientCarbonCopy("<" + config.cc() + ">")
				} else if (!config.bcc.isEmpty) {
					SMTPClientBlindCarbonCopy("<" + config.bcc() + ">")
				} else {
					SMTPClientData
				}
			}
			case SMTPServerCarbonCopyOK => {
				if (!config.bcc.isEmpty) {
					SMTPClientBlindCarbonCopy("<" + config.bcc() + ">")
				} else {
					SMTPClientData
				}
			}
			case SMTPServerDataOK => SMTPClientDataBody(generator.generate(config.eml))
			case SMTPServerDataBodyError(error) => SMTPClientQuit
			case SMTPServerDataBodyOK => SMTPClientQuit
			case _ => SMTPClientEhlo(config.ehloname())
		}
	}

  protected def trySmtpAuth(features: String) = {
    val authtypes = features.toUpperCase match {
      case SMTPPattern.authTypes(types) => types.split("\r\n");
      case _ => Array[String]()
    }

    val username = if (config.user.isSupplied) config.user() else ""

    if (authtypes.contains("LOGIN")) {
      val authdata = Base64.getEncoder.encodeToString(s"$username".getBytes)
      SMTPClientAuthRequest(s"LOGIN $authdata")
    } else {
      Logger.error("Couldn't find a suitable authentication method, quitting.")
      SMTPClientQuit
    }
  }

  protected def trySmtpAuthSecondPass = {
    val password = if (config.password.isSupplied) config.password() else { print("Password: "); System.console().readPassword() }
    val authdata = Base64.getEncoder.encodeToString(s"$password".getBytes)

    SMTPClientAuthResponse(authdata)
  }

  protected def tryStartTls = {
    if (config.starttls() && secureSocket == null) {
      SMTPClientStartTLSRequest
    } else {
      SMTPClientMail("<" + config.from() + ">")
    }
  }

	protected def connect = {
		socket.connect(new InetSocketAddress(config.server(), config.port()))
		currentSocket = socket
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream))
		val configStr = config.server() + ":" + config.port()
		Logger.verbose(s"Connected to $configStr")
	}

	protected def disconnect = {
		reader.close()
		socket.close()
		Logger.verbose(s"Disconnected")
	}

	protected def readLine():String = {
		//new String(Stream.continually(socket.getInputStream.read).takeWhile(x => x >= 0 && x != 10).map(_.toByte).toArray :+ a)

		val line = reader.readLine()
		Logger.protocol(s"< $line")
		line
	}

	protected def readMessage(msg: SMTPMessage) = {
    val sb = new StringBuilder
    var line = ""

    var run = true

    while (run) {
      line = readLine()
      sb.append(line)
      sb.append("\r\n")

      run = !line.matches("^[0-9]+ .*$")
    }

    parseMessage(msg, sb.toString())
	}

	protected def sendMessage(message: SMTPMessage):SMTPMessage = {
		message match {
			case SMTPClientEhlo(name) => writeLine(s"EHLO $name")
			case SMTPClientMail(from) => writeLine(s"MAIL FROM: $from")
			case SMTPClientRecipient(to) => writeLine(s"RCPT TO: $to")
			case SMTPClientCarbonCopy(to) => writeLine(s"RCPT TO: $to")
			case SMTPClientBlindCarbonCopy(to) => writeLine(s"RCPT TO: $to")
			case SMTPClientData => writeLine("DATA")
			case SMTPClientDataBody(body) => writeLine(body + "\r\n.")
			case SMTPClientQuit => writeLine("QUIT")
			case SMTPClientStartTLSRequest => writeLine("STARTTLS")
			case SMTPClientStartTLS => {
				val sc = SSLContext.getInstance("TLS")

				sc.init(null, Array[TrustManager](new NaiveTrustManager), new SecureRandom())

				val sf = sc.getSocketFactory
				secureSocket = sf.createSocket(socket, socket.getInetAddress.getHostAddress(), socket.getPort(), true).asInstanceOf[SSLSocket]

//				secureSocket.setUseClientMode(true)
				reader = new BufferedReader(new InputStreamReader(secureSocket.getInputStream))
				currentSocket = secureSocket
				sendMessage(SMTPClientEhlo(config.ehloname()))
			}
      case SMTPClientAuthRequest(authType) => writeLine(s"AUTH $authType")
      case SMTPClientAuthResponse(response) => writeLine(response)
  		case _ => {}
		}

		message
	}

	protected def writeLine(line: String) = {
		val closedLine = line + "\r\n"
		Logger.protocol("> " + closedLine.trim)
		currentSocket.getOutputStream().write(closedLine.getBytes())
	}

	protected def parseMessage(clientMessage: SMTPMessage, input: String): SMTPMessage = {
		Logger.debug("Parsing line: " + input.trim)

		clientMessage match {
			case SMTPClientEhlo(name) => {
				input match {
					case SMTPPattern.serverEhlo(features) => SMTPServerEhlo(features)
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
			case SMTPClientStartTLSRequest => {
				input match {
					case SMTPPattern.serverStarttlsOk(status) => SMTPServerStartTLSOK
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
			case SMTPClientStartTLS => {
				input match {
					case SMTPPattern.serverEhlo(features) => SMTPServerEhlo(features)
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
      case SMTPClientAuthRequest(request) => {
        input match {
          case SMTPPattern.serverAuthChallenge(challenge) => SMTPServerAuthChallenge(challenge)
          case SMTPPattern.serverAuthOk(status) => SMTPServerAuthOK
          case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
          case _ => SMTPUnknownMessage
        }
      }
      case SMTPClientAuthResponse(response) => {
        input match {
          case SMTPPattern.serverAuthOk(status) => SMTPServerAuthOK
          case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
          case _ => SMTPUnknownMessage
        }
      }
			case SMTPClientMail(from) => {
				input match {
					case SMTPPattern.serverOk(status) => SMTPServerMailOK
					case SMTPPattern.serverLocalError(error) => SMTPLocalError(error)
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
			case SMTPClientRecipient(to) => {
				input match {
					case SMTPPattern.serverOk(status) => SMTPServerRecipientOK
					case SMTPPattern.serverLocalError(error) => SMTPLocalError(error)
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
			case SMTPClientCarbonCopy(to) => {
				input match {
					case SMTPPattern.serverOk(status) => SMTPServerCarbonCopyOK
					case SMTPPattern.serverLocalError(error) => SMTPLocalError(error)
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
			case SMTPClientBlindCarbonCopy(to) => {
				input match {
					case SMTPPattern.serverOk(status) => SMTPServerBlindCarbonCopyOK
					case SMTPPattern.serverLocalError(error) => SMTPLocalError(error)
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
			case SMTPClientData => {
				input match {
					case SMTPPattern.serverDataOk(status) => SMTPServerDataOK
					case SMTPPattern.serverLocalError(error) => SMTPServerDataBodyError(error)
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
			case SMTPClientDataBody(body) => {
				input match {
					case SMTPPattern.serverOk(status) => SMTPServerDataBodyOK
					case SMTPPattern.serverLocalError(error) => SMTPServerDataBodyError(error)
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
			case _ => {
				input match {
					case SMTPPattern.serverGreet(name) => SMTPServerGreet(name)
					case SMTPPattern.serverQuit() => SMTPServerQuit
					case SMTPPattern.serverError(code, error) => SMTPServerError(code, error)
					case _ => SMTPUnknownMessage
				}
			}
		}
	}
}

object Client {
	def process(config: Config) = {
		(new Client(config)).process()
	}
}