/**
 * Copyright (c) 2018 Phasmid Software, Project Args.
 */

package com.phasmidsoftware.args

import com.phasmidsoftware.util.MonadOps.*

import scala.util.*

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
  def validate(w: String): Try[Args[X]] =
    validate(new SynopsisParser().parseOptionalSynopsis(Some(w)))

  /**
    * Method to validate this Args according to the (optional) synopsis given as sy.
    *
    * CONSIDER using lift
    * CONSIDER using map/recover on sy
    *
    * @param sy the optional Synopsis, as produced by SynopsisParser.parseOptionalSynopsis.
    * @return this, wrapped in Success, provided that no synopsis was given, or the synopsis was given and
    *         parsed successfully and the result of calling validate(Synopsis) is true.
    *         If a synopsis string was given but could not be parsed (a Failure other than the
    *         NoSuchElementException used by parseOptionalSynopsis to signal "no synopsis given"),
    *         that Failure is propagated rather than being silently treated as "nothing to validate".
    */
  def validate(sy: Try[Synopsis]): Try[Args[X]] =
    sy match
      case Success(s) => if doValidation(s) then Success(this) else Failure(ValidationException(this, s))
      case Failure(_: NoSuchElementException) => Success(this)
      case Failure(e) => Failure(e)

  /**
    * Apply the given map(f) to each Arg of this Args
    *
    * @param f a function of type X => Y
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def mapMap[Y](f: X => Y): Args[Y] =
    Args(for xa <- xas yield xa.map(f))

  /**
    * Apply the given function f, using mapMap, to each Arg of this Args.
    *
    * @param f a function of type X => Option[Y]
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def mapOption[Y](f: X => Option[Y]): Args[Y] =
    Args(for xa <- xas yield xa.mapMap(f))

  /**
    * Apply the given function f to each Arg of this Args
    *
    * @param f a function of type Arg[X] => Arg[Y]
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def map[Y](f: Arg[X] => Arg[Y]): Args[Y] =
    Args(for xa <- xas yield f(xa))

  /**
    * Apply the given function f to each Arg of this Args
    *
    * @param f a function of type X => Args[Y]
    * @tparam Y the result type of the function f
    * @return an Args[Y] object
    */
  def flatMap[Y](f: Arg[X] => Args[Y]): Args[Y] =
    (for xa <- xas yield f(xa)).foldLeft(Args[Y](Seq()))((aa, a) => aa ++ a)

  /**
    * Convert this Args[X] to an Args[Y].
    * In practice, this method invokes map with the function deriveFrom invoked on the Derivable[Y] evidence.
    *
    * @tparam Y the underlying type of the result, such that there is evidence of a Derivable[Y] provided implicitly.
    * @return an Args[Y].
    */
  def as[Y: Derivable]: Args[Y] =
    mapOption[Y](summon[Derivable[Y]].deriveFromOpt[X](_))

  /**
    * Get the options (i.e. args with names) as map of names to (optional) values
    *
    * @return the options as a map
    */
  lazy val options: Map[String, Option[X]] =
    (for xa <- xas yield xa.asOption).flatten.toMap

  /**
    * Get the operands or positional arguments (i.e. args without names) as a sequence of X values.
    *
    * @return a sequence of X values.
    */
  lazy val operands: Seq[X] =
    (for xa <- xas yield xa.operand).flatten

  /**
    * Get the operands (positional arguments) as a map of String->X pairs.
    * This is achieved by matching up the names of operands from the synopsis (given) with the operands.
    *
    * @param s the synopsis from which we will derive the operand names.
    * @return a map of String->X pairs.
    */
  def operands(s: Synopsis): Map[String, X] =
    (s.operands zip operands).toMap

  /**
    * Method to get an Arg whose name matches the given string.
    *
    * @param w the string to match
    * @return Some(arg) if the name matches, else None
    */
  def getArg(w: String): Option[Arg[X]] =
    (for xa <- xas yield xa.byName(w)).flatten.toList match
      case xa :: Nil => Some(xa)
      case Nil => None
      case _ => throw AmbiguousNameException(w)

  /**
    * Get the arg value where the name matches the given string and where the resulting type is Y
    *
    * @param w the string to match
    * @tparam Y the result type
    * @return an option value of Y (None if toY yields a Failure)
    */
  def getArgValueAs[Y: Derivable](w: String): Option[Y] =
    getArg(w) flatMap (xa => xa.toY.toOption)

  /**
    * Get the arg value where the name matches the given string and where the resulting type is Y
    *
    * @param w the string to match
    * @return an option value of Y (None if there is no value).
    */
  def getArgValue(w: String): Option[X] =
    getArg(w) flatMap (xa => xa.value)

  /**
    * Get the arg value where the name matches the given string and where the resulting type is Y
    *
    * @param w the string to match
    * @return an option value of Y (None if there is no value).
    */
  def getArgValueEitherOr[Y: Derivable](w: String): Option[Either[X, Y]] =
    for xa <- getArg(w); xYea = xa.eitherOr[Y]; xYe <- xYea.value yield xYe

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
    sequence(for xa <- xas yield for x <- xa.process(fm) yield x) match
      case Success(xos) => Success(xos.flatten)
      case Failure(x) => Failure(x)

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
    * @return a Success of the remainder if f is defined for the head Arg;
    *         a Failure(NoMatchException) if f is not defined for the head Arg;
    *         a Failure(EmptyArgsException) if this Args is empty.
    */
  def matchAndShift(f: PartialFunction[Arg[X], Unit]): Try[Args[X]] = xas match
    case Nil =>
      Failure(EmptyArgsException)
    case xa :: tail =>
      if f.isDefinedAt(xa)
      then
        Try {
          f(xa)
          Args(tail)
        }
      else Failure(NoMatchException(xa.toString))

  /**
    * Method to process one Arg and return the remainder of the arguments as an Args.
    * In this form of the method, a failure to match by function f (including an empty Args) will
    * result in the default value being returned, rather than a failure.
    *
    * @param f       a partially-defined function which can process the arg.
    * @param default a call-by-name value which will be returned in the event that matchAndShift does not succeed.
    * @return if f is defined for the Arg, then return the remainder; otherwise return the result of invoking default.
    */
  def matchAndShiftOrElse(f: PartialFunction[Arg[X], Unit])(default: => Args[X]): Args[X] =
    matchAndShift(f).getOrElse(default)

  override def toString(): String = xas.mkString("; ")

  /**
    * Method to validate this Args according to the given Synopsis.
    *
    * Checks that the mandatory options declared by the synopsis are all present (safe: `Arg.compare`
    * is a total order, so sorting the mandatory Args by name can never throw), and that the number of
    * operands falls within the range of mandatory/optional operands declared by the synopsis.
    *
    * @param s the Synopsis.
    * @return true if all the Arg elements of this are compatible with the synopsis.
    */
  private def doValidation(s: Synopsis): Boolean =
    val (mandatoryElements, _) = s.mandatoryAndOptionalElements
    // mandatoryAndOptionalElements partitions *all* synopsis elements, including operands; only the
    // flag/option elements are relevant here, since operand arity is checked separately below.
    val m = mandatoryElements.filterNot(_.asOperand.isDefined)
    val (_, mandatory) = xas.filter(_.isOption).partition(_.isOptional(s).toBoolean(false))
    val optionsValid = if (m.size == mandatory.size) {
      val bs = for (z <- m.sorted zip mandatory.sorted; name <- z._2.name) yield (z._1.value compare name) == 0
      bs.forall(_ == true)
    }
    else
      false
    val (minOperands, maxOperands) = s.operandArity
    val operandsValid = operands.size >= minOperands && operands.size <= maxOperands
    optionsValid && operandsValid
}

