# sn-vcpkg

<!--toc:start-->
- [sn-vcpkg](#sn-vcpkg)
  - [Vcpkg and native dependencies 101](#vcpkg-and-native-dependencies-101)
  - [Usage](#usage)
    - [SBT plugin](#sbt-plugin)
    - [Scala Native integration](#scala-native-integration)
    - [Core](#core)
      - [VcpkgRootInit](#vcpkgrootinit)
<!--toc:end-->

Mill and SBT plugins to work with [vcpkg C/C++ dependency manager](https://vcpkg.io/en/index.html) 

If you are in a rant-reading mood, please refer to [motivation](/docs/motivation.md).

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

After that, run `vcpkgInstall` and vcpkg will be bootstrapped and dependencies installed


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

```scala mdoc 


