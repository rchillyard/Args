package com.phasmidsoftware.examples

import com.phasmidsoftware.args.Args

import scala.util.Try

/**
  * In this example, we convert all of the arguments into Ints.
  *
  */
class ExampleInt extends App {

  val as: Try[Args[Int]] = for (ss <- Args.parse(args); xs = ss.as[Int]) yield xs
  as foreach println
}
