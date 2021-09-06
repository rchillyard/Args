/*
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.util

import scala.util._

/**
  * @author scalaprof
  */
object MonadOps {

  def sequence[X](xys: Seq[Try[X]]): Try[Seq[X]] = xys.foldLeft(Try(Seq[X]())) {
    (xsy, xy) => for (xs <- xsy; x <- xy) yield xs :+ x
  }

  /**
    * method to map a pair of Option values (of same underlying type) into an Option value of another type (which could be the same of course)
    *
    * @param t1o a Option[T1] value
    * @param t2o a Option[T2] value
    * @param f   function which takes a T1 and a T2 parameter and yields a R result
    * @tparam T1 the underlying type of the first parameter
    * @tparam R  the result type
    * @return a Option[U]
    */
  def map2[T1, T2, R](t1o: Option[T1], t2o: => Option[T2])(f: (T1, T2) => R): Option[R] = for {t1 <- t1o; t2 <- t2o} yield f(t1, t2)

  def liftOption[T, R](f: T => R): Option[T] => Option[R] = _ map f

  def liftTry[T, R](f: T => R): Try[T] => Try[R] = _ map f

  def liftOptionToTry[T]: Option[T] => Try[T] = to => Try(to.get)

  def prune[K, V](x: Seq[(K, Option[V])]): Map[K, V] = (for ((k, vo) <- x; if vo.isDefined) yield (k, vo.get)).toMap
}