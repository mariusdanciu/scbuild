package net.scbuild

import scala.util.Failure
import scala.util.Try
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.DownloadOptions
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.resolve.ResolvedModuleRevision
import scala.util.Success
import org.apache.ivy.core.report.DownloadStatus
import net.scbuild.resolvers.BuildSetup

/**
 * @author marius
 */

case class Artifact(name: String, artifactType: String, path: String)

object Dependency {
  def apply(organization: String, module: String, version: String)(implicit setup: BuildSetup) = {
    new Dependency(organization, module, version, List("default"))
  }

}
case class Dependency(organization: String, module: String, version: String, configs: List[String])(implicit setup: BuildSetup) {

  private val ro = new ResolveOptions()

  ro.setTransitive(true);
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

}