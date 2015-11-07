package net.scbuild.resolvers

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.RepositoryResolver
import org.apache.ivy.plugins.resolver.AbstractResolver

/**
 * @author marius
 */

trait RepoResolver {
  def ivyResolver: AbstractResolver
  def register(implicit settings: BuildSetup)
}

object FileSystemResolver {
  def apply(ivyPattern: String,
            artifactPattern: String,
            local: Boolean)(implicit settings: BuildSetup) = {
    new FileSystemResolver("local", ivyPattern, artifactPattern, local, settings.ivySettings.getVariable("ivy.local.default.root"))
  }
}

case class FileSystemResolver(name: String,
                              ivyPattern: String,
                              artifactPattern: String,
                              local: Boolean,
                              root: String)(implicit settings: BuildSetup) extends RepoResolver {

  val ivyResolver = {
    val fs = new org.apache.ivy.plugins.resolver.FileSystemResolver()
    fs.setName(name)
    fs.setLocal(local)
    fs.addIvyPattern(s"$root/$ivyPattern")
    fs.addArtifactPattern(s"$root/$artifactPattern")
    fs.setSettings(settings.ivySettings)

    fs
  }

  def register(implicit settings: BuildSetup) = {
    settings.ivySettings.addResolver(ivyResolver)
    settings.ivySettings.setDefaultResolver(name)
  }
}

case class IBiblioResolver(name: String)(implicit settings: BuildSetup) extends RepoResolver {

  val ivyResolver = {
    val ibiblio = new org.apache.ivy.plugins.resolver.IBiblioResolver
    ibiblio.setM2compatible(true)
    ibiblio.setUsepoms(true)
    ibiblio.setName(name)
    ibiblio.setSettings(settings.ivySettings)

    ibiblio
  }

  def register(implicit settings: BuildSetup) = {
    settings.ivySettings.addResolver(ivyResolver)
    settings.ivySettings.setDefaultResolver(name)
  }
}

object ChainResolver {
  def apply(resolvers: RepoResolver*)(implicit settings: BuildSetup) = new ChainResolver(resolvers.toList)
}

case class ChainResolver(resolvers: List[RepoResolver])(implicit val settings: BuildSetup) {

  val ivyResolver = {
    val cr = new org.apache.ivy.plugins.resolver.ChainResolver
    cr.setName("chain")
    cr.setSettings(settings.ivySettings)
    for { r <- resolvers } {
      cr.add(r.ivyResolver)
    }
    cr
  }

  def register(implicit settings: BuildSetup) = {
    settings.ivySettings.addResolver(ivyResolver)
    settings.ivySettings.setDefaultResolver("chain")
  }
}