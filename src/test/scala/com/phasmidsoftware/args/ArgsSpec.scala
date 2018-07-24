/*
 * Copyright (w) 2018. Phasmid Software
 */

package com.phasmidsoftware.args

import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}

class ArgsSpec extends FlatSpec with Matchers {

  private val argFilename = "argFilename"
  private val nameF = "f"
  private val cmdF = "-" + nameF
  private val sX = "x"
  private val sY = "y"
  private val s1 = "1"
  private val x1 = 1

  private def printFilename(so: Option[String]): Unit = so.foreach(s => println(s"$argFilename: $s"))

  val processor: Map[String, Option[String] => Unit] = Map[String, Option[String] => Unit](nameF -> printFilename)

  behavior of "Arg"

  it should "work for " + sX + ": " + s1 in {
    val target = Arg(sX, s1)
    target.name shouldBe Some(sX)
    target.value shouldBe Some(s1)
  }

  it should "implement map" in {
    val target = Arg(sX, s1)
    val result = target.map(_.toInt)
    result.value shouldBe Some(x1)
  }

  it should "implement map with exception" in {
    val target = Arg(sX, sX)
    a[java.lang.NumberFormatException] shouldBe thrownBy(target.map(_.toInt))
  }

  it should "implement toY" in {
    implicit object DerivableStringInt$ extends Derivable[Int] {

      def deriveFrom[X](x: X): Int = x match {
        case x: String => x.toInt
        case _ => throw NotImplemented(s"deriveFrom: $x")
      }
    }
    val target = Arg(sX, s1)
    target.toY[Int] shouldBe 1
  }

  it should "implement byName" in {
    val target = Arg(sX, sX)
    target.byName(sX) should matchPattern { case Some(_) => }
  }

  it should "process " + sX + ": append" in {
    val sb = new StringBuilder
    val processor = Map[String, Option[String] => Unit](sX.->[Option[String] => Unit]({ x => sb.append(x) }))
    val target = Arg(sX, s1)
    val result = target.process(processor)
    result should matchPattern { case Success(None) => }
    sb.toString shouldBe "Some(" + s1 + ")"
  }

  it should "not process " + sY + ": append" in {
    val sb = new StringBuilder
    val processor = Map[String, Option[String] => Unit](sX.->[Option[String] => Unit] { x => sb.append(x) })
    val target = Arg(sY, s1)
    val result = target.process(processor)
    result should matchPattern { case Failure(_) => }
  }

  behavior of "Args"

  it should "work" in {
    val target = Args.create(Arg(sX, s1))
    target.size shouldBe 1
    target.head.name shouldBe Some(sX)
    target.head.value shouldBe Some(s1)
  }

  it should "implement map" in {
    val target = Args.create(Arg(sX, s1))
    val result = target.map[Int](_.toInt)
    result.head.value shouldBe Some(x1)
  }

  it should "implement getArg with good name" in {
    val x = Arg(sX, s1)
    val target = Args.create(x)
    val result = target.getArg(sX)
    result shouldBe Some(`x`)
  }

  it should "not implement getArg with bad name" in {
    val x = Arg(sX, s1)
    val target = Args.create(x)
    val result = target.getArg("")
    result shouldBe None
  }

  it should "not implement getArg with ambiguous name" in {
    val x = Arg(sX, s1)
    val target = Args.create(x, x)
    a [AmbiguousNameException] shouldBe thrownBy(target.getArg(sX))
  }

  it should "implement options" in {
    val target = Args.parse(Array("-"+sX, s1))
    target.options shouldBe Map(sX -> Some(s1))
  }

  it should "implement operands" in {
    val target = Args.parse(Array(s1))
    target.operands shouldBe Seq(s1)
  }

  it should "process " + sX + ": append" in {
    val sA = "a"
    val sb = new StringBuilder
    val processor = Map[String, Option[String] => Unit](sX.->[Option[String] => Unit] { case Some(x) => sb.append(x); case _ => })
    val target = Args.create(Arg(sX, s1), Arg(sX, sA))
    val result = target.process(processor)
    result should matchPattern { case Success(_) => }
    sb.toString shouldBe s1 + sA
  }

  it should "parse " + cmdF + " " + argFilename in {
    val args = Array(cmdF, argFilename)
    val as = Args.parse(args)
    as.xas.length shouldBe 1
    as.xas.head shouldBe Arg(Some(nameF), Some(argFilename))
  }

  it should "parse 1 2 3" in {
    val sa = Args.parse(Array("1", "2", "3"))
    sa.xas.length shouldBe 3
    sa.xas.head shouldBe Arg(None, Some("1"))
    val xa = sa.map[Int](_.toInt)
    xa shouldBe Args(Seq(Arg(None, Some(1)), Arg(None, Some(2)), Arg(None, Some(3))))
    val processor = Map[String, Option[Int] => Unit]()
    xa.process(processor) should matchPattern { case Success(Seq(1, 2, 3)) => }
  }

