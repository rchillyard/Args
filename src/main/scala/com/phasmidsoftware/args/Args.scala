/*
 * Copyright (c) 2018. Phasmid Software
 */

package com.phasmidsoftware.args

import com.phasmidsoftware.util.MonadOps._
import com.phasmidsoftware.util.{Kleenean, Maybe}

import scala.util._

/**
  * Case class to represent an "option" in a flag line.
  * Such an option has an (optional) name which is a String;
  * and an (optional) value, which is of type X.
  *
  * @param name the optional name.
  * @param value the optional value.
  * @tparam X the underlying type of the value.
  */
case class Arg[X](name: Option[String], value: Option[X]) extends Ordered[Arg[X]] {

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
  def isOptional(s: Synopsis): Maybe = s.find(name) match {
    case Some(e) => Kleenean(e.isOptional)
    case _ => Kleenean()
  }

  /**
    * Method to get this Arg, if and only if its name matches the given String (w)
    * @param w the string to match.
    * @return either Some(this) or else None.
    */
  def byName(w: String): Option[Arg[X]] = name match {
    case Some(`w`) => Some(this)
    case _ => None
  }

  /**
    * Method to map this Arg into an Arg of underlying type Y
    * @param f a function to convert an X into a Y.
    * @tparam Y the underlying type of the result.
    * @return an Arg[Y]
    */
  def map[Y](f: X => Y): Arg[Y] = Arg(name, value map f)

  /**
    * Method to return this Arg as an optional tuple of a String and an optional X value, according to whether it's an "option".
    *
    * @return Some[(String, Option[X]) if name is not None otherwise None.
    */
  def asOption: Option[(String, Option[X])] = name match {
    case Some(w) => Some(w, value)
    case _ => None
  }

  /**
    * Method to return this Arg as an optional X value, according to whether it's an "operand".
    *
    * @return Some[X] if name is None otherwise None.
    */
  def operand: Option[X] = name match {
    case None => value
    case _ => None
  }

  /**
    * Method to get the value of this Arg as a Y.
    * @tparam Y the type of the result.
    * @return the result of deriving a Y value from the actual value of this Arg.
    * @throws NoValueException if the value of this Arg is None.
    */
  def toY[Y: Derivable]: Y = value match {
    case Some(x) => implicitly[Derivable[Y]].deriveFrom(x)
    case _ => throw NoValueException(name)
  }

  /**
    * Method to process this Arg, given a map of options and their corresponding functions.
    * @param fm a Map of String->function where function is of type Option[X]=>Unit
    * @return a Success[None] if there was a function defined for this Arg in the map fm AND if the function invocation was successful;
    *         otherwise a Failure[X]
    */
  def process(fm: Map[String, Option[X] => Unit]): Try[Option[X]] = {
    def processFuncMaybe(fo: Option[Option[X] => Unit]): Try[Option[X]] = fo match {
      case Some(f) => Try(f(value)).map(_ => None)
      case None => Failure(AnonymousNotFoundException)
    }

    def process(c: String): Try[Option[X]] = processFuncMaybe(fm.get(c)).recoverWith({ case AnonymousNotFoundException => Failure(NotFoundException(c)) })

    name match {
      case Some(c) => process(c)
      case None => Success(value)
    }
  }

  /**
    * Method to form a String from this Arg
    *
    * @return "Arg: flag name/anonymous with value: value/none"
    */
  override def toString: String = s"Arg: flag ${name.getOrElse("anonymous")} with value: ${value.getOrElse("none")}"

  /**
    * Method to compare this Arg with that.
    *
    * TEST this method.
    *
    * @param that the Arg to compare with.
    * @return the result of invoking x compare y where x and y are the values of this and that Args.
    */
  def compare(that: Arg[X]): Int = name match {
    case Some(x) => that.name match {
      case Some(y) => x compare y
      case None => throw CompareException(s"$this vs $that")
    }
    case None => throw CompareException(s"$this vs $that")
  }
}

object Arg {
  /**
    * Method to create an Arg with name given as w and no value.
    * @param w the name of the arg
    * @return a valueless Arg[String] with name w.
    */
  def apply(w: String): Arg[String] = Arg(Some(w), None)

  /**
    * Method to create an Arg with name given as w and value v.
    * @param w the name of the arg.
    * @param v the value of the arg.
    * @return v valueless Arg[String] with name w and value v.
    */
  def apply(w: String, v: String): Arg[String] = Arg(Some(w), Some(v))
}