object Args {
  def showArgs(args: Array[String]): String = args.mkString(" ")

  /**
    * Method to create an empty Args.
    *
    * @tparam T the underlying type of the result.
    * @return an Args[T].
    */
  def empty[T]: Args[T] = Args(Nil)

  /**
    * Method to create a singleton Args.
    *
    * @param ta an Arg[T].
    * @tparam T the underlying type of ta, and the result.
    * @return an Args[T].
    */
  def singleton[T](ta: Arg[T]): Args[T] = Args(Seq(ta))

  /**
    * Method to parse a set of command line arguments that don't necessarily conform to the POSIX standard,
    * and which cannot be validated.
    *
    * @param args the command line arguments.
    * @return the arguments parsed as an Args[String], wrapped in Try.
    */
  def parseSimple(args: Array[String]): Try[Args[String]] =
    val p = new SimpleArgParser

    @scala.annotation.tailrec
    def inner(r: Seq[Arg[String]], w: Seq[p.Token]): Seq[Arg[String]] = w match
      case Nil => r
      case p.Flag(c) :: p.Argument(a) :: t => inner(r :+ Arg(c, a), t)
      case p.Flag(c) :: t => inner(r :+ Arg(c), t)
      case p.Argument(a) :: t => inner(r :+ Arg(None, Some(a)), t)

    val tys = for (a <- args) yield p.parseToken(a)
    sequence(tys.toIndexedSeq) match
      case Success(ts_) => Success(Args(inner(Seq(), ts_)))
      case Failure(x) => Failure(x)

