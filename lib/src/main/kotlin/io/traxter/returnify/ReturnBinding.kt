package io.traxter.returnify

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal class ReturnBindingException : Exception()

interface ReturnBinding<B> {
    fun <A> Return<A, B>.unwrap(): A
    fun <A, E> Return<A, E>.unwrap(context: (E) -> B): A
}

internal class ReturnBindingImpl<B> : ReturnBinding<B> {
    lateinit var error: Failure<B>

    override fun <A> Return<A, B>.unwrap(): A =
        when (this) {
            is Success -> this.value
            is Failure -> {
                error = this
                throw ReturnBindingException()
            }
        }

    // Integration with the ReturnError type. In general the Return type is not bound to a specific Failure type
    override fun <A, E> Return<A, E>.unwrap(context: (E) -> B): A =
        when (this) {
            is Success -> this.value
            is Failure -> {
                val oldFailureValue = this.cause
                val newFailureValue = context(oldFailureValue)

                if (newFailureValue is ReturnError && oldFailureValue is ReturnError) {
                    newFailureValue.causedBy(oldFailureValue)
                }

                error = Failure(newFailureValue)
                throw ReturnBindingException()
            }
        }
}

@OptIn(ExperimentalContracts::class)
fun <A, B> returns(computation: ReturnBinding<B>.() -> A): Return<A, B> {
    contract {
        callsInPlace(computation, InvocationKind.EXACTLY_ONCE)
    }

    val binding = ReturnBindingImpl<B>()

    return try {
        Success(binding.computation())
    } catch (e: ReturnBindingException) {
        binding.error
    }
}

@OptIn(ExperimentalContracts::class)
suspend fun <A, B> coReturns(computation: suspend ReturnBinding<B>.() -> A): Return<A, B> {
    contract {
        callsInPlace(computation, InvocationKind.EXACTLY_ONCE)
    }

    val binding = ReturnBindingImpl<B>()

    return try {
        Success(binding.computation())
    } catch (e: ReturnBindingException) {
        binding.error
    }
}