case class Args[X](xas: Seq[Arg[X]]) extends Iterable[Arg[X]] {

  /**
    * Method to validate this Args according to the POSIX-style synopsis w, expressed as a String.
    *
    * @param w the synopsis
    * @return this Args, assuming that all is OK
    */
  def validate(w: String): Args[X] = validate(new SynopsisParser().parseSynopsis(Some(w)))

  /**
    * Method to validate this Args according to the (optional) synopsis given as so.
    * @param so the optional Synopsis.
    * @return this, provided that the so is not None and that the result of calling validate(Synopsis) is true.
    * @throws ValidationException if validation returns false.
    * @throws InvalidOptionException if any Arg cannot be found in the synopsis.
    */
  def validate(so: Option[Synopsis]): Args[X] = so match {
    case Some(s) => if (validate(s)) this else throw ValidationException(this, s)
    case _ => this
  }

  /**
    * Method to validate this Args according to the given Synopsis.
    *
    * @param s the Synopsis.
    * @return true if all the Arg elements of this are compatible with the synopsis.
    * @throws InvalidOptionException if this Arg cannot be found in the synopsis.
    */
  def validate(s: Synopsis): Boolean = {
    val (m, _) = s.mandatoryAndOptionalElements
    // NOTE: the following will throw an exception if any Arg is invalid
    val (_, mandatory) = xas.filter(_.isOption).partition(_.isOptional(s).toBoolean(false))
    if (m.size == mandatory.size) {
      val bs = for (z <- m.sorted zip mandatory.sorted) yield (z._1.value compare z._2.name.get) == 0
      bs.forall(_ == true)
    }
    else
      false
  }

  /**
    * Apply the given function f to each Arg of this Args
    *
    * @param f a function of type X => Y
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def map[Y](f: X => Y): Args[Y] = Args(for (xa <- xas) yield xa.map(f))

  /**
    * Convert this Args[X] to an Args[Y].
    * In practice, this method invokes map with the function deriveFrom invoked on the Derivable[Y] evidence.
    *
    * @tparam Y the underlying type of the result, such that there is evidence of a Derivable[Y] provided implicitly.
    * @return an Args[Y].
    */
  def as[Y: Derivable]: Args[Y] = map[Y](implicitly[Derivable[Y]].deriveFrom[X](_))

  /**
    * Get the options (i.e. args with names) as map of names to (optional) values
    *
    * @return the options as a map
    */
  def options: Map[String, Option[X]] = (for (xa <- xas) yield xa.asOption).flatten.toMap

  /**
    * Get the operands or positional arguments (i.e. args without names) as a sequence of X values.
    *
    * @return a sequence of X values.
    */
  def operands: Seq[X] = (for (xa <- xas) yield xa.operand).flatten

  /**
    * Method to get an Arg whose name matches the given string.
    *
    * @param w the string to match
    * @return Some(arg) if the name matches, else None
    */
  def getArg(w: String): Option[Arg[X]] = (for (xa <- xas) yield xa.byName(w)).flatten.toList match {
    case xa :: Nil => Some(xa)
    case Nil => None
    case _ => throw AmbiguousNameException(w)
  }

  /**
    * Get the arg value where the name matches the given string and where the resulting type is Y
    *
    * @param w the string to match
    * @tparam Y the result type
    * @return an option value of Y
    */
  def getArgValue[Y: Derivable](w: String): Option[Y] = getArg(w) map (xa => xa.toY)

  /**
    * Method to determine if the argument identified by w is defined.
    *
    * @param w the name of an argument (flag).
    * @return true if the argument is found by getArg
    */
  def isDefined(w: String): Boolean = getArg(w).isDefined

  /**
    * Process this Args according to the map fm of String->function.
    * @param fm a Map of String->function where function is of type Option[X]=>Unit
    * @return a Try[Seq[X] resulting from iteration through each Arg and processing it.
    */
  def process(fm: Map[String, Option[X] => Unit]): Try[Seq[X]] =
    sequence(for (xa <- xas) yield for (x <- xa.process(fm)) yield x) match {
      case Success(xos) => Success(xos.flatten)
      case Failure(x) => Failure(x)
    }

  def iterator: Iterator[Arg[X]] = xas.iterator

  /**
    * Method to process one Arg and return the remainder of the arguments as an Args.
    *
    * @param f a partially-defined function which can process the arg.
    * @return if f is defined for the Arg, then return the remainder; otherwise return this as is.
    * @throws MatchError         if the head Arg does not match function f.
    * @throws EmptyArgsException if this Args is empty.
    */
  def matchAndShift(f: PartialFunction[Arg[X], Unit]): Args[X] = matchAndShiftOrElse(f)(throw new MatchError("matchAndShift"))

  /**
    * Method to process one Arg and return the remainder of the arguments as an Args.
    * In this form of the method, a failure to match by function f will result in the default value being returned.
    *
    * @param f       a partially-defined function which can process the arg.
    * @param default a call-by-name value which will be returned in the event that function f is not defined for the actual Arg at the head of the list.
    * @return if f is defined for the Arg, then return the remainder; otherwise return the result of invoking default.
    * @throws EmptyArgsException if this Args is empty.
    */
  def matchAndShiftOrElse(f: PartialFunction[Arg[X], Unit])(default: => Args[X]): Args[X] = xas match {
    case xa :: tail => if (f.isDefinedAt(xa)) {
      f(xa); Args(tail)
    } else default
    case Nil => throw EmptyArgsException
  }
}

