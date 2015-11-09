package net.scbuild

import org.apache.ivy.core.settings.IvyVariableContainer

object BuildProps {
  def apply(props: (String, String)*) = new BuildProps(props.toMap)
}
/**
 * @author marius
 */
case class BuildProps(map: Map[String, String]) {

  def get(name: String) = map get name

  def asMap = map

}