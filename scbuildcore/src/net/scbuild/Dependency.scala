package net.scbuild

import java.net.URL
import java.util.Date
import scala.collection.JavaConversions.seqAsJavaList
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.apache.ivy.core.deliver.DeliverOptions
import org.apache.ivy.core.module.descriptor.DefaultArtifact
import org.apache.ivy.core.module.descriptor.DefaultDependencyArtifactDescriptor
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.publish.PublishOptions
import org.apache.ivy.core.report.DownloadStatus
import org.apache.ivy.core.resolve.DownloadOptions
import org.apache.ivy.core.resolve.ResolveOptions
import net.scbuild.resolvers.BuildContext
import org.apache.ivy.core.module.descriptor.Configuration

case class Artifact(name: String, artifactType: String, path: String)

object Dependency {
  def apply(organization: String, module: String, version: String)(implicit setup: BuildContext) = {
    new Dependency(organization, module, version, List("default"))
  }
}
case class Dependency(organization: String, module: String, version: String, configs: List[String])(implicit setup: BuildContext) {

  private val ro = new ResolveOptions()

  ro.setTransitive(true)
  ro.setUseCacheOnly(false)
  ro.setRefresh(true)
  ro.setRevision(version)
  ro.setConfs(configs.toArray)

  def dependencies: Try[List[Dependency]] = {
    val mr = new ModuleRevisionId(new ModuleId(organization, module), version)
    val found = setup.ivy.findModule(mr)

    if (found != null) {
      Try(for { d <- found.getDescriptor.getDependencies.toList } yield {
        Dependency(d.getDependencyId.getOrganisation, d.getDependencyId.getName, d.getDependencyRevisionId.getRevision, d.getModuleConfigurations.toList)
      })
    } else {
      Failure(ArtifactNotFoundException(organization, module, version))
    }
  }

  def resolveArtifacts: Try[List[Artifact]] = {
    val mr = new ModuleRevisionId(new ModuleId(organization, module), version)

    val found = setup.ivy.findModule(mr)

    if (found != null) {
      Try {
        found.getDescriptor.getAllArtifacts flatMap { a =>
          {
            val k = found.getArtifactResolver.download(Array(a), new DownloadOptions)
            if (k.getArtifactReport(a).getDownloadStatus() != DownloadStatus.FAILED) {
              val l = k.getArtifactsReports.map { d =>
                Artifact(d.getArtifact.getName, d.getArtifact.getType, d.getLocalFile.getAbsolutePath)
              }
              l
            } else Nil
          }
        } toList
      }
    } else {
      Failure(ArtifactNotFoundException(organization, module, version))
    }
  }

  def resolveAll: Try[List[Artifact]] = {

    def resolveDependencies(d: List[Dependency], found: List[Artifact]): Try[List[Artifact]] = d match {
      case Nil => Success(found)
      case h :: tail => for {
        l <- h.resolveArtifacts
        d <- resolveDependencies(tail, found)
      } yield {
        l ++ d
      }
    }

    for {
      a <- resolveArtifacts
      d <- dependencies
      l <- resolveDependencies(d, Nil)
    } yield {
      a ++ l
    }
  }

  def publish(ps: PublishSettings) = {
    val mr = new ModuleRevisionId(new ModuleId(organization, module), version)

    import scala.collection.JavaConversions._

    val arts = for { Artifact(name, tpe, path) <- ps.artifacts } yield {
      new DefaultArtifact(mr, new Date, name, tpe, tpe, null, null)
    }

    val m = DefaultModuleDescriptor.newDefaultInstance(mr, Array())
    for {
      c <- ps.confs
      a <- arts
    } {
      val conf = new Configuration(c)
      m.addConfiguration(conf)
      m.addArtifact(c, a)
    }
    setup.ivy.getSettings.getResolutionCacheManager.saveResolvedModuleDescriptor(m)

    val ro = new ResolveOptions
    val r = setup.ivy.resolve(m, ro)

    val dopts = new DeliverOptions
    dopts.setConfs(ps.confs.toArray)
    dopts.setPubdate(new Date)
    dopts.setStatus(ps.status)

    setup.ivy.deliver(mr, version, ps.ivyPattern, dopts)

    val opts = new PublishOptions
    opts.setConfs(ps.confs.toArray)
    opts.setSrcIvyPattern(ps.ivyPattern)
    opts.setOverwrite(true)

    val published = setup.ivy.publish(mr, ps.artifacts.map { _.path }, ps.repositoryName, opts)
  }

}
object PublishSettings {
  def apply(artifacts: List[Artifact], confs: String*) = new PublishSettings(confs.toList, "release", "local", "ivy.xml", artifacts)
}

case class PublishSettings(confs: List[String], status: String, repositoryName: String, ivyPattern: String, artifacts: List[Artifact])
