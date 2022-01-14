/*
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.args

import org.scalatest.flatspec
import org.scalatest.matchers.should

import java.io.File
import java.net.URL
import scala.util.{Failure, Success}

class ArgsSpec extends flatspec.AnyFlatSpec with should.Matchers {

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

  it should "implement flatMap" in {
    val target = Arg(sX, s1)
    val result = target.flatMap(w => Arg(None, w.toIntOption))
    result.value shouldBe Some(x1)
  }

  it should "implement map with exception" in {
    val target = Arg(sX, sX)
    a[java.lang.NumberFormatException] shouldBe thrownBy(target.map(_.toInt))
  }

  it should "implement as[Int]" in {
    val target = Arg(sX, s1)
    target.as[Int] shouldBe Arg(Some("x"), Some(1))
  }

  it should "implement as[Boolean]" in {
    val target = Arg(sX, "true")
    target.as[Boolean] shouldBe Arg(Some("x"), Some(true))
  }

  it should "implement as[Double]" in {
    val target = Arg(sX, "1.0")
    target.as[Double] shouldBe Arg(Some("x"), Some(1.0))
  }

  it should "implement as[File]" in {
    val target = Arg(sX, "build.sbt")
    target.as[File] shouldBe Arg(Some("x"), Some(new File("build.sbt")))
  }

  it should "implement as[URL]" in {
    val target = Arg(sX, "https://www.google.com")
    target.as[URL] shouldBe Arg(Some("x"), Some(new URL("https://www.google.com")))
  }

  it should "implement as[URL] malformed" in {
    val target = Arg(sX, "://www.google.com")
    target.as[URL] shouldBe Arg(Some("x"), None)
  }

  it should "implement toY" in {
    val target = Arg(sX, s1)
    target.toY[Int] shouldBe Success(1)
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

  it should "compare properly" in {
    val target = Arg(sY, s1)
    target.compare(Arg(sY, s1)) shouldBe 0
    target.compare(Arg(sX, s1)) shouldBe 1
    target.compare(Arg("z", s1)) shouldBe -1
    // CONSIDER is it correct that the value is not significant to compare?
    target.compare(Arg(sY, "2")) shouldBe 0
  }

  it should "hasValue properly" in {
    Arg(sY, s1).hasValue shouldBe true
    Arg(sX).hasValue shouldBe false
  }

  behavior of "Args"

  it should "create" in {
    val target = Args.create(Arg(sX, s1))
    target.size shouldBe 1
    target.head.name shouldBe Some(sX)
    target.head.value shouldBe Some(s1)
  }

  it should "showArgs" in {
    val args = Array("-x", "1")
    Args.showArgs(args) shouldBe "-x 1"
  }

  it should "implement :+" in {
    val target = Args.empty[String]
    val result = target :+ Arg(sX)
    result shouldBe Args.create(Arg(sX))
    result :+ Arg(sY) shouldBe Args.create(Arg(sX), Arg(sY))
  }

  it should "implement +:" in {
    val target = Args.empty[String]
    val result = Arg(sX) +: target
    result shouldBe Args.create(Arg(sX))
    Arg(sY) +: result shouldBe Args.create(Arg(sY), Arg(sX))
  }

  it should "implement ++" in {
    val target = Args.create(Arg(sX))
    val result = target ++ Args.create(Arg(sY))
    result shouldBe Args(Seq(Arg(sX), Arg(sY)))
  }

  it should "implement mapMap" in {
    val target = Args.create(Arg(sX, s1))
    val result: Args[Int] = target.mapMap(_.toInt)
    result.head.value shouldBe Some(x1)
  }

  it should "implement map" in {
    val target = Args.create(Arg(sX, s1))
    val result: Args[Int] = target.map(xa => xa.map(_.toInt))
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
    a[AmbiguousNameException] shouldBe thrownBy(target.getArg(sX))
  }

  it should "implement options" in {
    val target = Args.parseSimple(Array("-" + sX, s1))
    target.get.options shouldBe Map(sX -> Some(s1))
  }

  it should "implement operands (form 1)" in {
    val target = Args.parseSimple(Array(s1))
    target.get.operands shouldBe Seq(s1)
  }

  it should "implement operands (form 2) (1)" in {
    val target = Args.parseSimple(Array(s1))
    val p = new SynopsisParser
    val s = p.parseSynopsis("first [second]")
    target.get.operands(s) shouldBe Map("first" -> s1)
  }

  it should "implement operands (form 2) (2)" in {
    val target = Args.parseSimple(Array(s1, sX))
    val p = new SynopsisParser
    val s = p.parseSynopsis("first [second]")
    target.get.operands(s) shouldBe Map("first" -> s1, "second" -> sX)
  }

  it should "fail on empty array" in {
    val sa = Args.parse(Array[String]())
    sa.isSuccess shouldBe false
  }

  it should "implement toList" in {
    val sa = Args.parse(Array("-f", "argFilename", "3.1415927"))
    sa.isSuccess shouldBe true
    sa.get.to(List) shouldBe List(Arg(Some("f"), Some("argFilename")), Arg(None, Some("3.1415927")))
  }

  it should "do implement matchAndShift 1" in {
    val sa: Args[String] = Args.make(Array("-f", "argFilename", "3.1415927"))
    a[MatchError] shouldBe thrownBy(sa.matchAndShift { case Arg(None, None) => })
  }

  it should "toString" in {
    val sa = Args.parse(Array("-f", "argFilename", "3.1415927"))
    sa.isSuccess shouldBe true
    sa.get.toString shouldBe "Arg: flag f with value: argFilename; Arg: flag anonymous with value: 3.1415927"
  }

  it should "do implement matchAndShift 2" in {
    val sa: Args[String] = Args.make(Array("-f", "argFilename", "3.1415927"))
    println(sa.matchAndShift { case Arg(Some(name), Some(file)) => println(s"$name $file") })
    sa.matchAndShift { case Arg(Some("f"), Some("argFilename")) => println("f argFilename") } shouldBe Args(List(Arg(None, Some("3.1415927"))))
    val z: Args[Double] = sa.matchAndShift { case Arg(Some("f"), Some("argFilename")) => }.mapMap(_.toDouble)
    z.matchAndShift { case Arg(None, Some(3.1415927)) => println("3.1415927") } shouldBe Args(List())
  }

  it should "do implement matchAndShiftOrElse" in {
    val sa: Args[String] = Args.make(Array("-f", "argFilename", "3.1415927"))
    sa.matchAndShiftOrElse { case Arg(None, None) => }(Args.empty) shouldBe Args.empty
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
    val say = Args.parseSimple(args)
    val sa = say.get
    sa.xas.length shouldBe 1
    sa.xas.head shouldBe Arg(Some(nameF), Some(argFilename))
  }

  it should "parseSimple 1 2 3" in {
    val say = Args.parseSimple(Array("1", "2", "3"))
    val sa = say.get
    sa.xas.length shouldBe 3
    sa.xas.head shouldBe Arg(None, Some("1"))
    val xa: Args[Int] = sa.mapMap(_.toInt)
    xa shouldBe Args(Seq(Arg(None, Some(1)), Arg(None, Some(2)), Arg(None, Some(3))))
    val processor = Map[String, Option[Int] => Unit]()
    xa.process(processor) should matchPattern { case Success(Seq(1, 2, 3)) => }
  }

  it should "parse 1 2 3" in {
    val say = Args.parse(Array("1", "2", "3"))
    say should matchPattern { case Success(_) => }
    val sa = say.get
    sa.xas.length shouldBe 3
    sa.xas.head shouldBe Arg(None, Some("1"))
    val xa: Args[Int] = sa.mapMap(_.toInt)
    xa shouldBe Args(Seq(Arg(None, Some(1)), Arg(None, Some(2)), Arg(None, Some(3))))
    val processor = Map[String, Option[Int] => Unit]()
    xa.process(processor) should matchPattern { case Success(Seq(1, 2, 3)) => }
  }

  it should """parse "-xf argFilename 3.1415927"""" in {
    val say = Args.parse(Array("-xf", "argFilename", "3.1415927"))
    say should matchPattern { case Success(_) => }
    val sa = say.get
    sa.xas.length shouldBe 3
    sa.xas.head shouldBe Arg(Some("x"), None)
    sa.xas.tail.head shouldBe Arg(Some("f"), Some("argFilename"))
    sa.xas.last shouldBe Arg(None, Some("3.1415927"))
  }

  it should """parse "-xf argFilename -p 3.1415927"""" in {
    val say = Args.parse(Array("-xf", "argFilename", "-p", "3.1415927"))
    say should matchPattern { case Success(_) => }
    val sa = say.get
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
    val sa = Args[String](Seq(Arg(Some("x"), None), Arg(Some("f"), Some("argFilename")), Arg(None, Some("3.1415927"))))
    val value: Option[String] = sa.getArgValue("f")
    value shouldBe Some("argFilename")
  }

  it should """implement getArgValueEitherOr("f")""" in {
    val sa = Args[String](Seq(Arg(Some("x"), None), Arg(Some("f"), Some("argFilename")), Arg(Some("d"), Some("3.1415927"))))
    sa.getArgValueEitherOr[Double]("x") shouldBe None
    sa.getArgValueEitherOr[Double]("f") shouldBe Some(Left("argFilename"))
    sa.getArgValueEitherOr[Double]("d") shouldBe Some(Right(3.1415927))
  }

  it should """implement getArgValueAs("f")""" in {
    val sa = Args[String](Seq(Arg(Some("x"), None), Arg(Some("n"), Some("1")), Arg(None, Some("3.1415927"))))
    val value: Option[Int] = sa.getArgValueAs[Int]("n")
    value shouldBe Some(1)
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

  it should "as" in {
    val say = Args.parse(Array("1", "2", "3"))
    say.isSuccess shouldBe true
    val ia: Args[Int] = say.get.as
    ia.operands shouldBe Seq(1, 2, 3)
  }

  //  it should """support for-comprehension for complex Args""" in {
  //    val sa = Args[String](Seq(Arg(Some("x"), None), Arg(Some("f"), Some("argFilename")), Arg(None, Some("3.1415927"))))
  //    for {
  //      x <-
  //    }
  //    var x = false
  //    var filename = ""
  //    val processor = Map[String, Option[String] => Unit]("x" -> { _ => x = true }, "f" -> { case Some(w) => filename = w; case _ => })
  //    sa.process(processor) should matchPattern { case Success(Seq("3.1415927")) => }
  //    x shouldBe true
  //    filename shouldBe "argFilename"
  //  }

  behavior of "Args validation"
  it should "parse " + cmdF + " " + argFilename in {
    val args = Array(cmdF, argFilename, "positionalArg")
    val asy = Args.parse(args, Some(cmdF + " " + "filename"))
    asy should matchPattern { case Success(Args(_)) => }
  }

  it should "parse " + cmdF + argFilename + " where filename is optional (1)" in {
    val args = Array(cmdF, argFilename, "3.1415927")
    val asy = Args.parse(args, Some(cmdF + "[ filename" + "]"))
    asy should matchPattern { case Success(Args(_)) => }
  }

  it should "parse " + cmdF + argFilename + " where filename is optional (2)" in {
    val args = Array(cmdF + argFilename, "positionalArg")
    val asy = Args.parse(args, Some(cmdF + "[ filename" + "]"))
    asy should matchPattern { case Success(Args(_)) => }
  }

  it should "parse " + cmdF + " " + argFilename + "where -xf filename required" in {
    a[ValidationException[String]] shouldBe thrownBy(Args.parse(Array(cmdF, argFilename), Some("-xf filename")).get)
  }

  it should """implement validate(String)""" in {
    val say = Args.parse(Array("-xf", "argFilename", "-p", "3.1415927"), optionalProgramName = Some("unit test"))
    say should matchPattern { case Success(_) => }
    val sa = say.get
    say.get.validate("-x[f filename][p number]") shouldBe Success(sa)
    say.get.validate("-xf filename -p number") shouldBe Success(sa)
  }

  it should "validate -x[f[ filename]] as a synopsis for -xfargFilename" in {
    Args.parse(Array("-xfargFilename"), Some("-x[f[ filename]]")) shouldBe Success(Args.create(Arg("x"), Arg("f", "argFilename")))
  }

}

case class NotImplemented(str: String) extends Exception(s"Not Implemented for $str")
