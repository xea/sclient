package io.soshite.sclient.generator

import java.io.{ByteArrayOutputStream, FileInputStream, File}
import io.soshite.sclient.config.Config
import org.rogach.scallop.ScallopOption

import scala.io.Source

class EMLContentGenerator(config: Config) extends ContentGenerator {

  override def generate(id : String) : String = {
	  fileToString(new File(id), "UTF-8")
  }

	override def generate(source: ScallopOption[String]) : String = {
		if (source.isEmpty) "" else fileToString(new File(source()), "UTF-8")
	}

	def fileToString(file: File, encoding: String) = {
		val inStream = new FileInputStream(file)
		val outStream = new ByteArrayOutputStream
		try {
			var reading = true
			while ( reading ) {
				inStream.read() match {
					case -1 => reading = false
					case c => outStream.write(c)
				}
			}
			outStream.flush()
		}
		finally {
			inStream.close()
		}
		new String(outStream.toByteArray(), encoding)
	}
}