# Args

[![CircleCI](https://circleci.com/gh/rchillyard/Args.svg?style=svg)](https://circleci.com/gh/rchillyard/Args)

This library is to make it easy to use and verify command line arguments in Scala programs

## Introduction
*Args* provides a mechanism for parsing, validating, and processing a set of command line arguments.
The option parsing is achieved via a map of options to functions
The format for the arguments follows POSIX standards, which allow for a set of options followed by a set of arguments.
Either (or both) the sets of options/arguments may be empty.
By convention, options come first, and have names, while arguments are positional and follow the last option, if any.
An option may be required or optional;
An option may have a value or not.
Option's name may be a single lower-case letter, or an identifier.
Single-letter options may be grouped together.
More information about the syntax of command line arguments is to be found here: http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html

*Args* will always parse and process a set of command line options.
Validation is performed if the application programmer provides a syntax template (known as the *synopsis*).

There is additional support for non-POSIX styles of command line arguments,
but in this case, there is no means of validating the command line.

## Classes
The main class used is a case class: *Args[X]* which is defined thus:

    case class Args[X](xas: Seq[Arg[X]]) extends Iterable[Arg[X]] {
        // ...
    }

*Args* defines a sequence of *Arg[X]* elements.
The order follows the order as parsed, but the order of the options (not the positional arguments)
is immaterial.

The class *Arg[X]* is defined thus:

    case class Arg[X](name: Option[String], value: Option[X]) extends Ordered[Arg[X]] {
        // ...
    }
    
An *Arg* is a single command line argument and can have a name, a value, or both.
The positional arguments which come at the end of the command line have values but no name.
Optional arguments ("options") which come first have a name but may or may not have a value.

The underlying type *X* is the type of the (optional) value.
When an *Arg* results directly from parsing the command line, then *X* is always String.
But a map method is defined which allows an *Arg[X]* to be transformed into an *Arg[Y]*.
    
## Parsing and Processing
An example of parsing with validation is the following:

    val args = Array("-f", "argFilename", "positionalArg")
    val as: Args[String] = Args.parsePosix(args, Some("-f filename"))
    
This will create an *Args[String]* with two *Arg* elements: one corresponding to *f:filename* and one corresponding to *positionalArg*.
Note that, currently at least, there is no way to validate that the required number of positional arguments is present.

There is another form of parsePosix which takes only the args parameter.
In this case, there will be no validation.

    val args = Array("-f", "argFilename", "positionalArg")
    val as: Args[String] = Args.parsePosix(args)

There is another method for parsing which doesn't require POSIX-style but cannot be validated:

    val args = Array("-f", "argFilename", "positionalArg")
    val as: Args[String] = Args.parse(args)

Once parsed and validated, an Args object can be processed by invoking the process method with a
map of "name"->function pairs.
The signature of the process method is:

    def process(fm: Map[String, Option[X] => Unit]): Try[Seq[X]]
    
The args are processed as *side-effects* (!!) but any positional args are returned in the result.
If any exceptions are thrown by the functions, the result will be a *Failure* and not a *Success*.

Please see the *ArgsSpec* class for more examples of invoking the various methods available.
