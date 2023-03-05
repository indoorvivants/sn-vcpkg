### Motivation

The world of C/C++ libraries paints a controversial picture: on the one hand you have incredibly robust,
industry standard libraries, with most vigorous testing imaginable, with millions of developers using them 
every day.

On the other hand, you have libraries that have CVEs named after them.

What unites those seemingly opposite worlds, is that installation and version management of those libraries
is a logistical nightmare.

Your usual options:

1. Downloading the library tarball from a university website and building it yourself
2. Installing the library package from the global package manager, hoping it won't corrupt the rest of your packages - 
   bonus points for libraries with versions in the name, and libraries served from non-official repositories, packaged
   by poor people who have no relation to the library whatsoever!
3. Depending on the Git repository directly from a submodule
4. Receiving the library code by email because the author adamantly despises any notion of VCS

5 through N are various other methods of distribution which are either not portable to different OS, or no longer 
work because the instructions were last updated in 2009.

The issue gets exponentially more complex if the library dares to declare dependencies - you need to build those first,
and then tackle the menagerie of CMake flags to produce the version of the code that works with your particular system and 
where it places header files and binary artifacts.

As much as us JVM developers like to groan and moan about Maven distribution model, it is _incredibly_ easy to add a dependency to your build
and move on with the actual task at hand. Same can be said about JavaScript world, and to a degree Rust ecosystem.


