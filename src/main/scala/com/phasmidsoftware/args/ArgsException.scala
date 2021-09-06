/*
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.args

abstract class ArgsException(s: String) extends Exception(s"Args exception: $s")

case object AnonymousNotFoundException extends ArgsException("no anonymous arg found")

case object EmptyArgsException extends ArgsException("Args is empty")

case class NotFoundException(command: String) extends ArgsException(s"Arg: flag $command not found")

case class AmbiguousNameException(name: String) extends ArgsException(s"$name ambiguous")

case class ParseException(cause: String) extends ArgsException(cause)

case class NoValueException(name: Option[String]) extends ArgsException(s"Arg: flag ${name.getOrElse("anonymous")} has no value")

case class ValidationException[X](a: Args[X], s: Synopsis) extends ArgsException(s"Args: validation failed for $a with synopsis: $s")

case class InvalidOptionException[X](arg: Arg[X]) extends ArgsException(s"Arg ${arg.name} not valid")

case class CompareException(str: String) extends ArgsException(s"Arg compare exception: $str")

case class NoOptionInSynopsisException(str: String) extends ArgsException(s"parse error: option $str was not found in synopsis")
