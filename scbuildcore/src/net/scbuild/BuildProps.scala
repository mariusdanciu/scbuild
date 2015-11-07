package net.scbuild

import org.apache.ivy.core.settings.IvyVariableContainer

/**
 * @author marius
 */
case class BuildProps() extends IvyVariableContainer {
  var map : Map[String, String] = Map.empty
  var prefix = ""
  
  setVariable("ivy.local.default.root", "/home/marius/.ivy2/local", true)
  
  def setVariable(name: String, value: String, overwrite: Boolean) {
    map += (name -> value)
  }
  
  def getVariable(name: String): String = {
    map get (name) getOrElse null
  }
  
  def setEnvironmentPrefix(prefix: String) {
    this.prefix = prefix
  }

}