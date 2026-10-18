package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testReferralCommissionCalculation() {
    val planPrice = 2500.0
    val commission = planPrice * 0.20
    assertEquals(500.0, commission, 0.001)
  }

  @Test
  fun testReferralCodeFormat() {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val code = "MB" + (1..4).map { chars.random() }.joinToString("")
    assertEquals(6, code.length)
    assertTrue(code.startsWith("MB"))
  }
}

