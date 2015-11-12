package net.scbuild

import java.io.File
import scala.util.Failure
import net.scbuild.resolvers._

/**
 * @author marius
 */
object Main extends App {

  implicit val props = BuildProps("ivy.local.default.root" -> "/home/marius/.ivy2/local")

  val fs = FileSystemResolver("[organisation]/[module]/[revision]/ivys/ivy.xml", "[organisation]/[module]/[revision]/jars/[artifact].[ext]", true)

  val ibiblio = IBiblioResolver("central")

  implicit val c = BuildContext(props, ChainResolver(ibiblio, fs))

  val dep = Dependency("com.ibm.tdw2", "tdw2_2.10", "0.1.0")
  
  println(dep.resolveArtifacts match {
    case Failure(f) => f.printStackTrace()
    case s          => s
  })

  println(dep.dependencies)

  println(dep.resolveAll)

  
  val test = Dependency("com.ibm.test", "test", "0.1.0")
  test.publish(PublishSettings(List(Artifact("test", "jar", "/home/marius/work/dev/git/scbuild/scbuildcore/lib/test.jar")), "build"))
}