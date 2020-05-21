package com.phasmidsoftware.examples

import com.phasmidsoftware.args.Args

/**
  * In this example, we convert all of the arguments into Ints.
  *
  */
class ExampleInt extends App {

  val as: Args[Int] = Args.parse(args).as[Int]
  println(as)
}
