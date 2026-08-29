plugins {
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        getByName("webMain") {
            dependencies {
                api(project(":openrndr-application"))
                api(project(":openrndr-draw"))
                implementation(project(":openrndr-gl-common"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.js)
                implementation(libs.kotlin.browser)
                implementation(libs.kotlin.web)
                implementation(libs.kotlin.stdlib)

            }
        }
        getByName("webTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        getByName("wasmJsMain") {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.named("jsNodeTest") {
    enabled = false
}

tasks.named("wasmJsNodeTest") {
    enabled = false
}

// Disabled: wasmJsBrowserTest currently crashes before any test code runs, e.g.:
//   ClassCastException: Cannot cast instance of Object to Node: incompatible types
//       at kotlin.createJsError (.../commons.js)
//       at ...kotlin.wasm.internal.THROW_CCE_WITH_INFO
//       at ...web.dom.__JsClosureToKotlinClosure_((Js)->Js).invoke
//       at ...kotlin.test.adapter
// This was verified to be unrelated to openrndr code: it reproduces identically
// with a single trivial `assertTrue(true)` test that has no dependency on
// ApplicationWebGL/DOM at all. The crash happens inside the generated
// `kotlin.test.adapter` closure (from kotlin-test-wasm-js), which uses the
// kotlin-wrappers `web.dom` JS-closure-to-Kotlin-closure conversion to report
// results back to Karma; that conversion throws a ClassCastException, and
// constructing the exception's message itself triggers a second failing cast,
// crashing the whole run before any test result is reported. This points to an
// incompatibility/bug between the Kotlin/Wasm test runner (kotlin-test-wasm-js)
// and the karma-webpack test reporting glue used for wasmJs browser tests.
// Re-enable once this is fixed upstream (Kotlin / kotlin-wrappers).
tasks.named("wasmJsBrowserTest") {
    enabled = false
}
