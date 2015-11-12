package net.scbuild.resolvers

import org.apache.ivy.Ivy
import net.scbuild.BuildProps
import org.apache.ivy.core.settings.IvySettings
import java.io.File
import org.apache.ivy.core.settings.IvyVariableContainer
import scala.collection.mutable.HashMap

/**
 * @author marius
 */

case class BuildContext(props: BuildProps, resolver: RepoResolver) {

  val ivy = {

    val ivyConf = new IvyVariableContainer() {
      val map: HashMap[String, String] = HashMap.empty
      var prefix = ""
      def getVariable(name: String): String = map get name getOrElse null
      def setEnvironmentPrefix(prefix: String): Unit = this.prefix = prefix
      def setVariable(name: String, value: String, overwrite: Boolean): Unit = if (overwrite) {
        map += name -> value
      }
    }

    for { (n, v) <- props.asMap } {
      ivyConf.setVariable(n, v, true)
    }

    val settings = new IvySettings(ivyConf)
    settings.setDefaultCache(new File("ivy/cache"))
    settings.addResolver(resolver.ivyResolver)
    settings.setDefaultResolver(resolver.name)

    Ivy.newInstance(settings)
  }

}