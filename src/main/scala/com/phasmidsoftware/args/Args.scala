/**
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.args

import com.phasmidsoftware.util.MonadOps._
import com.phasmidsoftware.util.{Kleenean, Maybe}

import scala.util._

/**
  * Case class to represent an "option" in a command line.
  * Such an option has an (optional) name which is a String;
  * and an (optional) value, which is of type X.
  *
  * @param name  the optional name.
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
    *
    * @param w the string to match.
    * @return either Some(this) or else None.
    */
  def byName(w: String): Option[Arg[X]] = name match {
    case Some(`w`) => Some(this)
    case _ => None
  }

  /**
    * Method to map this Arg into an Arg of underlying type Y
    *
    * @param f a function to convert an X into a Y.
    * @tparam Y the underlying type of the result.
    * @return an Arg[Y]
    */
  def map[Y](f: X => Y): Arg[Y] = Arg(name, value map f)

  /**
    * Method to map this Arg into an Arg of underlying type Y but where the function given is X => Option[Y]
    *
    * @param f a function to convert an X into an Option[Y].
    * @tparam Y the underlying type of the result.
    * @return an Arg[Y]
    */
  def mapMap[Y](f: X => Option[Y]): Arg[Y] = Arg(name, value flatMap f)

  /**
    * Method to map this Arg into an Arg of underlying type Y but where the function given is Option[X] => Option[Y]
    *
    * @param f a function to convert an Option[X] into an Option[Y].
    * @tparam Y the underlying type of the result.
    * @return an Arg[Y]
    */
  def mapMapOption[Y](f: Option[X] => Option[Y]): Arg[Y] = Arg(name, f(value))

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
  def as[Y: Derivable]: Arg[Y] = mapMap[Y](implicitly[Derivable[Y]].deriveFromOpt[X](_))

  /**
    * Convert this Arg[X] to an Arg of Either[Y].
    * In practice, this method invokes map with the function deriveFromOpt invoked on the Derivable[Y] evidence.
    *
    * @tparam Y the underlying type of the result, such that there is evidence of a Derivable[Y] provided implicitly.
    * @return an Arg of Either[Y]..
    */
  def eitherOr[Y: Derivable]: Arg[Either[X, Y]] = map[Option[Y]](implicitly[Derivable[Y]].deriveFromOpt(_)) match {
    case Arg(no, Some(Some(y))) => Arg(no, Some(Right(y)))
    case _ => Arg(name, value map (Left(_)))
  }

  /**
    * Method to return this Arg as an optional tuple of a String and an optional X value, according to whether it's an "option".
    *
    * @return Some[(String, Option[X]) if name is not None otherwise None.
    */
  lazy val asOption: Option[(String, Option[X])] = name match {
    case Some(w) => Some(w, value)
    case _ => None
  }

  /**
    * Method to return this Arg as an optional X value, according to whether it's an "operand".
    *
    * @return Some[X] if name is None otherwise None.
    */
  lazy val operand: Option[X] = name match {
    case None => value
    case _ => None
  }

  /**
    * Method to get the value of this Arg as a Y.
    *
    * @tparam Y the type of the result.
    * @return the result of deriving a Y value from the actual value of this Arg, wrapped in Try.
    */
  def toY[Y: Derivable]: Try[Y] = value match {
    case Some(x) => implicitly[Derivable[Y]].deriveFromOpt(x) match {
      case Some(y) => Success(y)
      case None => Failure(MapException("cannot map from X to Y"))
    }
    case _ => Failure(NoValueException(name))
  }

  /**
    * Method to process this Arg, given a map of options and their corresponding functions.
    *
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
  //
  //  def multiply(x: Int, y: Int): Int = x * y
  //  val identity: Int => Int = 1 * _
  //
  //  val string: Any => String = _.toString
  //
  //  val f = println _
  //
  //  val z = new (Int => String) { def apply(x: Int): String = x.toString }
  //
  //  f(string(identity(42)))
}

case class Args[X](xas: Seq[Arg[X]]) extends Iterable[Arg[X]] {

  /**
    * Method to append an Arg[X] to this Args[X].
    *
    * @param xa the Arg[X] to be appended.
    * @return a new Args[X].
    */
  def :+(xa: Arg[X]): Args[X] = Args(xas :+ xa)

  /**
    * Method to prepend an Arg[X] to this Args[X].
    *
    * @param xa the Arg[X] to be prepended.
    * @return a new Args[X].
    */
  def +:(xa: Arg[X]): Args[X] = Args(xa +: xas)

  /**
    * Method to concatenate this Args[X] with xq.
    *
    * @param xq an Args[X].
    * @return a new Args[X].
    */
  def ++(xq: Args[X]): Args[X] = Args(xas ++ xq.xas)

  /**
    * Method to validate this Args according to the POSIX-style synopsis w, expressed as a String.
    *
    * @param w the synopsis
    * @return this Args, assuming that all is OK
    */
  def validate(w: String): Try[Args[X]] = validate(new SynopsisParser().parseOptionalSynopsis(Some(w)))

  /**
    * Method to validate this Args according to the (optional) synopsis given as sy.
    *
    * CONSIDER using lift
    * CONSIDER using map/recover on sy
    *
    * @param sy the optional Synopsis.
    * @return this, wrapped in Success, provided that the sy is not a Failure and that the result of calling validate(Synopsis) is true.
    * @throws InvalidOptionException if any Arg cannot be found in the synopsis.
    */
  def validate(sy: Try[Synopsis]): Try[Args[X]] = sy match {
    case Success(s) => if (validate(s)) Success(this) else Failure(ValidationException(this, s))
    case _ => Success(this)
  }

  /**
    * Method to validate this Args according to the given Synopsis.
    *
    * CONSIDER returning Try[Boolean]
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
      val bs = for (z <- m.sorted zip mandatory.sorted; name <- z._2.name) yield (z._1.value compare name) == 0
      bs.forall(_ == true)
    }
    else
      false
  }

  /**
    * Apply the given map(f) to each Arg of this Args
    *
    * @param f a function of type X => Y
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def mapMap[Y](f: X => Y): Args[Y] =
    Args(for (xa <- xas) yield xa.map(f))

  /**
    * Apply the given function f, using mapMapOption, to each Arg of this Args.
    *
    * @param f a function of type Option[X] => Option[Y].
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def mapMapOption[Y](f: Option[X] => Option[Y]): Args[Y] =
    Args(for (xa <- xas) yield xa.mapMapOption(f))

  /**
    * Apply the given function f, using mapMap, to each Arg of this Args.
    *
    * @param f a function of type X => Option[Y]
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def mapOption[Y](f: X => Option[Y]): Args[Y] =
    Args(for (xa <- xas) yield xa.mapMap(f))

  /**
    * Apply the given function f to each Arg of this Args
    *
    * @param f a function of type Arg[X] => Arg[Y]
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def map[Y](f: Arg[X] => Arg[Y]): Args[Y] =
    Args(for (xa <- xas) yield f(xa))

  /**
    * Apply the given function f to each Arg of this Args
    *
    * @param f a function of type X => Args[Y]
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def flatMap[Y](f: Arg[X] => Args[Y]): Args[Y] =
    (for (xa <- xas) yield f(xa)).foldLeft(Args[Y](Seq()))((aa, a) => aa ++ a)

  /**
    * Convert this Args[X] to an Args[Y].
    * In practice, this method invokes map with the function deriveFrom invoked on the Derivable[Y] evidence.
    *
    * @tparam Y the underlying type of the result, such that there is evidence of a Derivable[Y] provided implicitly.
    * @return an Args[Y].
    */
  def as[Y: Derivable]: Args[Y] = mapOption[Y](implicitly[Derivable[Y]].deriveFromOpt[X](_))

  /**
    * Get the options (i.e. args with names) as map of names to (optional) values
    *
    * @return the options as a map
    */
  lazy val options: Map[String, Option[X]] = (for (xa <- xas) yield xa.asOption).flatten.toMap

  /**
    * Get the operands or positional arguments (i.e. args without names) as a sequence of X values.
    *
    * @return a sequence of X values.
    */
  lazy val operands: Seq[X] = (for (xa <- xas) yield xa.operand).flatten

  /**
    * Get the operands (positional arguments) as a map of String->X pairs.
    * This is achieved by matching up the names of operands from the synopsis (given) with the operands.
    *
    * @param s the synopsis from which we will derive the operand names.
    * @return a map of String->X pairs.
    */
  def operands(s: Synopsis): Map[String, X] = (s.operands zip operands).toMap

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
    * @return an option value of Y (None if toY yields a Failure)
    */
  def getArgValueAs[Y: Derivable](w: String): Option[Y] = getArg(w) flatMap (xa => xa.toY.toOption)

  /**
    * Get the arg value where the name matches the given string and where the resulting type is Y
    *
    * @param w the string to match
    * @return an option value of Y (None if there is no value).
    */
  def getArgValue(w: String): Option[X] = getArg(w) flatMap (xa => xa.value)

  /**
    * Get the arg value where the name matches the given string and where the resulting type is Y
    *
    * @param w the string to match
    * @return an option value of Y (None if there is no value).
    */
  def getArgValueEitherOr[Y: Derivable](w: String): Option[Either[X, Y]] = for (xa <- getArg(w); xYea = xa.eitherOr[Y]; xYe <- xYea.value) yield xYe

  /**
    * Method to determine if the argument identified by w is defined.
    *
    * @param w the name of an argument (flag).
    * @return true if the argument is found by getArg
    */
  def isDefined(w: String): Boolean = getArg(w).isDefined

  /**
    * Process this Args according to the map fm of String->function.
    *
    * @param fm a Map of String->function where function is of type Option[X]=>Unit
    * @return a Try[Seq[X] resulting from iteration through each Arg and processing it.
    */
  def process(fm: Map[String, Option[X] => Unit]): Try[Seq[X]] =
    sequence(for (xa <- xas) yield for (x <- xa.process(fm)) yield x) match {
      case Success(xos) => Success(xos.flatten)
      case Failure(x) => Failure(x)
    }

  //  /**
  //    * Process this Args according to the map fm of String->function.
  //    *
  //    * @param fm a Map of String->function where function is of type Option[X]=>Unit
  //    * @return a Try[Seq[X] resulting from iteration through each Arg and processing it.
  //    */
  //  def processAsync(fm: Map[String, Option[X] => Unit]): Future[Seq[X]] = {
  //    import scala.concurrent.ExecutionContext.Implicits.global
  //    val xoyfs: Seq[Future[Try[Option[X]]]] = for (xa <- xas) yield for (x <- Future(xa.process(fm))) yield x
  //    val xoysf: Future[Seq[Try[Option[X]]]] = Future.sequence(xoyfs)
  //    val xosyf: Future[Try[Seq[Option[X]]]] = for (xoys <- xoysf) yield sequence(xoys)
  //    xosyf
  //  }

  def iterator: Iterator[Arg[X]] = xas.iterator

  override def size: Int = xas.size

  override def head: Arg[X] = xas.head

  override def headOption: Option[Arg[X]] = xas.headOption

  override def last: Arg[X] = xas.last

  override def lastOption: Option[Arg[X]] = xas.lastOption

  override def filter(pred: Arg[X] => Boolean): Iterable[Arg[X]] = xas.filter(pred)

  override def filterNot(pred: Arg[X] => Boolean): Iterable[Arg[X]] = xas.filterNot(pred)

  override def foreach[U](f: Arg[X] => U): Unit = xas.foreach(f)

  override def forall(p: Arg[X] => Boolean): Boolean = xas.forall(p)

  override def exists(p: Arg[X] => Boolean): Boolean = xas.exists(p)

  override def count(p: Arg[X] => Boolean): Int = xas.count(p)

  override def find(p: Arg[X] => Boolean): Option[Arg[X]] = xas.find(p)

  override def isEmpty: Boolean = xas.isEmpty

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

  override def toString(): String = xas.mkString("; ")
}

