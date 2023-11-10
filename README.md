# sn-vcpkg

<!--toc:start-->
- [sn-vcpkg](#sn-vcpkg)
  - [Vcpkg and native dependencies 101](#vcpkg-and-native-dependencies-101)
  - [Usage](#usage)
    - [Examples](#examples)
    - [Summary](#summary)
    - [SBT plugin](#sbt-plugin)
    - [Mill plugin](#mill-plugin)
    - [Scala Native integration](#scala-native-integration)
      - [SBT](#sbt)
      - [Mill](#mill)
    - [CLI](#cli)
      - [`bootstrap`](#bootstrap)
      - [`install`](#install)
      - [`clang` and `clang++`](#clang-and-clang)
      - [`scala-cli`](#scala-cli)
    - [Docker base image](#docker-base-image)
    - [Core](#core)
      - [VcpkgRootInit](#vcpkgrootinit)
      - [VcpkgNativeConfig](#vcpkgnativeconfig)
      - [VcpkgDependencies](#vcpkgdependencies)
      - [VcpkgConfigurator](#vcpkgconfigurator)
      - [PkgConfig](#pkgconfig)
<!--toc:end-->

Utilities and build tools to work with [vcpkg C/C++ dependency manager](https://vcpkg.io/en/index.html) 

If you are in a rant-reading mood, please refer to [motivation](/docs/motivation.md).

**If you want to update this documentation file, don't edit it directly - edit [docs/README.in.md](/docs/README.in.md) and run `sbt updateDocs`. It's annoying, but this document contains compiled snippets of code which I want to prevent from going out of date**

Usage TLDR: most likely you want to take a look at [SBT plugin](#sbt-plugin) or [Mill plugin](#mill-plugin)

## Vcpkg and native dependencies 101

- By native dependencies we mean C/C++ libraries
- Usually distributed as sources (but some distributions contain pre-built artifacts, like gtk)
- Usually built on user's machine
- Contain following artifact types:
  - Binary
      - dynamic libraries (needed at runtime and linktime)
      - static libraries (needed at linktime)
  - C/C++ headers (optional, needed for compile time)
  - pkg-config files `*.pc` (optional, provide a recipe for constructing the correct clang/gcc flags to use the dependency)
- vcpkg contains lots and lots of pre-made recipes for popular libraries, with aim to work on Linux, MacOS, and Windows
- vcpkg works as both a CLI installer (`vcpkg install cjson`) and as 
  manifest installer (`vcpkg install vcpkg.json`) with dependencies specified in a JSON file <sup>:)</sup>
- vcpkg can bootstrap itself from a cloned repo <sup>:)</sup>
- vcpkg can install libraries into a custom location (instead of system-wide) making caching and isolation easier <sup>:)</sup>


<sup>:)</sup> - **this project does it for you**

## Usage 

### Examples

- [Example in this repo](/example/build.sbt) - used as part of CI, uses libuv, cjson, zeromq and [sn-bindgen]
- [Roach library](https://github.com/indoorvivants/roach/blob/main/build.sbt#L19-L44) - uses the Scala Native integration, and integrates with [sn-bindgen]
- [SN Bindgen Examples](https://github.com/indoorvivants/sn-bindgen-examples) - lots of different libraries (lua, openssl, postgres, sqlite, libuv, libgit2, etc.) using Scala Native integration and [sn-bindgen]

### Summary

There are several modules of interest:

1. `core` - contains all the tool-agnostic logic for bootstrapping, invoking, and communicating with `vcpkg`.
   This module is the meat on the proverbial bones, the plugins and the CLI merely invoke it.

2. `cli` - contains a very barebones CLI wrapper over `core` with dubious value proposition:
   as it delegates fully to the `core` module, it uses the same defaults, location, and installation logic 
   as the build tooling would use.

   As such, it's possible to invoke the CLI prior to launching the build, to preinstall the necessary dependencies (as in a separate layer in a Docker container), and to be sure that the same dependencies definition in a build tool of your choosing (SBT or Mill) will _immediately_ find the packages where they're supposed to be.

   You can quickly test it by running:

   ```
    $ cs launch com.indoorvivants.vcpkg:sn-vcpkg_3:0.0.16 -- install libpq -l -q -c
    -I<...>/sbt-vcpkg/vcpkg-install/arm64-osx/lib/pkgconfig/../../include
    -L<...>/sbt-vcpkg/vcpkg-install/arm64-osx/lib/pkgconfig/../../lib
    -L<...>/sbt-vcpkg/vcpkg-install/arm64-osx/lib/pkgconfig/../../lib/pkgconfig/../../lib
    -lpq
    -lpgcommon
    -lpgport
    -lm
    -lssl
    -lcrypto
   ```

   This particular example
   1. Installs `libpq` (C interface to Postgres)
   2. Outputs compilation _and_ linking flags for Clang/GCC one per line
    
   As part of the process, the CLI will also bootstrap vcpkg if it cannot find it in the predetermined location (see below)

3. [SBT plugin](#sbt-plugin) and [Mill plugin](#mill-plugin) that just install the dependencies
4. SBT and Mill plugins that additionally integrate with respective Scala Native plugins, see [Scala Native integration](#scala-native-integration)

### SBT plugin

For SBT, add this to your `project/plugins.sbt`:

```scala
addSbtPlugin("com.indoorvivants.vcpkg" % "sbt-vcpkg" % "0.0.16")
```

And in your build.sbt:

```scala
enablePlugins(VcpkgPlugin)

vcpkgDependencies := VcpkgDependencies(
  "cjson",
  "cmark"
)
```

After that, run `vcpkgInstall` and vcpkg will be bootstrapped and dependencies installed.

Tasks and settings (find them all by doing `help vcpkg*` in SBT shell):

- `vcpkgDependencies` - see [VcpkgDependencies](#vcpkgdependencies)
- `vcpkgInstall` - performs bootstrap and installation of all specified dependencies. **Triggers bootstrap**

- `vcpkgRootInit` - see [VcpkgRootInit](#vcpkgrootinit)

- `vcpkgRoot` - the actual location of `vcpkg` installation, computed using `vcpkgRootInit`

- `vcpkgInstallDir` - the location where installed vcpkg artifacts will be placed - by default it's a cache folder located according to OS-specific guidelines

- `vcpkgBinary` - the location of just the `vcpkg` binary itself. **Triggers bootstrap**

- `vcpkgConfigurator` - see [VcpkgConfigurator](#vcpkgconfigurator)
-
- `vcpkgRun` - invoke the vcpkg CLI directly

### Mill plugin 

Add dependency to your `build.sc`:

```scala
import $ivy.`com.indoorvivants.vcpkg::mill-vcpkg:0.0.16`
```

And use the `VcpkgModule` mixin:

```scala 
import com.indoorvivants.vcpkg.millplugin.VcpkgModule
import com.indoorvivants.vcpkg._

import mill._, mill.scalalib._
object example extends ScalaModule with VcpkgModule {
  def scalaVersion = "3.3.1"
  def vcpkgDependencies = T(VcpkgDependencies("cmark", "cjson"))
}
```

and use `vcpkgInstall` to install vcpkg dependencies. 

The Mill tasks are the same as in the SBT plugin

### Scala Native integration

#### SBT

In `project/plugins.sbt`:

```scala
addSbtPlugin("com.indoorvivants.vcpkg" % "sbt-vcpkg-native" % "0.0.16")
```

In `build.sbt`:

```scala
enablePlugins(VcpkgNativePlugin, ScalaNativePlugin)

vcpkgDependencies := VcpkgDependencies(
  "cjson",
  "cmark",
)

vcpkgNativeConfig ~= {
  _.withRenamedLibraries(
    Map("cjson" -> "libcjson", "cmark" -> "libcmark")
  )
}
```

With that, if you `run` the project, vcpkg dependencies will be automatically installed 
and the `NativeConfig` will be configured so that compilation and linking will succeed.

For real world usage, see [Examples](#examples).

#### Mill

Add dependency to your `build.sc`:

```scala
import $ivy.`com.indoorvivants.vcpkg::mill-vcpkg-native:0.0.16`
```

And use the `VcpkgNativeModule` mixin:

```scala 
import com.indoorvivants.vcpkg.millplugin.native.VcpkgNativeModule
import com.indoorvivants.vcpkg.millplugin.VcpkgModule
import com.indoorvivants.vcpkg._

import mill._, mill.scalalib._
object example extends VcpkgNativeModule {
  def vcpkgDependencies = T(VcpkgDependencies("cjson", "cmark"))
  def scalaVersion = T("3.2.2")
  def scalaNativeVersion = T("0.4.10")

  override def vcpkgNativeConfig =
    T(super
        .vcpkgNativeConfig()
        .addRenamedLibrary("cjson", "libcjson")
        .addRenamedLibrary("cmark", "libcmark")
    )
}
```

### CLI

This is a very thin interface to the [Core](#core) module, designed 
mostly for demonstration purposes or to install dependencies in CI/containers,
without launching the SBT/Mill project.

Installation with Coursier:

```bash
$ cs install sn-vcpkg --channel https://cs.indoorvivants.com/i.json
```

Usage example:

```bash
$ sn-vcpkg install libgit2 -l -c
```

This will install `libgit2` package, and output linking flags (`-l`) and compilation flags (`-c`), one per line.

#### `--rename` argument

All commands accept a `--rename name1=alt_name1,name2=alt_name2` because for some packages in 
vcpkg the name of the package and the name under which it is installed in pkg-config might 
be different.

For example, curl is one of those. Running the simple command will complain that 
pkg-config configuration was not found

```
$ sn-vcpkg install curl -l -c 
...
    [vcpkg stderr] Package curl was not found in the pkg-config search path.
    [vcpkg stderr] Perhaps you should add the directory containing `curl.pc'
    [vcpkg stderr] to the PKG_CONFIG_PATH environment variable
    [vcpkg stderr] No package 'curl' found
...
```

And the approximated arguments it will output will be insufficient.

But if you ask to rename it during resolution:

```
$ sn-vcpkg install -l -c curl --rename curl=libcurl
```

Then you get the correct flags. The name of the argument is not great, but it's a bandaid
for an equally not great gotcha in vcpkg so I think we're even.


#### `pass` command

If you just want to invoke the vcpkg CLI, you can do so with the `pass` command:

```
sn-vcpkg pass -- help
```

Arguments after `--` will be passed directly to vcpkg, and its STDOUT output will be printed to STDOUT.


#### `bootstrap`

Only bootstrap vcpkg if necessary, without installing anything

```
Usage: sn-vcpkg bootstrap [--vcpkg-root-manual <location> [--no-bootstrap] | --vcpkg-root-env <env-var> [--no-bootstrap] | --no-bootstrap] [--vcpkg-install <dir>] [--no-bootstrap] [--verbose] [--quiet]

Bootstrap vcpkg

Options and flags:
    --help
        Display this help text.
    --vcpkg-root-manual <location>
        Initialise vcpkg in this location
    --no-bootstrap
        Allow bootstrapping vcpkg from scratch
    --vcpkg-root-env <env-var>
        Pick up vcpkg root from the environment variable
    --vcpkg-install <dir>
        folder where packages will be installed
    --verbose, -v
        Verbose logging
    --quiet, -q
        Only error logging
```

#### `install` command

Install one or several dependencies, by name or from a manifest file, and optionally output linking/compilation flags for all of them.

Examples: 
- `sn-vcpkg install libgit2 cjson -l -c`
- `sn-vcpkg install --manifest vcpkg.json -l -c`

```
Usage:
    sn-vcpkg install --manifest <string> [--output-compilation] [--output-linking] [--rename <spec1,spec2,spec3>] [--vcpkg-root-manual <location> [--no-bootstrap] | --vcpkg-root-env <env-var> [--no-bootstrap] | --no-bootstrap] [--vcpkg-install <dir>] [--no-bootstrap] [--verbose] [--quiet]
    sn-vcpkg install [--output-compilation] [--output-linking] [--rename <spec1,spec2,spec3>] [--vcpkg-root-manual <location> [--no-bootstrap] | --vcpkg-root-env <env-var> [--no-bootstrap] | --no-bootstrap] [--vcpkg-install <dir>] [--no-bootstrap] [--verbose] [--quiet] <dep>...

Install a list of vcpkg dependencies

Options and flags:
    --help
        Display this help text.
    --manifest <string>
        vcpkg manifest file
    --output-compilation, -c
        Output (to STDOUT) compilation flags for installed libraries, one per line
    --output-linking, -l
        Output (to STDOUT) linking flags for installed libraries, one per line
    --rename <spec1,spec2,spec3>
        rename packages when looking up their flags in pkg-config
        e.g. --rename curl=libcurl,cjson=libcjson
    --vcpkg-root-manual <location>
        Initialise vcpkg in this location
    --no-bootstrap
        Allow bootstrapping vcpkg from scratch
    --vcpkg-root-env <env-var>
        Pick up vcpkg root from the environment variable
    --vcpkg-install <dir>
        folder where packages will be installed
    --verbose, -v
        Verbose logging
    --quiet, -q
        Only error logging
```

#### `clang` and `clang++` commands

These commands invoke clang or clang++ with all the configuration 
flags required [^1] to run the specified dependencies.

For example, say you have a snippet of C code that needs sqlite3 dependency:

```c 
#include <stdio.h>
#include <sqlite3.h> 

int main(int argc, char* argv[]) {
   sqlite3 *db;
   char *zErrMsg = 0;
   int rc;

   rc = sqlite3_open("test.db", &db);

   if( rc ) {
      fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
      return(0);
   } else {
      fprintf(stderr, "Opened database successfully\n");
   }
   sqlite3_close(db);
}
```

You can compile it directly by running 

```
sn-vcpkg clang sqlite3 -- test-sqlite.c
```

Or if you have a vcpkg manifest file:

```json 
{
 "name": "my-application",
 "version": "0.15.2",
 "dependencies": ["sqlite3"]
}
```

You can use that as well:

```
sn-vcpkg clang --manifest vcpkg.json -- test-sqlite.c
```

All the arguments after `--` will be passed to clang/clang++ without modification (_before_ the flags calculated for dependencies)

#### `scala-cli` command

This command invokes your local installation of Scala CLI (`scala-cli` must be available on PATH),
and passes all the flags required by the specified dependencies [^1].

For example, say you have a Scala CLI script using [Porcupine](https://github.com/armanbilge/porcupine), a cross-platform functional library for Sqlite3:

**scala-cli-sqlite3.scala**
```scala

//> using dep "com.armanbilge::porcupine::0.0.1"
//> using platform scala-native
//> using scala 3.3.1

import porcupine.*
import cats.effect.IOApp
import cats.effect.IO
import cats.syntax.all.*
import scodec.bits.ByteVector

import Codec.*

object Test extends IOApp.Simple:
  val run =
    Database
      .open[IO](":memory:")
      .use: db =>
        db.execute(sql"create table porcupine (n, i, r, t, b);".command) *>
          db.execute(
            sql"insert into porcupine values(${`null`}, $integer, $real, $text, $blob);".command,
            (None, 42L, 3.14, "quill-pig", ByteVector(0, 1, 2, 3))
          ) *>
          db.unique(
            sql"select b, t, r, i, n from porcupine;"
              .query(blob *: text *: real *: integer *: `null` *: nil)
          ).flatTap(IO.println)
      .void
end Test
```

To run it with Scala Native, you must have `sqlite3` native 
dependency installed and configured, along with correct flags
passed to Scala Native.

You can run the script like this:

```
sn-vcpkg scala-cli sqlite3 -- run scala-cli-sqlite3.scala
```

The sn-vcpkg CLI will add the required `--native-compile/--native-linking` flags to the _end_ of your argument list automatically.


[^1]: as long as the dependencies themselves provide a well configured pkg-config file, of course

### Docker base image

Because of the sheer number of different tools required to install 
packages from vcpkg (like libtool, curl, zip/unzip, autoconf, make, cmake, etc.) we provide a Docker base image that contains _some_ of them. The list is by no means exhaustive and a PR adding more will be happily accepted.

The docker image contains the following:

1. Ubuntu 22.04 base
2. OpenJDK 17
3. Tools like

   ```
   clang zip unzip tar make cmake autoconf ninja-build
   pkg-config git libtool curl
   ```
4. SBT (1.9.x)
5. Coursier 
6. `sn-vpkg` CLI itself

Te purpose of this docker image is to be used as a baser on CI, e.g.:

```docker
# huge container we use only for builds 
FROM keynmol/sn-vcpkg:latest as dev

# install your application's dependencies
RUN sn-vcpkg install curl

WORKDIR /workdir

# copy your sources into container
COPY . .

# run the build of your scala native application
RUN sbt myApp/nativeLink

# This is the actual, much smaller container that will run the app
FROM <runtime-container> 

# copy the built app from the dev container
COPY --from=dev /workdir/build/server /usr/bin/server

ENTRYPOINT ["server"]
```

### Core


#### VcpkgRootInit

Defines the location where Vcpkg will be bootstrapped.

**Variations (with defaults)**:

- **(default)** `SystemCache(allowBootstrap = true)` - will bootstrap (if allowed) vcpkg in a 
  system cache directory, decided by [dirs-dev](https://github.com/dirs-dev/directories-jvm#introduction) library

- `FromEnv(name = "VCPKG_ROOT", allowBootstrap = true)` - will bootstrap (if allowed) 
  in a location specified by an environment variable `name`

- `Manual(file: File, allowBootstrap = true)` - will bootstrap (if allowed) in 
  a location specified by `file`

#### VcpkgNativeConfig

This configuration object controls various aspects of how sn-vcpkg 
manipulates Scala Native's `NativeConfig` object to add linking and 
compilation arguments from installed vcpkg dependencies.

**Defaults**
```scala
VcpkgNativeConfig()
// res2: VcpkgNativeConfig = Vcpkg NativeConfig: 
//   | approximate = true
//   | autoConfigure = true
//   | prependCompileOptions = true
//   | prependLinkingOptions = true
//   | renamedLibraries = 
//   | staticLinking = false
```

**approximate** - whether to approximate compilation/linking flags 
in case pkg-config file is not shipped with the library

```scala
VcpkgNativeConfig().withApproximate(true)
```

**autoConfigure** - whether to automatically configure Scala Native's `NativeConfig` 
with flags for all specified vcpkg dependencies

```scala
VcpkgNativeConfig().withAutoConfigure(true)
```

**prependCompileOptions** -  whether to **prepend** compilation flags derived from vcpkg before 
the flags that Scala Native puts.

It can be useful because Scala Native adds certain system locations to linking flags 
by default, and these might have non-vcpkg versions of some of your dependencies

```scala
VcpkgNativeConfig().withPrependCompileOptions(true)
```

**prependLinkingOptions** -  whether to **prepend** linking flags derived from vcpkg before 
the flags that Scala Native puts.

```scala
VcpkgNativeConfig().withPrependLinkingOptions(true)
```

**renamedLibraries** - a mapping between vcpkg package names and the names under which the `pkg-config` files are installed - those can be different for no good reason whatsoever.

```scala
// Completely overwrite
VcpkgNativeConfig().withRenamedLibraries(Map("cjson" -> "libcjson", "cmark" -> "libcmark"))
// res7: VcpkgNativeConfig = Vcpkg NativeConfig: 
//   | approximate = true
//   | autoConfigure = true
//   | prependCompileOptions = true
//   | prependLinkingOptions = true
//   | renamedLibraries = cjson -> libcjson, cmark -> libcmark
//   | staticLinking = false

// Append only
VcpkgNativeConfig().addRenamedLibrary("cjson", "libcjson")
// res8: VcpkgNativeConfig = Vcpkg NativeConfig: 
//   | approximate = true
//   | autoConfigure = true
//   | prependCompileOptions = true
//   | prependLinkingOptions = true
//   | renamedLibraries = cjson -> libcjson
//   | staticLinking = false
```

#### VcpkgDependencies

Specification for vcpkg dependencies. Can be either:

- a simple list of dependency names:

```scala
VcpkgDependencies("cmark", "cjson")
// res9: VcpkgDependencies = Names(
//   deps = List(
//     Dependency(name = "cmark", features = Set()),
//     Dependency(name = "cjson", features = Set())
//   )
// )
```

- a path to manifest file:

```scala
VcpkgDependencies(new java.io.File("./vcpkg.json"))
// res10: VcpkgDependencies = ManifestFile(path = ./vcpkg.json)
```

- a list of detailed dependency specs:

```scala
VcpkgDependencies.Names(List(Dependency("libpq", Set("arm-build")), Dependency.parse("cpprestsdk[boost]")))
// res11: Names = Names(
//   deps = List(
//     Dependency(name = "libpq", features = Set("arm-build")),
//     Dependency(name = "cpprestsdk", features = Set("boost"))
//   )
// )
```

#### VcpkgConfigurator

While this class has useful methods of its own (see [API docs](https://www.javadoc.io/doc/com.indoorvivants.vcpkg/vcpkg-core_3/latest/com/indoorvivants/vcpkg/VcpkgConfigurator.html)), its main purpose is to provide a configured `PkgConfig` instance

#### PkgConfig

[API docs](https://www.javadoc.io/doc/com.indoorvivants.vcpkg/vcpkg-core_3/latest/com/indoorvivants/vcpkg/PkgConfig.html)

A thin pre-configured (by the build tool) wrapper around [pkg-config](https://www.freedesktop.org/wiki/Software/pkg-config/) tool.

[sn-bindgen]: https://sn-bindgen.indoorvivants.com
