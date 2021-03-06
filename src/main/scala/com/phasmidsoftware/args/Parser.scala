/*
 * Copyright (c) 2018. Phasmid Software
 */

package com.phasmidsoftware.args

import scala.util._
import scala.util.parsing.combinator.RegexParsers

/**
  * Parser of POSIX-style command lines.
  */
class Parser extends RegexParsers {

  def parseCommandLine(ws: Seq[String]): Seq[PosixArg] = parseAll(posixCommandLine, ws.mkString("", terminator, terminator)) match {
    case Success(t, _) => t
    case _ => throw ParseException(s"could not parse '$ws' as a token")
  }

  /**
    * NOTE that it is impossible to tell whether the first arg after an option set is an option value or the first operand.
    * Only validating it with a command line synopsis can do that for sure.
    *
    * @return
    */
  def posixCommandLine: Parser[Seq[PosixArg]] = rep(posixOptionSet) ~ rep(posixOperand) ^^ { case pss ~ ps => pss.flatten ++ ps }

  def posixOptionSet: Parser[Seq[PosixArg]] = posixOptions ~ opt(posixOptionValue) ^^ { case p ~ po => p +: po.toSeq }

  def posixOptions: Parser[PosixArg] = "-" ~> """[a-zA-Z0-9]+""".r <~ terminator ^^ (s => PosixOptionString(s))

  def posixOptionValue: Parser[PosixArg] = nonOption ^^ (s => PosixOptionValue(s))

  def posixOperand: Parser[PosixArg] = nonOption ^^ (s => PosixOperand(s))

  def nonOption: Parser[String] = """[^;]+""".r <~ terminator

  val terminator = ";"
}

/**
  * a Posix Arg
  */
trait PosixArg {
  def value: String
}

/**
  * One or more options.
  * Each option is a single-letter, although the terminating
  * characters can be a value.
  *
  * @param value the string of options, without the "-" prefix.
  */
case class PosixOptionString(value: String) extends PosixArg

/**
  * The value of the preceding option.
  *
  * @param value a String
  */
case class PosixOptionValue(value: String) extends PosixArg

/**
  * The value of an operand, i.e. a String which follows all of the options and their values.
  *
  * @param value a String
  */
case class PosixOperand(value: String) extends PosixArg

/**
  * This represents an element in the synopsis for a command line
  */
trait Element extends Ordered[Element] {
  def value: String

  def isOptional: Boolean = false

  def compare(that: Element): Int = value compare that.value
}

case class Synopsis(es: Seq[Element]) {
  def getElement(w: String): Option[Element] = find(Some(w))

  def find(wo: Option[String]): Option[Element] = wo match {
    case Some(w) => es.find(e => e.value == w)
    case _ => None
  }

  def mandatoryAndOptionalElements: (Seq[Element], Seq[Element]) = es partition (!_.isOptional)
}

/**
  * This represents an "Option" in the parlance of POSIX flag line interpretation (but formerly these options were known as flags)
  *
  * @param value the (single-character) String representing the option (flag)
  */
case class Flag(value: String) extends Element

/**
  * This represents an Option Value in the parlance of POSIX.
  *
  * @param value the String
  */
case class Value(value: String) extends Element

/**
  * This represents an "Option" and its "Value"
  *
  * @param value   the flag or "option" String
  * @param element the Element which corresponds to the "value" of this synopsis flag (and which may of course be OptionalElement).
  */
case class FlagWithValue(value: String, element: Element) extends Element {
  override def equals(obj: scala.Any): Boolean = obj match {
    case FlagWithValue(x, y) => value == x && element == y
    case _ => false
  }
}

/**
  * This represents an optional synopsis element, either an optional flag, or an optional value.
  *
  * @param element a synopsis element that is optional
  */
case class OptionalElement(element: Element) extends Element {
  def value: String = element.value

  override def isOptional: Boolean = true

  override def equals(obj: scala.Any): Boolean = obj match {
    case e: Element => e.isOptional && value == e.value
    case _ => false
  }
}

class SynopsisParser extends RegexParsers {
  def parseSynopsis(wo: Option[String]): Option[Synopsis] = wo match {
    case Some(w) => parseAll(synopsis, w) match {
      case Success(es, _) => Some(Synopsis(es))
      case _ => throw new Exception(s"could not parse '$w' as a synopsis")
    }
    case _ => None
  }

