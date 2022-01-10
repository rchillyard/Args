/**
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.args

import java.io.File
import java.net.URL
import scala.util.Try

/**
  * Type-class trait to allow conversion from type X to type T
  *
  * @tparam T the result type
  */
trait Derivable[T] {
  /**
    * Method to convert an X to a T
    *
    * @param x the X value
    * @tparam X the input type
    * @return a T
    */
  def deriveFrom[X](x: X): T

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

  implicit object DerivableStringBoolean$ extends Derivable[Boolean] {

    def deriveFrom[X](x: X): Boolean = x match {
      case x: String => x.toBoolean
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }

    def deriveFromOpt[X](x: X): Option[Boolean] = x match {
      case x: String => x.toBooleanOption
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }
  }

  implicit object DerivableStringInt$ extends Derivable[Int] {

    def deriveFrom[X](x: X): Int = x match {
      case x: String => x.toInt
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }

    def deriveFromOpt[X](x: X): Option[Int] = x match {
      case x: String => x.toIntOption
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }
  }

  implicit object DerivableStringDouble$ extends Derivable[Double] {

    def deriveFrom[X](x: X): Double = x match {
      case x: String => x.toDouble
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }

    def deriveFromOpt[X](x: X): Option[Double] = x match {
      case x: String => x.toDoubleOption
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }
  }

  implicit object DerivableStringFile$ extends Derivable[File] {

    def deriveFrom[X](x: X): File = x match {
      case x: String => new File(x)
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }

    def deriveFromOpt[X](x: X): Option[File] = x match {
      case x: String => Try(new File(x)).toOption
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }
  }

  implicit object DerivableStringURL$ extends Derivable[URL] {

    def deriveFrom[X](x: X): URL = x match {
      case x: String => new URL(x)
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }

    def deriveFromOpt[X](x: X): Option[URL] = x match {
      case x: String => Try(new URL(x)).toOption
      case _ => throw NoDerivationAvailable(x.getClass, Int.getClass)
    }
  }
}

case class NoDerivationAvailable(xc: Class[_], yc: Class[_]) extends RuntimeException(s"no implicitly defined conversion from $xc to $yc")
