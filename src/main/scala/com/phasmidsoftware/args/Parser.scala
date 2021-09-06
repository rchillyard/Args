/*
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.args

import com.phasmidsoftware.util.MonadOps._

import scala.util._
import scala.util.parsing.combinator.RegexParsers

/**
  * Parser of POSIX-style command lines.
  *
  * TODO there is a problem with testing equality of Elements (it's too liberal)
  *
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
  /**
    * Method to yield the value (name) of this Element
    *
    * @return the value/name
    */
  def value: String

  /**
    * Method to determine if this Element is optional.
    *
    * @return true if this Element is optional.
    */
  def isOptional: Boolean = false

  /**
    * Method to compare this Element with that Element.
    *
    * @param that the comparand.
    * @return the result of invoking value compare that.value
    */
  def compare(that: Element): Int = value compare that.value

  /**
    * Method to yield an optional String according as whether this Element is an operand.
    *
    * @return optionally the value of this Operand element (includes optional elements); None if this Element is not an operand.
    */
  def asOperand: Option[String] = this match {
    case Operand(x) => Some(x)
    case OptionalElement(Operand(x)) => Some(x)
    case _ => None
  }
}

case class Synopsis(es: Seq[Element]) {
  /**
    * Method to get an element by value (basically that's its name).
    *
    * TEST this method
    *
    * @param value the value to find
    * @return an Option[Element]
    */
  def getElement(value: String): Option[Element] = find(Some(value))

  /**
    * Method to find an element by its value (basically that's its name)
    *
    * @param wo an optional value string
    * @return an Option[Element]
    */
  def find(wo: Option[String]): Option[Element] = wo match {
    case Some(w) => es.find(e => e.value == w)
    case _ => None
  }

  /**
    * Method to distinguish between mandatory and optional elements.
    *
    * @return a tuple of Element sequences--first is the mandatory elements, second is the optional elements.
    */
  def mandatoryAndOptionalElements: (Seq[Element], Seq[Element]) = es partition (!_.isOptional)

  /**
    * Method to get the operands (the non-option parameters) as a sequence of Strings
    *
    * @return Seq[String]
    */
  def operands: Seq[String] = es.flatMap(e => e.asOperand)
}

/**
  * This represents an "Option" in the parlance of POSIX command line interpretation (but formerly these options were known as flags)
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
  * This represents an operand in the parlance of POSIX.
  *
  * @param value the String
  */
case class Operand(value: String) extends Element

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
  def parseSynopsis(w: String): Synopsis = parseAll(synopsis, w) match {
    case Success(es, _) => Synopsis(es)
      case _ => throw new Exception(s"could not parse '$w' as a synopsis")
  }

  def parseOptionalSynopsis(wo: Option[String]): Try[Synopsis] = liftTry(parseSynopsis)(liftOptionToTry(wo))

  override def skipWhitespace: Boolean = false

  /**
    * A "synopsis" of command-line options and their potential argument values.
    * It matches a dash ('-') followed by a list of optionalOrRequiredElement OR: an optional list of flagWithOrWithoutValue
    *
    * @return a Parser[Seq[Element]
    */
  def synopsis: Parser[Seq[Element]] = rep(flagGroup) ~ opt(operands) ^^ { case x ~ oo => x.flatten ++ oo.toSeq.flatten }

  def operands: Parser[Seq[Element]] = rep(opt(whiteSpace) ~> operand) ~ rep(opt(whiteSpace) ~> optionalOperand) ^^ { case x ~ y => x ++ y }

  def optionalOperand: Parser[Element] = openBracket ~> operand <~ closeBracket ^^ (e => OptionalElement(e))

  def operand: Parser[Element] = operandToken ^^ (o => Operand(o))

  /**
    * A "synopsis" of command-line options and their potential argument values.
    * It matches a dash ('-') followed by a list of optionalOrRequiredElement OR: an optional list of flagWithOrWithoutValue
    *
    * @return a Parser[Seq[Element]
    */
  def flagGroup: Parser[Seq[Element]] = "-" ~> (optionalElements | rep(optionalOrRequiredElement))

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
  //noinspection Annotator
  def flag: Parser[Flag] = """[\p{Ll}\d]""".r ^^ (t => Flag(t))

  /**
    * A valueToken2 matches an uppercase letter followed by any number of non-space, non-bracket symbols
    *
    * CONSIDER should not allow "-"
    *
    * @return a Parser[String]
    */
  //noinspection Annotator
  val valueToken2: Parser[String] = """\p{Lu}[^\[\]\s]*""".r

  /**
    * A valueToken1 matches at least one non-space, non-bracket symbol
    *
    * CONSIDER should not allow "-"
    *
    * @return a Parser[String]
    */
  val valueToken1: Parser[String] = """[^\[\]\s]+""".r

  /**
    * A operandToken matches at least one non-space, non-dash, non-bracket symbol
    *
    * @return a Parser[String]
    */
  val operandToken: Parser[String] = """[^-\[\]\s]+""".r

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
    * TEST
    *
    * @param s the single-character string which is the name of this flag
    */
  case class Flag(s: String) extends Token

  // TEST
  case class Argument(s: String) extends Token

  def token: Parser[Token] = flag | argument

  /**
    * "flag" is the old name for an "option"
    *
    * @return a Parser[Flag]
    */
  def flag: Parser[Flag] = "-" ~> cmdR ^^ (s => Flag(s))

  /**
    * An argument can be made up of alphabetic and numeric characters, including "." but not "-"
    *
    * TODO should make this more flexible.
    *
    * @return a Parser[Argument]
    */
  def argument: Parser[Argument] = argR ^^ (s => Argument(s))

  private val cmdR = """[a-z]+""".r
  private val argR = """[\w.]+""".r
}

