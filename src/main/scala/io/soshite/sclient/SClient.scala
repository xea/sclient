package io.soshite.sclient

import java.io.File
import io.soshite.sclient.config.Config
import io.soshite.sclient.log.Logger

import scala.collection.mutable.ArrayBuffer


object SClient {

  def main(args: Array[String]) {
    val cliOptions = new Config(args)
    val result = validateConfiguration(cliOptions)

    Logger.verbose("Starting console client")

    if (result.isEmpty) {
      Client.process(cliOptions)
    } else {
      Logger.error(result.get.mkString("\n"))
    }
  }

  def validateConfiguration(config: Config): Option[ArrayBuffer[String]] = {
    val errors = new ArrayBuffer[String]()

    if (config.eml.isSupplied) {
      val file = new File(config.eml())
      if (!file.exists()) {
        errors += "Source EML file not found: " + config.eml()
      }
    }

    Logger.loglevel = Logger.ALL;//config.verbose()

    if (errors.length > 0) {
      Some(errors)
    } else {
      None
    }
  }
}