package net.scbuild

import java.io.File
import scala.util.Failure
import org.apache.ivy.Ivy
import org.apache.ivy.core.settings.IvySettings
import net.scbuild.resolvers._

/**
 * @author marius
 */
object Main extends App {

  implicit val setup = BuildSetup(BuildProps());
  
  val fs = FileSystemResolver("[organisation]/[module]/[revision]/ivys/ivy.xml", "[organisation]/[module]/[revision]/jars/[artifact].[ext]", true)

  val ibiblio = IBiblioResolver("central")

  ChainResolver(ibiblio, fs) register


  val dep = Dependency("com.ibm.tdw2", "tdw2_2.10", "0.1.0")
  println(dep.resolveArtifacts match {
    case Failure(f) => f.printStackTrace()
    case s          => s
  })

  println(dep.dependencies)

  println(dep.resolveAll)

}