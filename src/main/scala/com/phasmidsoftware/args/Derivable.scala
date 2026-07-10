/**
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.args

import java.io.File
import java.net.URI
import scala.util.Try

/**
  * Type-class trait to allow conversion from type X to type T
  *
  * @tparam T the result type
  */
trait Derivable[T] {
  /**
    * Method to convert an X to an Option[T].
    *
    * @param x the X value
    * @tparam X the input type
    * @return an Option[T]
    */
  def deriveFromOpt[X](x: X): Option[T]
}

object Derivable {

  given DerivableStringBoolean: Derivable[Boolean] with {
    def deriveFromOpt[X](x: X): Option[Boolean] = x match {
      case x: String => x.toBooleanOption
      case _ => throw NoDerivationAvailable(x.getClass, classOf[Boolean])
    }
  }

  given DerivableStringInt: Derivable[Int] with {
    def deriveFromOpt[X](x: X): Option[Int] = x match {
      case x: String => x.toIntOption
      case _ => throw NoDerivationAvailable(x.getClass, classOf[Int])
    }
  }

  given DerivableStringDouble: Derivable[Double] with {
    def deriveFromOpt[X](x: X): Option[Double] = x match {
      case x: String => x.toDoubleOption
      case _ => throw NoDerivationAvailable(x.getClass, classOf[Double])
    }
  }

  given DerivableStringFile: Derivable[File] with {
    def deriveFromOpt[X](x: X): Option[File] = x match {
      case x: String => Try(new File(x)).toOption
      case _ => throw NoDerivationAvailable(x.getClass, classOf[File])
    }
  }

  given DerivableStringURL: Derivable[java.net.URL] with {
    def deriveFromOpt[X](x: X): Option[java.net.URL] = x match {
      case x: String => Try(URI(x).toURL).toOption
      case _ => throw NoDerivationAvailable(x.getClass, classOf[java.net.URL])
    }
  }
}

case class NoDerivationAvailable(xc: Class[_], yc: Class[_]) extends RuntimeException(s"no implicitly defined conversion from $xc to $yc")
