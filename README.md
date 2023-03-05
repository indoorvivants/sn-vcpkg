# sn-vcpkg

<!--toc:start-->
- [sn-vcpkg](#sn-vcpkg)
  - [Vcpkg and native dependencies 101](#vcpkg-and-native-dependencies-101)
  - [Usage](#usage)
    - [SBT plugin](#sbt-plugin)
    - [Mill plugin](#mill-plugin)
    - [Scala Native integration](#scala-native-integration)
    - [Core](#core)
      - [VcpkgRootInit](#vcpkgrootinit)
      - [VcpkgNativeConfig](#vcpkgnativeconfig)
      - [VcpkgDependencies](#vcpkgdependencies)
<!--toc:end-->

Mill and SBT plugins to work with [vcpkg C/C++ dependency manager](https://vcpkg.io/en/index.html) 

If you are in a rant-reading mood, please refer to [motivation](/docs/motivation.md).

**If you want to update this documentation file, don't edit it directly - edit [docs/README.in.md](/docs/README.in.md) and run `sbt updateDocs`. It's annoying, but this document contains compiled snippets of code which I want to prevent from going out of date**

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

There are several modules of interest:

1. `core` - contains all the tool-agnostic logic for bootstrapping, invoking, and communicating with `vcpkg`.
   This module is the meat on the proverbial bones, the plugins and the CLI merely invoke it.

2. `cli` - contains a very barebones CLI wrapper over `core` with dubious value proposition:
   as it delegates fully to the `core` module, it uses the same defaults, location, and installation logic 
   as the build tooling would use.

   As such, it's possible to invoke the CLI prior to launching the build, to preinstall the necessary dependencies (as in a separate layer in a Docker container), and to be sure that the same dependencies definition in a build tool of your choosing (SBT or Mill) will _immediately_ find the packages where they're supposed to be.

   You can quickly test it by running:

   ```
    $ cs launch com.indoorvivants.vcpkg:sn-vcpkg_3:<VERSION> -- install libpq -l -q -c
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
addSbtPlugin("com.indoorvivants.vcpkg" % "sbt-vcpkg" % <version>)
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

### Mill plugin 

Add dependency to your `build.sc`:

```scala
import $ivy.`com.indoorvivants.vcpkg::mill-vcpkg:<VERSION>`
```

And use the `VcpkgModule` mixin:

```scala 
import com.indoorvivants.vcpkg.millplugin.VcpkgModule
import com.indoorvivants.vcpkg._

import mill._, mill.scalalib._
object example extends ScalaModule with VcpkgModule {
  def scalaVersion = "3.2.2"
  def vcpkgDependencies = T(VcpkgDependencies("cmark", "cjson"))
}
```

and use `vcpkgInstall` to install vcpkg dependencies. 

The Mill tasks are the same as in the SBT plugin

### Scala Native integration




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
// res0: VcpkgNativeConfig = Vcpkg NativeConfig: 
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
// res5: VcpkgNativeConfig = Vcpkg NativeConfig: 
//   | approximate = true
//   | autoConfigure = true
//   | prependCompileOptions = true
//   | prependLinkingOptions = true
//   | renamedLibraries = cjson -> libcjson, cmark -> libcmark
//   | staticLinking = false

// Append only
VcpkgNativeConfig().addRenamedLibrary("cjson", "libcjson")
// res6: VcpkgNativeConfig = Vcpkg NativeConfig: 
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
// res7: VcpkgDependencies = Names(
//   deps = List(
//     Dependency(name = "cmark", features = Set()),
//     Dependency(name = "cjson", features = Set())
//   )
// )
```

- a path to manifest file:

```scala
VcpkgDependencies(new java.io.File("./vcpkg.json"))
// res8: VcpkgDependencies = ManifestFile(path = ./vcpkg.json)
```

- a list of detailed dependency specs:

```scala
VcpkgDependencies.Names(List(Dependency("libpq", Set("arm-build")), Dependency.parse("cpprestsdk[boost]")))
// res9: VcpkgDependencies.Names = Names(
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
