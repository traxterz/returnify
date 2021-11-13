package io.traxter.returnify

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReturnBindingTests {
    @Test
    fun `One can chain complex computations with the ReturnBinding`() {
        val r: Return<Int, String> = returns {
            val r1 = saveDivide(12, 2).unwrap()
            val r2 = saveDivide(r1, 2).unwrap()
            r1 + r2
        }

        assertEquals(9, r.getOrThrow())
    }

    @Test
    fun `Failed computations abort the execution of a ReturnBinding`() {
        val r = returns<Int, String> {
            val r1 = saveDivide(8, 2).unwrap()
            saveDivide(r1, 0).unwrap()
        }

        assertTrue(r.isFailure)
    }
}
