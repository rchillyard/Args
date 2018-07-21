package com.phasmidsoftware.util

import scala.util._

/**
  * @author scalaprof
  */
object MonadOps {

  def sequence[X](xys: Seq[Try[X]]): Try[Seq[X]] = (Try(Seq[X]()) /: xys) {
    (xsy, xy) => for (xs <- xsy; x <- xy) yield xs :+ x
  }
}