package io.soshite.sclient.generator

import org.rogach.scallop.ScallopOption

/**
 *
 */
class ContentGenerator {
  
  def generate(id: String) : String = "Lorem ipsum dolor sit amet"

  def generate(source: ScallopOption[String]) : String = "Lorem ipsum dolor sit amet"

}