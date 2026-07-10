/**
  * Copyright (c) 2018 Phasmid Software, Project Args.
  */

package com.phasmidsoftware.args

import com.phasmidsoftware.util.{Kleenean, Maybe}

import scala.util.*

/**
  * Case class to represent an "option" in a command line.
  * Such an option has an (optional) name which is a String;
  * and an (optional) value, which is of type X.
  *
  * @param name  the optional name.
  * @param value the optional value.
  * @tparam X the underlying type of the value.
  */
case class Arg[X](name: Option[String], value: Option[X]) extends Ordered[Arg[X]]:

  /**
    * Method to determine if this Arg is an option (also known as a "flag"), as opposed to an operand.
    *
    * @return true if name is not None
    */
  def isOption: Boolean = name.isDefined

  /**
    * Method to determine if this Arg has a value, thus either an option with value, or an operand.
    *
    * @return true if value is not None
    */
  def hasValue: Boolean = value.isDefined

  /**
    * Method to determine if this Arg is optional according to the synopsis provided.
    *
    * @param s the synopsis.
    * @return Kleenean(true if this Arg is optional.
    */
  def isOptional(s: Synopsis): Maybe = s.find(name) match
    case Some(e) => Kleenean(e.isOptional)
    case _ => Kleenean()

  /**
    * Method to get this Arg, if and only if its name matches the given String (w)
    *
    * @param w the string to match.
    * @return either Some(this) or else None.
    */
  def byName(w: String): Option[Arg[X]] = name match
    case Some(`w`) => Some(this)
    case _ => None

  /**
    * Method to map this Arg into an Arg of underlying type Y
    *
    * @param f a function to convert an X into a Y.
    * @tparam Y the underlying type of the result.
    * @return an Arg[Y]
    */
  def map[Y](f: X => Y): Arg[Y] =
    Arg(name, value map f)

  /**
    * Method to map this Arg into an Arg of underlying type Y but where the function given is X => Option[Y]
    *
    * @param f a function to convert an X into an Option[Y].
    * @tparam Y the underlying type of the result.
    * @return an Arg[Y]
    */
  def mapMap[Y](f: X => Option[Y]): Arg[Y] =
    Arg(name, value flatMap f)

  /**
    * Method to flatMap this Arg into an Arg of underlying type Y
    *
    * @param f a function to convert an X into a Y.
    * @tparam Y the underlying type of the result.
    * @return an Arg[Y]
    */
  def flatMap[Y](f: X => Arg[Y]): Arg[Y] = value.map(f) match {
    case Some(a) => a
    case _ => Arg[Y](name, None)
  }

  /**
    * Convert this Arg[X] to an Arg[Y].
    * In practice, this method invokes mapMap with the function deriveFromOpt invoked on the Derivable[Y] evidence.
    *
    * @tparam Y the underlying type of the result, such that there is evidence of a Derivable[Y] provided implicitly.
    * @return an Arg[Y].
    */
  def as[Y: Derivable]: Arg[Y] =
    mapMap[Y](summon[Derivable[Y]].deriveFromOpt[X](_))

  /**
    * Convert this Arg[X] to an Arg of Either[Y].
    * In practice, this method invokes map with the function deriveFromOpt invoked on the Derivable[Y] evidence.
    *
    * @tparam Y the underlying type of the result, such that there is evidence of a Derivable[Y] provided implicitly.
    * @return an Arg of Either[Y]..
    */
  def eitherOr[Y: Derivable]: Arg[Either[X, Y]] =
    map[Option[Y]](summon[Derivable[Y]].deriveFromOpt(_)) match
      case Arg(no, Some(Some(y))) => Arg(no, Some(Right(y)))
      case _ => Arg(name, value map (Left(_)))

  /**
    * Method to return this Arg as an optional tuple of a String and an optional X value, according to whether it's an "option".
    *
    * @return Some[(String, Option[X]) if name is not None otherwise None.
    */
  lazy val asOption: Option[(String, Option[X])] = name match
    case Some(w) => Some(w, value)
    case _ => None

  /**
    * Method to return this Arg as an optional X value, according to whether it's an "operand".
    *
    * @return Some[X] if name is None otherwise None.
    */
  lazy val operand: Option[X] = name match
    case None => value
    case _ => None

  /**
    * Method to get the value of this Arg as a Y.
    *
    * @tparam Y the type of the result.
    * @return the result of deriving a Y value from the actual value of this Arg, wrapped in Try.
    */
  def toY[Y: Derivable]: Try[Y] = value match
    case Some(x) =>
      summon[Derivable[Y]].deriveFromOpt(x) match
        case Some(y) => Success(y)
        case None => Failure(MapException("cannot map from X to Y"))
    case _ =>
      Failure(NoValueException(name))

  /**
    * Method to process this Arg, given a map of options and their corresponding functions.
    *
    * @param fm a Map of String->function where function is of type Option[X]=>Unit
    * @return a Success[None] if there was a function defined for this Arg in the map fm AND if the function invocation was successful;
    *         otherwise a Failure[X]
    */
  def process(fm: Map[String, Option[X] => Unit]): Try[Option[X]] =
    def processFuncMaybe(fo: Option[Option[X] => Unit]): Try[Option[X]] = fo match
      case Some(f) => Try(f(value)).map(_ => None)
      case None => Failure(AnonymousNotFoundException)

    def process(c: String): Try[Option[X]] =
      processFuncMaybe(fm.get(c)).recoverWith({ case AnonymousNotFoundException => Failure(NotFoundException(c)) })

    name match
      case Some(c) => process(c)
      case None => Success(value)

  /**
    * Method to form a String from this Arg
    *
    * @return "Arg: flag name/anonymous with value: value/none"
    */
  override def toString: String =
    s"Arg: flag ${name.getOrElse("anonymous")} with value: ${value.getOrElse("none")}"

  /**
    * Method to compare this Arg with that.
    *
    * This is a total order: two named Args (options) compare by name (their values are
    * intentionally not significant, since ordering exists only to match Args against a
    * Synopsis); an unnamed Arg (operand) always sorts after a named one, matching the POSIX
    * convention that options precede operands; two unnamed Args compare equal.
    *
    * @param that the Arg to compare with.
    * @return the result of invoking x compare y where x and y are the names of this and that Args.
    */
  def compare(that: Arg[X]): Int = (name, that.name) match
    case (Some(x), Some(y)) => x compare y
    case (Some(_), None) => -1
    case (None, Some(_)) => 1
    case (None, None) => 0

object Arg:
  /**
    * Method to create an Arg with name given as w and no value.
    *
    * @param w the name of the arg
    * @return a valueless Arg[String] with name w.
    */
  def apply(w: String): Arg[String] = Arg(Some(w), None)

  /**
    * Method to create an Arg with name given as w and value v.
    *
    * @param w the name of the arg.
    * @param v the value of the arg.
    * @return v valueless Arg[String] with name w and value v.
    */
  def apply(w: String, v: String): Arg[String] = Arg(Some(w), Some(v))
