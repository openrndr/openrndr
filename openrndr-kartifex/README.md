# openrndr-kartifex

Here you find a pure Kotlin port of [Artifex](https://github.com/lacuna/artifex).

Artifex is originally written by Zachary Tellman and is distributed under the MIT license.

This port is initiated by Edwin Jakobs in July 2021, and is part of the OPENRNDR library. Reason for
porting the code to Kotlin is the need for the library to be available when targetting Kotlin/JS. 

Zachary has written an [insightful text](https://ideolalia.com/code/better-geometry-through-graph-theory.html) on the problems that Artifex solves.

Artifex is used by OPENRNDR to handle Bezier polygon clipping (boolean operations).

## Port limitations

A decision was made to not use Zachary's [Bifurcan](https://github.com/lacuna/bifurcan) library of containers and to use 
the standard Kotlin container classes instead. This may cause a difference in performance
but should not affect precision. 

## Port variance

The implementations of `Scalar.normalizationFactor` have been adjusted to address [an Artifex issue](https://github.com/lacuna/artifex/issues/3) in which intersections are not detected.

Artifex sets `Intersections.SPATIAL_EPSILON = 1e-10`. In order to address missed intersections between cubic bezier curves that value
has been increased to `1e-6`. [Relevant Artifex issue](https://github.com/lacuna/artifex/issues/4)

## Validation

This port has not been fully validated. Unit tests that lightly compare the outputs of Artifex and Kartifex can be found under [src/jvmTest](src/jvmTest)

