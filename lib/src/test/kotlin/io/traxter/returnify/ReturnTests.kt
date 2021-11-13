import io.traxter.returnify.Failure
import io.traxter.returnify.Return
import io.traxter.returnify.Success
import io.traxter.returnify.andThen
import io.traxter.returnify.getOrDefault
import io.traxter.returnify.getOrElse
import io.traxter.returnify.getOrNull
import io.traxter.returnify.getOrThrow
import io.traxter.returnify.inCaseOfFailure
import io.traxter.returnify.map
import io.traxter.returnify.mapFailure
import io.traxter.returnify.returnCatching
import io.traxter.returnify.saveDivide
import io.traxter.returnify.unsafeDivide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ReturnTests {
    @Test
    fun `Return objects can be folded`() {
        val r1 = saveDivide(4, 2).fold({ "Success" }, { "Failure" })
        assertEquals("Success", r1)

        val r2 = saveDivide(4, 0).fold({ "Success" }, { "Failure" })
        assertEquals("Failure", r2)
    }

    @Test
    fun `One can chain computations with andThen`() {
        val result = saveDivide(12, 2)
            .andThen { saveDivide(it, 2) }.getOrThrow()
        assertEquals(3, result)
    }

    @Test
    fun `Failure objects will not be chained with andThen`() {
        val result = saveDivide(12, 0)
            .andThen { Success(1) }
        assertTrue(result.isFailure)
    }

    @Test
    fun `Failure objects can be chained with inCaseOfFailure`() {
        val result = saveDivide(12, 0)
            .inCaseOfFailure { Success(1) }.getOrThrow()
        assertEquals(1, result)
    }

    @Test
    fun `Success objects will not be chained with inCaseOfFailure`() {
        val result = saveDivide(12, 2)
            .inCaseOfFailure { Failure(0) }.getOrThrow()
        assertEquals(6, result)
    }

    @Test
    fun `Wrapped Success values can be mapped with the map function`() {
        val two = Success(1).map { it + 1 }
        assertEquals(2, two.getOrThrow())
    }

    @Test
    fun `The map function will not change Failure objects`() {
        when (val f = Failure(1).map { "new value" }) {
            is Success -> fail("Must not be a Success value")
            is Failure -> assertEquals(1, f.cause)
        }
    }

    @Test
    fun `Wrapped failure values can be mapped with the mapFailure function`() {
        when (val r = Failure(1).mapFailure { it + 1 }) {
            is Success -> fail("Must not be reached")
            is Failure -> assertEquals(2, r.cause)
        }
    }

    @Test
    fun `The mapFailure function will not change Success objects`() {
        val f: Return<Int, Int> = Success(1)
        assertEquals(1, f.mapFailure { it + 1 }.getOrThrow())
    }

    @Test
    fun `Function that throw exceptions can be wrapped into Results with returnCatching`() {
        val failure = returnCatching {
            unsafeDivide(1, 0)
        }
        assertTrue(failure.isFailure)
    }

    @Test
    fun `The result of returnCatching will be wrapped into a Success object`() {
        val r = returnCatching { 1 }.getOrThrow()
        assertEquals(1, r)

        val success = "Hello".returnCatching { length }
        assertEquals(5, success.getOrThrow())
    }

    @Test
    fun `Return objects can be converted to optionals`() {
        assertEquals(2, saveDivide(4, 2).getOrNull())
        assertNull(saveDivide(4, 0).getOrNull())
    }

    @Test
    fun `One can recover from Failures by providing a fallback value`() {
        val r = Failure("Error").getOrDefault("Success")
        assertEquals("Success", r)
    }

    @Test
    fun `One can recover from Failures by providing a fallback computation`() {
        val r = Failure("Error").getOrElse { errorValue -> "$errorValue message" }
        assertEquals("Error message", r)
    }
}
