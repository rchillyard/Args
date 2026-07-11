package com.phasmidsoftware.examples

import com.phasmidsoftware.args.Args

/**
  * In this example, we convert all of the arguments into Ints.
  *
  */
object ExampleInt {

  def main(args: Array[String]): Unit = args.length match {
    case 0 => System.err.println("no command-line arguments")
    case _ => (for (ss <- Args.parse(args); xs = ss.as[Int]) yield xs) foreach println
  }
}
