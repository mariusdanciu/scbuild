package net.scbuild

/**
 * @author marius
 */
case class ArtifactNotFoundException(organization: String, module: String, version: String) extends Exception {

  override def toString() = s"ArtifactNotFoundException($organization, $module, $version)"
}