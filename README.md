# Args

![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.phasmidsoftware/args_3/badge.svg?color=blue)
[![args Scala version support](https://index.scala-lang.org/rchillyard/args/args/latest-by-scala-version.svg)](https://index.scala-lang.org/rchillyard/args/args)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/78b1a73d5903494c87d950f9e2f7addf)](https://www.codacy.com/gh/rchillyard/Args/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=rchillyard/Args&amp;utm_campaign=Badge_Grade)
[![CI](https://github.com/rchillyard/Args/actions/workflows/ci.yml/badge.svg)](https://github.com/rchillyard/Args/actions/workflows/ci.yml)
![GitHub Top Languages](https://img.shields.io/github/languages/top/rchillyard/Args)
![GitHub](https://img.shields.io/github/license/rchillyard/Args)
![GitHub last commit](https://img.shields.io/github/last-commit/rchillyard/Args)
![GitHub issues](https://img.shields.io/github/issues-raw/rchillyard/Args)
![GitHub issues by-label](https://img.shields.io/github/issues/rchillyard/Args/bug)

*Args* is a Scala 3 library for parsing and validating command-line arguments.

Most CLI-argument libraries — [scopt](https://github.com/scopt/scopt), [decline](https://ben.kirw.in/decline/), [mainargs](https://github.com/com-lihaoyi/mainargs), Java's [picocli](https://picocli.info/) — make you *declare* your grammar in code: one builder call, annotation, or method parameter per option. *Args* takes a different approach: you write the grammar the way it's already documented in every `man` page, as a **POSIX synopsis string**, and *Args* parses your command line against it directly.

```scala
import com.phasmidsoftware.args.Args

val parsed = Args.parse(args, Some("-f filename operand"))
```

That one string says: `-f` is required and takes a value, and there's exactly one required operand. Get that wrong on the command line — wrong flag, missing value, wrong number of operands — and `parsed` is a `Failure` — no separate schema to keep in sync with a `--help` string, because the synopsis *is* the schema.

## Quick start

```scala
import com.phasmidsoftware.args.Args

val args = Array("-f", "input.txt", "report.csv")

Args.parse(args, Some("-f filename operand")) match {
  case scala.util.Success(parsed) =>
    val filename = parsed.getArgValue("f")   // Some("input.txt")
    val operand = parsed.operands.head       // "report.csv"
  case scala.util.Failure(exception) =>
    System.err.println(exception.getMessage)
}
```

Add `libraryDependencies += "com.phasmidsoftware" %% "args" % "2.0.0"` to your `build.sbt` (Scala 3 only — see [Versions](#versions)).

## Why Args

|                                   | Args | scopt | decline | mainargs | picocli |
|-----------------------------------|:----:|:-----:|:-------:|:--------:|:-------:|
| Grammar as a POSIX synopsis string|  ✅  |  ❌   |   ❌    |    ❌    |   ❌    |
| Pure functional core (`Try`/`Option`, no framework)|  ✅  |  ❌   |   ✅    |    ✅    |   ❌    |
| Java-callable                    |  ✅  |  ⚠️   |   ❌    |    ❌    |   ✅    |

If your grammar is simple and well-known (it's already in your `--help` text), *Args* lets you validate against it with one string instead of a builder chain. If you need subcommands, shell completion, or a big ecosystem, reach for decline or picocli instead — *Args* stays deliberately small.

## API Documentation

Full Scaladoc is available via [javadoc.io](https://javadoc.io/doc/com.phasmidsoftware/args_3).

## Introduction
*Args* provides a mechanism for parsing, validating, and processing a set of command line arguments.
Option processing is typically achieved via a map of options to functions.
The format for the arguments follows POSIX standards, which allow for a set of options followed by a set of operands (positional arguments).
Either (or both) the sets of options/arguments may be empty.
By convention, options come first, and have single-character names, while operands are positional and follow the last option, if any.
An option may be required or optional;
An option may be required to have a value or not.
Options may be grouped together in one argument, with only one "-" preceding them.
Options that take optional values may be part of a single *String* (without intervening space);
required arguments must be in separate arguments.
More information about the syntax of command line arguments is to be found here: http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html

*Args* will always parse and process a set of command line options.
Validation is performed if the application programmer provides a syntax template (known as the *synopsis*).
When a synopsis is given, both the mandatory/optional options *and* the number of operands are validated against it.

There is additional support for non-POSIX styles of command line arguments,
but in this case, there is no means of validating the command line.

Whether your application extends the *App* interface, or creates its own *main* program, the command-line arguments
will be available as Strings in an *Array\[String\]* usually called *args*. 


## Classes
The main class used is a case class: *Args\[X\]* which is defined thus:

    case class Args[X](xas: Seq[Arg[X]]) extends Iterable[Arg[X]\] {
        // ...
    }

*Args* defines a sequence of *Arg\[X\]* elements.
The order follows the order as parsed, but the order of the options (but not the operands)
is immaterial.

The class *Arg\[X\]* is defined thus:

    case class Arg[X](name: Option[String], value: Option[X]) extends Ordered[Arg[X]] {
        // ...
    }
    
An *Arg* is a single command line argument and can have a name, a value, or both.
The operands (positional arguments) which come at the end of the command line have values but no name.
Optional arguments ("options") which come first have a name but may or may not have a value.

The underlying type *X* is the type of the (optional) value.
When an *Arg* results directly from parsing the command line, then *X* is always *String*.
But a *map* method is defined which allows an *Arg\[X\]* to be transformed into an *Arg[Y\]*.

*Arg* is `Ordered`: two options compare by name (their values are not significant), and an
operand always sorts after an option, matching the POSIX convention that options precede operands.
This ordering is total — comparing two *Arg*s never throws.

## Parsing and Processing
An example of parsing with validation is the following:

    val args = Array("-f", "argFilename", "operand")
    val say: Try[Args[String]] = Args.parse(args, Some("-f filename operand"))
    
This will create an *Args\[String\]* with two *Arg* elements: one corresponding to *f:filename* and one corresponding to *operand*.
In this case, filename is a required argument to the f option, the f option is itself required, and exactly one operand is required.

There is another form of *parse* which takes only the args parameter.
In this case, there will be no validation.

    val args = Array("-f", "argFilename", "operand")
    val say: Try[Args[String]] = Args.parse(args)

There is another method for parsing which doesn't require POSIX-style and so cannot be validated:

    val args = Array("-f", "argFilename", "operand")
    val say: Try[Args[String]] = Args.parseSimple(args)

Once parsed and validated, an *Args* object can be processed by invoking the *process* method with a
map of "name"->function pairs.
The signature of the *process* method is:

    def process(fm: Map[String, Option[X] => Unit]): Try[Seq[X]]
    
The args are processed as *side-effects* (!!) but any operands are returned in the result.
If any exceptions are thrown by the functions, the result will be a *Failure* and not a *Success*.

An alternative to invoking *process* is to split the *Args* up into options and operands using the *options* and *operands* methods:

    def options: Map[String, Option[X]]
    def operands(s: Synopsis): Map[String, X]

The result of invoking the *options* method is a map of *String->Option\[X\]* pairs. Each String is the name of an option
(according to the synopsis) and the *Option\[X]* value is its actual value read from the command line (if there is an argument to the option).

The result of invoking the *operands* method is a map of *String->X* pairs. Each String is the name of an operand
(according to the synopsis) and the *X* value is its actual value read from the command line.

For non-posix-style parsing, there is an additional signature of *operands*:

    def operands: Seq[X]
    
This result is simply the list of the operands as given on the command line.

Please see the *ArgsSpec* class for more examples of invoking the various methods available.

## More on (posix) validation

The following strings are valid synopsis/command line pairs/tuples (separated by "<-->"):

    -f filename <--> -f README.md
    -f[ filename]  <--> -f README.md <--> -f
    -[f filename] <--> -f README.md <--> 
    -[f[ filename]] <--> -f README.md <--> -fREADME.md <--> -f <--> 
    -xf filename <--> -xf README.md
    -x operand1 [operand2] <--> -x 1 <--> <--> -x 1 2
    operand1 [operand2] <--> 1 <--> <--> 1 2
    
Square brackets make the option or its parameter optional.
Options can be combined in the synopsis (and in the command line) but, in the command line,
an option which is optional must be the last of any group.

**Known limitation:** an option's value must always be a mandatory (non-optional) value component
in the synopsis to be recognized when given as a *separate* command-line token (e.g. `-f README.md`).
An *optional* value (e.g. `-f[ filename]`) is only ever recognized when fused into the same token as
its flag (e.g. `-fREADME.md`); given as a separate token, it is instead treated as an operand.

Operand *counts* are validated too: a synopsis of `operand1 [operand2]` requires one or two
operands on the command line — zero or three will fail validation.

## Class Arg: method signatures

    def isOption: Boolean
    def hasValue: Boolean
    def isOptional(s: Synopsis): Maybe
    def byName(w: String): Option[Arg[X]]
    def map[Y](f: X => Y): Arg[Y]
    def flatMap[Y](f: X => Arg[Y]): Arg[Y]
    def mapMap[Y](f: X => Option[Y]): Arg[Y]
    def as[Y: Derivable]: Arg[Y]
    def toY[Y: Derivable]: Try[Y]
    def eitherOr[Y: Derivable]: Arg[Either[X, Y]]
    def process(fm: Map[String, Option[X] => Unit]): Try[Option[X]]
    def compare(that: Arg[X]): Int
    lazy val asOption: Option[(String, Option[X])]
    lazy val operand: Option[X]

## Object Arg: method signatures

    def apply(w: String): Arg[String]
    def apply(w: String, v: String): Arg[String]

## Class Args: method signatures

    def :+(xa: Arg[X]): Args[X]
    def +:(xa: Arg[X]): Args[X]
    def ++(xq: Args[X]): Args[X]
    def validate(w: String): Try[Args[X]]
    def validate(sy: Try[Synopsis]): Try[Args[X]]
    def mapMap[Y](f: X => Y): Args[Y]
    def mapOption[Y](f: X => Option[Y]): Args[Y]
    def map[Y](f: Arg[X] => Arg[Y]): Args[Y]
    def flatMap[Y](f: Arg[X] => Args[Y]): Args[Y]
    def as[Y: Derivable]: Args[Y]
    def operands(s: Synopsis): Map[String, X]
    def getArg(w: String): Option[Arg[X]]
    def getArgValueAs[Y: Derivable](w: String): Option[Y]
    def getArgValue(w: String): Option[X]
    def getArgValueEitherOr[Y: Derivable](w: String): Option[Either[X, Y]]
    def isDefined(w: String): Boolean
    def process(fm: Map[String, Option[X] => Unit]): Try[Seq[X]]
    def matchAndShift(f: PartialFunction[Arg[X], Unit]): Try[Args[X]]
    def matchAndShiftOrElse(f: PartialFunction[Arg[X], Unit])(default: => Args[X]): Args[X]
    lazy val options: Map[String, Option[X]]
    lazy val operands: Seq[X]

`matchAndShift` returns a `Try`: `Failure` if the head `Arg` doesn't match, or if `Args` is empty.
`matchAndShiftOrElse` never fails — it falls back to `default` in either of those cases.

Additionally, many methods of _Iterable\[Arg\[X]]_ are also included where it makes sense,
such as:

    def iterator: Iterator[Arg[X]]

## Object Args:

    def parseSimple(args: Array[String]): Try[Args[String]]
    def parse(args: Array[String], synopsis: Option[String] = None): Try[Args[String]]
    def create(args: Arg[String]*): Args[String]
    def make(args: Seq[String]): Args[String]

## Trait Derivable:

*Derivable\[T\]* is a type class which defines the following method:

    def deriveFromOpt[X](x: X): Option[T]

There are `given` instances of *Derivable[T]* defined for *String* parameters.
The currently supported \[T] types are _Boolean_, _Int_, _Double_, _File_, _URL_.

## Versions

### Version 2.0.0:
* Migrated to Scala 3 (dropped support for 2.10/2.11/2.12/2.13); CI moved from CircleCI to GitHub Actions;
* `Arg.compare` is now a safe total order (options before operands) — it no longer throws;
* `Args.matchAndShift` now returns `Try[Args[X]]` instead of throwing `MatchError`/`EmptyArgsException`; `matchAndShiftOrElse` correctly falls back to `default` on an empty `Args` (previously threw);
* Operand *counts* are now validated against the synopsis, not just the options;
* Removed deprecated, unused `mapMapOption` (on both `Arg` and `Args`) and `Derivable.deriveFrom`.

### Version V1.0.3:
* added deriveFromOpt method to _Derivable_; added more _Derivable_ objects;
* added concatenation methods for _Args_;
* added mapMap methods to _Arg_ and _Args_;
* a few other minor changes to name/signature;

Differences from V1.0.1: more usage of _Try_ (mostly internal).
