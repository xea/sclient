package io.soshite.sclient.generator

import scala.io.Source

class IDContentGenerator extends ContentGenerator {

  override def generate(id : String) : String = {
    val is = getClass.getResourceAsStream(id)
    Source.fromInputStream(is).mkString
  }
}