object Args {
  def empty[T]: Args[T] = Args(Nil)

  /**
    * Method to parse a set of command line arguments that don't necessarily conform to the POSIX standard,
    * and which cannot be validated.
    *
    * @param args the command line arguments.
    * @return the arguments parsed as an Args[String].
    */
  def parse(args: Array[String]): Args[String] = {
    val p = new SimpleArgParser

    @scala.annotation.tailrec
    def inner(r: Seq[Arg[String]], w: Seq[p.Token]): Seq[Arg[String]] = w match {
      case Nil => r
      case p.Flag(c) :: p.Argument(a) :: t => inner(r :+ Arg(c, a), t)
      case p.Flag(c) :: t => inner(r :+ Arg(c), t)
      case p.Argument(a) :: t => inner(r :+ Arg(None, Some(a)), t)
    }

    val tys = for (a <- args) yield p.parseToken(a)
    val ts = sequence(tys.toIndexedSeq) match {
      case Success(ts_) => ts_
      case Failure(x) => System.err.println(x.getLocalizedMessage); Seq[p.Token]()
    }
    Args(inner(Seq(), ts))
  }

  /**
    * Method to parse a set of command line arguments that conform to the POSIX standard.
    *
    * @param args     the command line arguments.
    * @param synopsis the (optional) syntax template which will be used, if not None, to validate the options.
    * @return the arguments parsed as an Args[String].
    */
  def parsePosix(args: Array[String], synopsis: Option[String] = None): Args[String] = doParse((new Parser).parseCommandLine(args.toIndexedSeq), synopsis)

  /**
    * Method to create an Args object from a variable number of Arg parameters.
    *
    * NOTE: this is normally used only for testing.
    *
    * @param args the command line arguments.
    * @return the arguments parsed as an Args[String].
    */
  def create(args: Arg[String]*): Args[String] = apply(args)

  /**
    * Method to create an empty Args[String]
    *
    * TEST this
    *
    * @return an empty Args[String]
    */
  def apply: Args[String] = parse(Seq[String]())

  /**
    * Method to create an Args[String] from the command line arguments in a main program (or a sub-class of App).
    *
    * TEST this
    *
    * @param args a Seq[String].
    * @return an Args[String]
    */
  def parse(args: Seq[String]): Args[String] = parse(args.toArray)

  private def doParse(ps: Seq[PosixArg], wo: Option[String] = None): Args[String] = {
    val so = (new SynopsisParser).parseSynopsis(wo)

    def processPosixArg(p: PosixArg): Seq[PosixArg] = p match {
      case PosixOptionString(w) =>
        so match {
          case Some(s) =>
            val cEm: Map[Char, Element] = prune(for (c <- w) yield c -> s.find(Some(c.toString)))

            def inner2(ws: Seq[PosixArg], cs: List[Char]): Seq[PosixArg] = cs match {
              case Nil => ws
              case c :: tail =>
                cEm.get(c) match {
                  case Some(e) =>
                    @scala.annotation.tailrec
                    def processElement(e: Element): Seq[PosixArg] = e match {
                      case OptionalElement(x) => processElement(x)
                      case FlagWithValue(_, OptionalElement(_)) => ws ++ Seq(PosixOptionString(c.toString), PosixOptionValue(tail.mkString("")))
                      case _ => inner2(ws :+ PosixOptionString(c.toString), tail)
                    }

                    processElement(e)
                  case _ => throw NoOptionInSynopsisException(c.toString)
                }
            }

            inner2(Seq(), w.toList)
          case _ =>
            for (c <- w) yield PosixOptionString(c.toString)
        }
      case x => Seq(x)
    }

    @scala.annotation.tailrec
    def inner(r: Seq[Arg[String]], w: Seq[PosixArg]): Seq[Arg[String]] = w match {
      case Nil => r
      case PosixOptionString(o) :: PosixOptionValue(v) :: t => inner(r :+ Arg(o, v), t)
      case PosixOptionString(o) :: t => inner(r :+ Arg(o), t)
      case PosixOperand(o) :: t => inner(r :+ Arg(None, Some(o)), t)
      // TODO figure out how to deal with this properly
      case PosixOptionValue(o) :: t => inner(r :+ Arg(None, Some(o)), t)
      case _ => throw ParseException(s"inner: failed to match $w")
    }

    val as = (for (p <- ps) yield processPosixArg(p)).flatten
    Args(inner(Seq(), as)).validate(so)
  }

}
