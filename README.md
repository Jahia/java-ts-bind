# TypeScript binding generator for Java
java-ts-bind takes your Java source code and generates TypeScript types for it.
It is meant to be used with [GraalJS](https://github.com/oracle/graaljs)
to provide a strongly-typed scripting environment.

This project was created for [CraftJS](https://github.com/Valtakausi/craftjs),
a Bukkit plugin for writing plugins in JavaScript. It is based on earlier work
by [Ap3teus](https://github.com/Ap3teus).

No releases are currently provided. If you need it, compile it yourself
(or open a bug in the issue tracker).

## Usage
This is a command-line application.

* --format: output format
  * Currently only TS_TYPES is supported
* --in: input directory or source jar
* --symbols: symbol sources (compiled jars)
* --repo: Maven repo to fetch the source jar from
* --artifact: Artifact to fetch from given repo
  * tld.domain:artifact:version (Gradle-style)
* --offset: path offset inside the input
  * Mainly used for Java core types; see .github/workflows for an example
* --include: prefixes for included paths
  * By default, everything is included
* --exclude: prefixes for excluded paths
  * Processed after includes; nothing is excluded by default
* --blacklist: blacklisted type regular expression patterns
  * Types that have names which equals any of these are omitted
  * Methods and fields that would use them are also omitted!
* --packageJson: read these options from a JSON file
  * The options should be placed under `tsbindOptions` object
  * Names of options lack -- prefixes but are otherwise same
  * Handy when you already have package.json for publishing
* --index: generate index.d.ts that references other generated files
* --emitReadOnly : if set, deactivates constructors and setter in the generated types
* --excludeMethods : a list of regular expressions that will be used to exclude methods by name
* --groupByModule : if set, the generated types will be grouped by module name, if not set the output will instead be grouped by domain
* --gettersAndSettersOff : if false, Typescript getters and setters will be used to group methods into setters and getters. If true this mechanism is disabled and no intelligence will be performed to regroup getter and setter methods.
* --methodWhitelist : a list of methods using regular expressions that if they match they will be the only methods retained in the generated types
* --flattenTypes : if set the generated types will be flattened, which might that all the inherited methods will be included in the generated types and inheritance will be removed. This makes it possible to reduce the number of types for APIs
* --forceParentJavadocs : if set it will always copy javadocs if they exist on parent types and don't exist locally.
* --debugMatching: if set it will output some useful debug information about the black/white listing mechanism

## Limitations
java-ts-bind does not necessarily generate *valid* TypeScript declarations.
The results are good enough to allow strongly-typed scripts, but it is
recommended that `noLibCheck` is used.

Please also note that java-ts-bind provides *only the types*. Implementing
a module loading system for importing them is left as an exercise for the
reader. For pointers, see [CraftJS](https://github.com/Valtakausi/craftjs)
which (at time of writing) implements a CommonJS module loader with
Java and TypeScript.