  /**
    * Method to parse a set of command line arguments that conform to the POSIX standard.
    *
    * @param args                the command line arguments.
    * @param synopsis            the (optional) syntax template which will be used, if not None, to validate the options.
    * @param optionalProgramName if optionalProgramName is defined,
    *                            the args array will be written to the Error Output, prefixed by the program name.
    * @param validate            if true (the default), the parsed result is validated against synopsis
    *                            (mandatory options and operand arity) before being returned; if false,
    *                            synopsis is still used to guide flag/value pairing during parsing, but
    *                            the caller is responsible for invoking validate explicitly afterwards.
    * @return the arguments parsed as an Args[String], wrapped in Try.
    */
  def parse(args: Array[String], synopsis: Option[String] = None, optionalProgramName: Option[String] = None, validate: Boolean = true): Try[Args[String]] =
    optionalProgramName.foreach(name => System.err.println(s"""$name: ${showArgs(args)}"""))
    doParse((new Parser).parseCommandLine(args.toIndexedSeq), synopsis, validate)

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
    * This method is only appropriate for unit testing.
    *
    * NOTE: we use get here on a Try. We might throw an exception, therefore.
    *
    * @param args an Array[String].
    * @return an Args[String]
    * @throws Exception the result of invoking parse.
    */
  def make(args: Array[String]): Args[String] = parse(args).get

  /**
    * Method to create an Args[String] from an IndexedSeq of Args.
    * This method is only appropriate for unit testing and is now deprecated.
    *
    * NOTE: we use get here on a Try. We might throw an exception, therefore.
    *
    * @param args an IndexedSeq[String].
    * @return an Args[String]
    * @throws Exception the result of invoking parse.
    */
  @deprecated
  def make(args: IndexedSeq[String]): Args[String] =
    parse(args.toArray[String]).get

  private def doParse(ps: => Seq[PosixArg], wo: Option[String] = None, validate: Boolean = true): Try[Args[String]] = {
    val sy = (new SynopsisParser).parseOptionalSynopsis(wo)

    // Method to unwrap an Element down to its "core" and determine whether it declares a
    // mandatory (non-optional) value component, i.e. one which must appear as a separate
    // following token (an optional value, by contrast, is only ever honored when fused into
    // the same token as its flag, e.g. "-fvalue" rather than "-f value").
    @scala.annotation.tailrec
    def hasMandatoryValue(e: Element): Boolean = e match
      case OptionalElement(x) => hasMandatoryValue(x)
      case FlagWithValue(_, OptionalElement(_)) => false
      case FlagWithValue(_, _) => true
      case _ => false

    // Method to expand a (possibly combined) option-string token, e.g. "xf", into individual
    // flag Args, one per character. Where the synopsis declares an optional value for a flag,
    // any remaining characters of the same token are consumed as that flag's fused value (per
    // POSIX, an optional value must be fused; a mandatory value never is, and is instead looked
    // for in the following raw token--see wantsSeparateValue below).
    def expandOptionString(w: String): Seq[Arg[String]] = sy match
      case Success(s) =>
        val cEm: Map[Char, Element] = prune(for (c <- w) yield c -> s.find(Some(c.toString)))

        @scala.annotation.tailrec
        def inner2(r: Seq[Arg[String]], cs: List[Char]): Seq[Arg[String]] = cs match {
          case Nil =>
            r
          case c :: tail =>
            cEm.get(c) match {
              case Some(e) =>
                @scala.annotation.tailrec
                def processElement(e: Element): Seq[Arg[String]] = e match
                  case OptionalElement(x) => processElement(x)
                  case FlagWithValue(_, OptionalElement(_)) => r :+ Arg(c.toString, tail.mkString(""))
                  case _ => inner2(r :+ Arg(c.toString), tail)

                processElement(e)
              case None =>
                throw NoOptionInSynopsisException(c.toString)
            }
        }

        inner2(Seq(), w.toList)
      case _ =>
        for c <- w yield Arg(c.toString)

    // Method to determine whether the trailing flag of an expanded option-string token (flags)
    // should consume the *next* raw token (from the overall command line, not the same token) as
    // its value. This is only true if that trailing flag doesn't already have a (fused) value, and:
    //   - there is no (usable) synopsis, in which case we fall back to the permissive legacy
    //     behavior of always pairing a bare flag with whatever token follows it; or
    //   - the synopsis declares a mandatory value component for that trailing flag.
    def wantsSeparateValue(w: String, flags: Seq[Arg[String]]): Boolean =
      flags.lastOption.exists(_.value.isEmpty) && (sy match
        case Success(s) => s.find(Some(w.last.toString)).exists(hasMandatoryValue)
        case _ => true
        )

    @scala.annotation.tailrec
    def loop(r: Seq[Arg[String]], w: Seq[PosixArg]): Seq[Arg[String]] = w match
      case Nil =>
        r
      case PosixOptionString(o) :: t =>
        val flags = expandOptionString(o)
        t match {
          case PosixOperand(v) :: rest if wantsSeparateValue(o, flags) =>
            loop(r ++ flags.init :+ flags.last.copy(value = Some(v)), rest)
          case _ =>
            loop(r ++ flags, t)
        }
      case PosixOperand(o) :: t =>
        loop(r :+ Arg(None, Some(o)), t)
      case _ =>
        throw ParseException(s"loop: failed to match $w")

    val ta = Try(Args(loop(Seq(), ps)))
    if (validate) ta flatMap (_ validate sy) else ta
  }
}
