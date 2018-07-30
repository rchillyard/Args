/*
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.args

import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Success, Try}

class ParserSpec extends FlatSpec with Matchers {

  private val argFilename = "argFilename"
  private val nameF = "f"
  private val cmdF = "-" + nameF

  private def printFilename(so: Option[String]): Unit = so.foreach(s => println(s"$argFilename: $s"))

  val processor: Map[String, Option[String] => Unit] = Map[String, Option[String] => Unit](nameF -> printFilename)

  behavior of "SimpleArgParser"

  val p = new SimpleArgParser

  it should "parse flag " + cmdF in {
    p.parseAll(p.flag,cmdF) should matchPattern { case p.Success(p.Flag(`nameF`), _) => }
  }

  it should "not parse flag -X" in {
    p.parseAll(p.flag,"-X") should matchPattern { case p.Failure(_, _) => }
  }

  it should "parse argument " + argFilename in {
    p.parseAll(p.argument,argFilename) should matchPattern { case p.Success(p.Argument(`argFilename`), _) => }
  }

  it should "not parse argument -x" in {
    p.parseAll(p.argument,"-x") should matchPattern { case p.Failure(_, _) => }
  }

  it should "parse token " + cmdF in {
    p.parseAll(p.token,cmdF) should matchPattern { case p.Success(p.Flag(`nameF`), _) => }
  }

  it should "parse token " + argFilename in {
    p.parseAll(p.token,argFilename) should matchPattern { case p.Success(p.Argument(`argFilename`), _) => }
  }

  it should "parse " + cmdF in {
    p.parseToken(cmdF) should matchPattern { case Success(p.Flag(`nameF`)) => }
  }

  it should "parse " + argFilename in {
    p.parseToken(argFilename) should matchPattern { case Success(p.Argument(`argFilename`)) => }
  }

  behavior of "Parser"

  it should "parse options from -xf;" in {
    val p = new Parser
    val pp: p.ParseResult[PosixArg] = p.parseAll(p.posixOptions, "-xf;")
    pp should matchPattern { case p.Success(_, _) => }
  }

  it should "parse operand from 3.1415927;" in {
    val p = new Parser
    val pp: p.ParseResult[PosixArg] = p.parseAll(p.posixOperand, "3.1415927;")
    pp should matchPattern { case p.Success(_, _) => }
  }

  it should "parse option value from -argFilename;" in {
    val p = new Parser
    val pp: p.ParseResult[PosixArg] = p.parseAll(p.posixOptionValue, "argFilename;")
    pp should matchPattern { case p.Success(_, _) => }
  }

  it should "parse option set from -xf;argFilename" in {
    val p = new Parser
    val pp: p.ParseResult[Seq[PosixArg]] = p.parseAll(p.posixOptionSet, "-xf;argFilename;")
    pp should matchPattern { case p.Success(_, _) => }
    pp.get.size shouldBe 2
    pp.get.head shouldBe PosixOptionString("xf")
    pp.get.last shouldBe PosixOptionValue("argFilename")
  }

  it should """parseCommandLine "-xf argFilename 3.1415927"""" in {
    val p = new Parser
    val as: Seq[PosixArg] = p.parseCommandLine(Seq("-xf", "argFilename", "3.1415927"))
    as.size shouldBe 3
    as.head shouldBe PosixOptionString("xf")
    as.tail.head shouldBe PosixOptionValue("argFilename")
    as.last shouldBe PosixOperand("3.1415927")
  }

  behavior of "SynopsisParser"
  it should "parse x as a valueToken1" in {
    val p = new SynopsisParser
    val xyz = "xyz"
    val cr = p.parse(p.valueToken1, xyz)
    cr should matchPattern { case p.Success(`xyz`, _) => }
  }
  it should "parse x as a valueToken2" in {
    val p = new SynopsisParser
    val xyz = "XYZ"
    val cr = p.parse(p.valueToken2, xyz)
    cr should matchPattern { case p.Success(`xyz`, _) => }
  }
  it should "parse x as OptionToken" in {
    val p = new SynopsisParser
    val cr = p.parse(p.flag, "x")
    cr should matchPattern { case p.Success(Flag("x"), _) => }
  }
  it should "parse xf as two optionTokens" in {
    val p = new SynopsisParser
    val eEr = p.parse(p.flag ~ p.flag, "xf")
    eEr should matchPattern { case p.Success(_, _) => }
  }
  it should """parse "filename" as ValueToken1""" in {
    val p = new SynopsisParser
    val wr = p.parse(p.valueToken1, "filename")
    wr should matchPattern { case p.Success("filename", _) => }
  }
  it should "parse Junk as ValueToken2" in {
    val p = new SynopsisParser
    val wr = p.parse(p.valueToken2, "Junk")
    wr should matchPattern { case p.Success("Junk", _) => }
  }
  it should "parse Junk as ValueToken" in {
    val p = new SynopsisParser
    val vr = p.parse(p.value, "Junk")
    vr should matchPattern { case p.Success(Value("Junk"), _) => }
  }
  it should """parse " filename" as ValueToken""" in {
    val p = new SynopsisParser
    val vr = p.parse(p.value, " filename")
    vr should matchPattern { case p.Success(Value("filename"), _) => }
  }
  it should "parse x as an optionOrValueToken" in {
    val p = new SynopsisParser
    val er = p.parse(p.flagWithOrWithoutValue, "x")
    er should matchPattern { case p.Success(_, _) => }
  }
  it should "parse xf as two optionOrValueTokens" in {
    val p = new SynopsisParser
    val eEr = p.parse(p.flagWithOrWithoutValue ~ p.flagWithOrWithoutValue, "xf")
    eEr should matchPattern { case p.Success(_, _) => }
  }
  it should "parse [f] as an optionalElement" in {
    val p = new SynopsisParser
    val er = p.parse(p.optionalElement, "[f]")
    er should matchPattern { case p.Success(_, _) => }
  }
  it should "parse x[f] as one optionOrValueToken with three characters left over" in {
    val p = new SynopsisParser
    val er = p.parse(p.flagWithOrWithoutValue, "x[f]")
    er should matchPattern { case p.Success(Flag("x"), _) => }
    er.next.pos.column should matchPattern { case 2 => }
  }
  it should "parse x[f] as one optionOrValueToken followed by an optionalElement" in {
    val p = new SynopsisParser
    val eEr = p.parse(p.flagWithOrWithoutValue ~ p.optionalElement, "x[f]")
    eEr should matchPattern { case p.Success(_, _) => }
  }
  it should "parse x[f filename] as one optionOrValueToken followed by an optionalElement" in {
    val p = new SynopsisParser
    val eEr = p.parse(p.flagWithOrWithoutValue ~ p.optionalElement, "x[f filename]")
    eEr should matchPattern { case p.Success(_, _) => }
  }
  it should "parse f[ filename] as an optionalOrRequiredElement" in {
    val p = new SynopsisParser
    val er = p.parse(p.optionalOrRequiredElement, "f[ filename]")
    er should matchPattern { case p.Success(_, _) => }
    er.get shouldBe FlagWithValue("f", OptionalElement(Value("filename")))
  }
  it should "parse f filename as a optionalOrRequiredElement" in {
    val p = new SynopsisParser
    val esr = p.parse(p.optionalOrRequiredElement, "f filename")
    esr should matchPattern { case p.Success(_, _) => }
  }
  it should "parse [xf filename] as a List[Element]" in {
    val p = new SynopsisParser
    val esr = p.parse(p.optionalElements, "[xf filename]")
    esr should matchPattern { case p.Success(_, _) => }
  }
  it should """parse "first" as operands""" in {
    val p = new SynopsisParser
    val vr = p.parse(p.operands, "first")
    vr should matchPattern { case p.Success(Seq(Operand("first")), _) => }
  }
  it should """parse "first second" as operands""" in {
    val p = new SynopsisParser
    val vr = p.parse(p.operands, "first second")
    vr should matchPattern { case p.Success(Seq(Operand("first"), Operand("second")), _) => }
  }
  it should """parse "first [second]" as operands""" in {
    val p = new SynopsisParser
    val vr = p.parse(p.phrase(p.operands), "first [second]")
    vr should matchPattern { case p.Success(Seq(Operand("first"), OptionalElement(Operand("second"))), _) => }
  }
  it should """parse "first" as operand""" in {
    val p = new SynopsisParser
    val vr = p.parse(p.operand, "first")
    vr should matchPattern { case p.Success(Operand("first"), _) => }
  }
  it should """parse "[first]" as optional operand""" in {
    val p = new SynopsisParser
    val vr = p.parse(p.optionalOperand, "[first]")
    vr should matchPattern { case p.Success(OptionalElement(Operand("first")), _) => }
  }
  it should "parse -x[f filename] as a flag group" in {
    val p = new SynopsisParser
    val esr = p.parse(p.flagGroup, "-x[f filename]")
    esr should matchPattern { case p.Success(_, _) => }
    esr.get shouldBe Seq(Flag("x"), OptionalElement(FlagWithValue("f", Value("filename"))))
  }

  behavior of "parseSynopsis"
  it should "parse -x[f filename] as a synopsis" in {
    val p = new SynopsisParser
    val s: Synopsis = p.parseSynopsis("-x[f filename]")
    s shouldBe Synopsis(Seq(Flag("x"), OptionalElement(FlagWithValue("f", Value("filename")))))
  }
  it should "parse -[xf filename]" in {
    val p = new SynopsisParser
    val s = p.parseSynopsis("-[xf filename]")
    s shouldBe Synopsis(Seq(OptionalElement(Flag("x")), OptionalElement(FlagWithValue("f", Value("filename")))))
  }
  it should "parse -x[f[ filename]]" in {
    val p = new SynopsisParser
    val s = p.parseSynopsis("-x[f[ filename]]")
    s shouldBe Synopsis(Seq(Flag("x"), OptionalElement(FlagWithValue("f", OptionalElement(Value("filename"))))))
  }
  it should "parse -x[f[ filename]] first" in {
    val p = new SynopsisParser
    val s = p.parseSynopsis("-x[f[ filename]] first")
    s shouldBe Synopsis(List(Flag("x"), OptionalElement(FlagWithValue("f", OptionalElement(Value("filename")))), Operand("first")))
  }

  it should "parse -x[f[ filename]] first [second]" in {
    val p = new SynopsisParser
    val s = p.parseSynopsis("-x[f[ filename]] first [second]")
    s shouldBe Synopsis(List(Flag("x"), OptionalElement(FlagWithValue("f", OptionalElement(Value("filename")))), Operand("first"), OptionalElement(Operand("second"))))
  }

  behavior of "parseOptionalSynopsis"
  it should "parse -x[f filename] as a synopsis" in {
    val p = new SynopsisParser
    val so: Try[Synopsis] = p.parseOptionalSynopsis(Some("-x[f filename]"))
    so shouldBe Success(Synopsis(Seq(Flag("x"), OptionalElement(FlagWithValue("f", Value("filename"))))))
  }

}
