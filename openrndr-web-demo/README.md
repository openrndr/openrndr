### Running the demo in the browser

#### Development run

Fast build, unoptimized binary.

```
# JavaScript
./gradlew openrndr-web-demo:jsBrowserDevelopmentRun -t

or

# Wasm 
./gradlew openrndr-web-demo:wasmJsBrowserDevelopmentRun -t
```

#### Production run

Slower build, optimized binary.

```
# JavaScript
./gradlew openrndr-web-demo:jsBrowserProductionRun -t

or

# Wasm 
./gradlew openrndr-web-demo:wasmJsBrowserProductionRun -t
```

#### Export

Export a runnable web application into `build/dist/`. 

```
# JavaScript
./gradlew jsBrowserDistribution

or

# Wasm
./gradlew wasmJsBrowserDistribution
```

The source code of the programs is under `src/webMain/kotlin/`, the HTML template at `src/webMain/resources/index.html`.