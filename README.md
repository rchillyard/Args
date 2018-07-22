# Args
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

## Parsing and Processing
