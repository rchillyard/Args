# Args

[![CircleCI](https://circleci.com/gh/rchillyard/Args.svg?style=svg)](https://circleci.com/gh/rchillyard/Args)

This library is to make it easy to use and verify command line arguments in Scala programs

## Introduction
*Args* provides a mechanism for parsing, validating, and processing a set of command line arguments.
Option processing is typically achieved via a map of options to functions.
The format for the arguments follows POSIX standards, which allow for a set of options followed by a set of operands (positional arguments).
Either (or both) the sets of options/arguments may be empty.
By convention, options come first, and have single-character names, while operands are positional and follow the last option, if any.
An option may be required or optional;
An option may be required to have a value or not.
Options may be grouped together in one argument, with only one "-" preceding them.
Options that take optional values may be part of a single String (without intervening space);
required arguments must be in separate arguments.
More information about the syntax of command line arguments is to be found here: http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html

*Args* will always parse and process a set of command line options.
Validation is performed if the application programmer provides a syntax template (known as the *synopsis*).

There is additional support for non-POSIX styles of command line arguments,
but in this case, there is no means of validating the command line.

Whether your application extends the *App* interface, or creates its own *main* program, the command-line arguments
will be available as Strings in an *Array[String]* usually called *args*. 


## Classes
The main class used is a case class: *Args[X]* which is defined thus:

    case class Args[X](xas: Seq[Arg[X]]) extends Iterable[Arg[X]] {
        // ...
    }

*Args* defines a sequence of *Arg[X]* elements.
The order follows the order as parsed, but the order of the options (but not the operands)
is immaterial.

The class *Arg[X]* is defined thus:

    case class Arg[X](name: Option[String], value: Option[X]) extends Ordered[Arg[X]] {
        // ...
    }
    
An *Arg* is a single command line argument and can have a name, a value, or both.
The operands (positional arguments) which come at the end of the command line have values but no name.
Optional arguments ("options") which come first have a name but may or may not have a value.

The underlying type *X* is the type of the (optional) value.
When an *Arg* results directly from parsing the command line, then *X* is always String.
But a *map* method is defined which allows an *Arg[X]* to be transformed into an *Arg[Y]*.
    
## Parsing and Processing
An example of parsing with validation is the following:

    val args = Array("-f", "argFilename", "operand")
    val say: Try[Args[String]] = Args.parse(args, Some("-f filename"))
    
This will create an *Args[String]* with two *Arg* elements: one corresponding to *f:filename* and one corresponding to *operand*.
In this case, filename is a required argument to the f option, and the f option is itself required.
Note that, currently at least, there is no way to validate that the required number of operands is present.

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

The result of invoking the *options* method is a map of *String->Option[]]X* pairs. Each String is the name of an option
(according to the synopsis) and the *Option[X]* value is its actual value read from the command line (if there is an argument to the option).

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
    -xf filename <--> -xf README.md <--> -x -f README.md
    -x operand1 [operand2] <--> -x 1 <--> <--> -x 1 2
    operand1 [operand2] <--> 1 <--> <--> 1 2
    
Square brackets make the option or its parameter optional.
Options can be combined in the synopsis (and in the command line) but, in the command line,
an option which is optional must be the last of any group.

## Class Arg: method signatures

    def isOption: Boolean
    def hasValue: Boolean
    def isOptional(s: Synopsis): Maybe
    def byName(w: String): Option[Arg[X]]
    def map[Y](f: X => Y): Arg[Y]
    def asOption: Option[(String, Option[X])]
    def operand: Option[X]
    def toY[Y: Derivable]: Try[Y]
    def process(fm: Map[String, Option[X] => Unit]): Try[Option[X]]
    def process(c: String): Try[Option[X]]
    def compare(that: Arg[X]): Int

## Class Args: method signatures

    def validate(w: String): Args[X]
    def validate(sy: Try[Synopsis]): Args[X]
    def validate(s: Synopsis): Boolean
    def map[Y](f: X => Y): Args[Y]
    def options: Map[String, Option[X]]
    def operands: Seq[X]
    def operands(s: Synopsis): Map[String, X]
    def getArg(w: String): Option[Arg[X]]
    def getArgValue[Y: Derivable](w: String): Option[Y]
    def isDefined(w: String): Boolean
    def process(fm: Map[String, Option[X] => Unit]): Try[Seq[X]]
    def iterator: Iterator[Arg[X]]
