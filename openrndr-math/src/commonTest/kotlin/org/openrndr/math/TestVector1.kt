package org.openrndr.math

import kotlin.test.Test

class TestVector1 {
    @Test
    fun shouldHaveArea() {
        val a = Vector1(10.0).areaBetween(Vector1(20.0))
        println(a)

    }

    @Test
    fun shouldHaveAtan2() {
        val a = Vector1(10.0).atan2(Vector1(20.0))
        println(a)
    }

}