object Args {
  def empty[T]: Args[T] = Args(Nil)

  /**
    * Method to parse a set of command line arguments that don't necessarily conform to the POSIX standard,
    * and which cannot be validated.
    *
    * @param args the command line arguments.
    * @return the arguments parsed as an Args[String], wrapped in Try.
    */
  def parseSimple(args: Array[String]): Try[Args[String]] = {
    val p = new SimpleArgParser

    @scala.annotation.tailrec
    def inner(r: Seq[Arg[String]], w: Seq[p.Token]): Seq[Arg[String]] = w match {
      case Nil => r
      case p.Flag(c) :: p.Argument(a) :: t => inner(r :+ Arg(c, a), t)
      case p.Flag(c) :: t => inner(r :+ Arg(c), t)
      case p.Argument(a) :: t => inner(r :+ Arg(None, Some(a)), t)
    }

    val tys = for (a <- args) yield p.parseToken(a)
    sequence(tys) match {
      case Success(ts_) => Success(Args(inner(Seq(), ts_)))
      case Failure(x) => Failure(x)
    }
  }

  /**
    * Method to parse a set of command line arguments that conform to the POSIX standard.
    *
    * @param args                the command line arguments.
    * @param synopsis            the (optional) syntax template which will be used, if not None, to validate the options.
    * @param optionalProgramName if optionalProgramName is defined,
    *                            the args array will be written to the Error Output, prefixed by the program name.
    * @return the arguments parsed as an Args[String], wrapped in Try.
    */
  def parse(args: Array[String], synopsis: Option[String] = None, optionalProgramName: Option[String] = None): Try[Args[String]] = {
    optionalProgramName.foreach(name => System.err.println(s"""$name: ${args.mkString(" ")}"""))
    doParse((new Parser).parseCommandLine(args), synopsis)
  }

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
    * Method to create an Args[String] from the command line arguments in a main program (or a sub-class of App).
    *
    * NOTE that we use get here on a Try. We might throw an exception, therefore.
    *
    * @param args a Seq[String].
    * @return an Args[String]
    * @throws Exception the result of invoking parse.
    */
  def make(args: Seq[String]): Args[String] = parse(args.toArray[String]).get

  private def doParse(ps: => Seq[PosixArg], wo: Option[String] = None): Try[Args[String]] = {
    val sy = (new SynopsisParser).parseOptionalSynopsis(wo)

    def processPosixArg(p: PosixArg): Seq[PosixArg] = p match {
      case PosixOptionString(w) =>
        sy match {
          case Success(s) =>
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

    lazy val as = (for (p <- ps) yield processPosixArg(p)).flatten
    Try(Args(inner(Seq(), as))) flatMap (_ validate sy)
  }
}