  override def skipWhitespace: Boolean = false

  /**
    * A "synopsis" of flag line options and their potential argument values.
    * It matches a dash ('-') followed by a list of optionalOrRequiredElement OR: an optional list of flagWithOrWithoutValue
    *
    * @return a Parser[Seq[Element]
    */
  def synopsis: Parser[Seq[Element]] = "-" ~> (optionalElements | rep(optionalOrRequiredElement))

  /**
    * An optionalOrRequiredElement matches EITHER: an optionalElement OR: a flagWithOrWithoutValue
    *
    * @return a Parser[Element] which is EITHER: a Parser[Flag] OR: a Parser[FlagWithValue] OR: a Parser[OptionalElement]
    */
  def optionalOrRequiredElement: Parser[Element] = optionalElement | flagWithOrWithoutValue

  /**
    * An optionalElement matches a '[' followed by a flagWithOrWithoutValue followed by a ']'
    *
    * @return a Parser[OptionalElement]
    */
  def optionalElement: Parser[Element] = openBracket ~> flagWithOrWithoutValue <~ closeBracket ^^ (t => OptionalElement(t))

  /**
    * An optionalElement matches a '[' followed by a flagWithOrWithoutValue followed by a ']'
    *
    * @return a Parser[Seq[Element]
    */
  def optionalElements: Parser[Seq[Element]] = openBracket ~> rep(flagWithOrWithoutValue) <~ closeBracket ^^ (ts => for (t <- ts) yield OptionalElement(t))

  /**
    * A flagWithOrWithoutValue matches EITHER: a flag (option); OR: a flag (option) followed by a value
    *
    * @return a Parser[Element] which is EITHER: a Parser[Flag] OR: a Parser[FlagWithValue]
    */
  def flagWithOrWithoutValue: Parser[Element] = (flag ~ optionalValue | flag ~ value | flag) ^^ {
    case o: Element => o
    case (o: Element) ~ (v: Element) => FlagWithValue(o.value, v)
    case _ => throw new Exception("")
  }

  /**
    * An optionalValue matches "[" "value" "]"
    *
    * @return a Parser[OptionalElement]
    */
  def optionalValue: Parser[Element] = openBracket ~> value <~ closeBracket ^^ { e => OptionalElement(e) }

  /**
    * A value matches EITHER: a space [which is ignored] followed by a valueToken1 OR: a valueToken2
    *
    * @return a Parser[Value]
    */
  def value: Parser[Value] = ("""\s""".r ~> valueToken1 | valueToken2) ^^ (t => Value(t))

  /**
    * A flag ("option") matches a single character which is either a letter or a digit
    *
    * @return a Parser[Flag]
    */
  def flag: Parser[Flag] = """[\p{Ll}\d]""".r ^^ (t => Flag(t))

  /**
    * A valueToken2 matches an uppercase letter followed by any number of non-space, non-bracket symbols
    *
    * @return a Parser[String]
    */
  val valueToken2: Parser[String] = """\p{Lu}[^\[\]\s]*""".r

  /**
    * A valueToken1 matches at least one non-space, non-bracket symbol
    *
    * @return a Parser[String]
    */
  val valueToken1: Parser[String] = """[^\[\]\s]+""".r

  private val openBracket = """\[""".r
  private val closeBracket = """\]""".r

}

/**
  * Parser of non-POSIX command lines
  */
class SimpleArgParser extends RegexParsers {
  def parseToken(s: String): Try[Token] = parseAll(token, s) match {
    case Success(t, _) => scala.util.Success(t)
    case _ => scala.util.Failure(new Exception(s"could not parse '$s' as a token"))
  }

  trait Token {
    def s: String
  }

  /**
    * Case class to represent a "flag", a.k.a. option.
    *
    * @param s the single-character string which is the name of this flag
    */
  case class Flag(s: String) extends Token

  case class Argument(s: String) extends Token

  def token: Parser[Token] = flag | argument

  /**
    * "flag" is the old name for an "option"
    *
    * @return a Parser[Flag]
    */
  def flag: Parser[Flag] = "-" ~> cmdR ^^ (s => Flag(s))

  def argument: Parser[Argument] = argR ^^ (s => Argument(s))

  private val cmdR = """[a-z]+""".r
  private val argR = """\w+""".r
}

