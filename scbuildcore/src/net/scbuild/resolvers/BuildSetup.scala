package net.scbuild.resolvers

import org.apache.ivy.Ivy
import net.scbuild.BuildProps
import org.apache.ivy.core.settings.IvySettings
import java.io.File

/**
 * @author marius
 */

case class BuildSetup(props: BuildProps) {

  lazy val ivySettings = {
    val settings = new IvySettings(props)
    settings.setDefaultCache(new File("ivy/cache"))
    settings
  }

  lazy val ivy = {
    Ivy.newInstance(ivySettings)
  }

}