  it should "parsePosix 1 2 3" in {
    val sa = Args.parse(Array("1", "2", "3"))
    sa.xas.length shouldBe 3
    sa.xas.head shouldBe Arg(None, Some("1"))
    val xa = sa.map[Int](_.toInt)
    xa shouldBe Args(Seq(Arg(None, Some(1)), Arg(None, Some(2)), Arg(None, Some(3))))
    val processor = Map[String, Option[Int] => Unit]()
    xa.process(processor) should matchPattern { case Success(Seq(1, 2, 3)) => }
  }

  it should """parsePosix "-xf argFilename 3.1415927"""" in {
    val sa = Args.parsePosix(Array("-xf", "argFilename", "3.1415927"))
    sa.xas.length shouldBe 3
    sa.xas.head shouldBe Arg(Some("x"), None)
    sa.xas.tail.head shouldBe Arg(Some("f"), Some("argFilename"))
    sa.xas.last shouldBe Arg(None, Some("3.1415927"))
  }

  it should """parsePosix "-xf argFilename -p 3.1415927"""" in {
    val sa = Args.parsePosix(Array("-xf", "argFilename", "-p", "3.1415927"))
    sa.xas.length shouldBe 3
    sa.xas.head shouldBe Arg(Some("x"), None)
    sa.xas.tail.head shouldBe Arg(Some("f"), Some("argFilename"))
    sa.xas.last shouldBe Arg(Some("p"), Some("3.1415927"))
  }

  it should """implement isDefined("x")""" in {
    val sa = Args[String](Seq(Arg(Some("x"), None), Arg(Some("f"), Some("argFilename")), Arg(None, Some("3.1415927"))))
    sa.isDefined("x") shouldBe true
  }

  it should """implement getArgValue("f")""" in {
    implicit object DerivableStringString$ extends Derivable[String] {

      def deriveFrom[X](x: X): String = x match {
        case x: String => x
        case _ => throw NotImplemented(s"deriveFrom: $x")
      }
    }
    val sa = Args[String](Seq(Arg(Some("x"), None), Arg(Some("f"), Some("argFilename")), Arg(None, Some("3.1415927"))))
    sa.getArgValue("f") shouldBe Some("argFilename")
  }

  it should """implement process for complex Args""" in {
    var x = false
    var filename = ""
    val sa = Args[String](Seq(Arg(Some("x"), None), Arg(Some("f"), Some("argFilename")), Arg(None, Some("3.1415927"))))
    val processor = Map[String, Option[String] => Unit]("x" -> { _ => x = true }, "f" -> { case Some(w) => filename = w; case _ => })
    sa.process(processor) should matchPattern { case Success(Seq("3.1415927")) => }
    x shouldBe true
    filename shouldBe "argFilename"
  }

  behavior of "Args validation"
  it should "parse " + cmdF + " " + argFilename in {
    val args = Array(cmdF, argFilename, "positionalArg")
    val as: Args[String] = Args.parsePosix(args, Some(cmdF + " " + "filename"))
    as should matchPattern { case Args(_) => }
  }

  it should "parse " + cmdF + argFilename + " where filename is optional (1)" in {
    val args = Array(cmdF, argFilename, "3.1415927")
    val as: Args[String] = Args.parsePosix(args, Some(cmdF + "[ filename" + "]"))
    as should matchPattern { case Args(_) => }
  }

  it should "parse " + cmdF + argFilename + " where filename is optional (2)" in {
    val args = Array(cmdF + argFilename, "positionalArg")
    val as: Args[String] = Args.parsePosix(args, Some(cmdF + "[ filename" + "]"))
    as should matchPattern { case Args(_) => }
  }

  it should "parse " + cmdF + " " + argFilename + "where -xf filename required" in {
    a[ValidationException[String]] shouldBe thrownBy(Args.parsePosix(Array(cmdF, argFilename), Some("-xf filename")))
  }

  it should """implement validate(String)""" in {
    val sa = Args.parsePosix(Array("-xf", "argFilename", "-p", "3.1415927"))
    sa.validate("-x[f filename][p number]") shouldBe sa
    // TODO why does the following not work?
//    sa.validate("-xf filename -p number") shouldBe sa
  }

  it should "validate -x[f[ filename]] as a synopsis for -xfargFilename" in {
    Args.parsePosix(Array("-xfargFilename"), Some("-x[f[ filename]]")) shouldBe Args.create(Arg("x"), Arg("f", "argFilename"))
  }


}

case class NotImplemented(str: String) extends Exception(s"Not Implemented for $str")
