package net.scbuild.resolvers

import org.apache.ivy.plugins.resolver.RepositoryResolver
import org.apache.ivy.plugins.resolver.AbstractResolver
import net.scbuild.BuildProps

/**
 * @author marius
 */

trait RepoResolver {
  def ivyResolver: AbstractResolver
  def name: String
}

object FileSystemResolver {
  def apply(ivyPattern: String,
            artifactPattern: String,
            local: Boolean)(implicit props: BuildProps) = {
    new FileSystemResolver("local", ivyPattern, artifactPattern, local, props get ("ivy.local.default.root") getOrElse "./")
  }
}

case class FileSystemResolver(name: String,
                              ivyPattern: String,
                              artifactPattern: String,
                              local: Boolean,
                              root: String) extends RepoResolver {

  val ivyResolver = {
    val fs = new org.apache.ivy.plugins.resolver.FileSystemResolver()
    fs.setName(name)
    fs.setLocal(local)
    fs.addIvyPattern(s"$root/$ivyPattern")
    fs.addArtifactPattern(s"$root/$artifactPattern")
    fs
  }

}

case class IBiblioResolver(name: String) extends RepoResolver {

  val ivyResolver = {
    val ibiblio = new org.apache.ivy.plugins.resolver.IBiblioResolver
    ibiblio.setM2compatible(true)
    ibiblio.setUsepoms(true)
    ibiblio.setName(name)
    ibiblio
  }

}

object ChainResolver {
  def apply(resolvers: RepoResolver*) = new ChainResolver("chain", resolvers.toList)
}

case class ChainResolver(name: String, resolvers: List[RepoResolver]) extends RepoResolver {

  val ivyResolver = {
    val cr = new org.apache.ivy.plugins.resolver.ChainResolver
    cr.setName(name)
    for { r <- resolvers } {
      cr.add(r.ivyResolver)
    }
    cr
  }

}