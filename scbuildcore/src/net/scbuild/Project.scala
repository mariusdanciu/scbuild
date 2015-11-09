package net.scbuild

import scala.util.Try
import scala.util.Success
import scala.util.Failure

/**
 * @author marius
 */

object Project {
  def apply(dependencies: List[Dependency]) = new Project("src/main/scala",
    "src/test/scala",
    "src/main/resources",
    "src/test/resources",
    dependencies)
}

case class Project(srcDir: String, testDir: String, resourcesDir: String, testResourcesDir: String, dependencies: List[Dependency]) {
  def compile() = {
    val l = sequence(for {
      d <- dependencies
    } yield {
      d.resolveAll
    }) map {_.flatten}
    
  }
  
  def sequence[A](t: List[Try[A]]): Try[List[A]] = {
    ((Success(Nil) : Try[List[A]]) /: t){
      case (Success(l), Success(a)) => Success(l ::: List(a))
      case (Failure(t), _) => Failure(t)
      case (_, Failure(t)) => Failure(t)
    }
  }
}

