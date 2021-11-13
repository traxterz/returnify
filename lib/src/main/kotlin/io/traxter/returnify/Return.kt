package io.traxter.returnify

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// NOTE: In general we use A for success types and B for error types
sealed class Return<out A, out B> {
    abstract val isSuccess: Boolean
    abstract val isFailure: Boolean

    inline fun <C> fold(onSuccess: (A) -> C, onFailure: (B) -> C): C = when (this) {
        is Success -> onSuccess(this.value)
        is Failure -> onFailure(this.cause)
    }

    inline fun onSuccess(f: (A) -> Unit): Return<A, B> {
        if (this is Success) {
            f(this.value)
        }
        return this
    }

    inline fun onFailure(f: (B) -> Unit): Return<A, B> {
        if (this is Failure) {
            f(this.cause)
        }
        return this
    }
}

data class Success<out A>(
    val value: A,
) : Return<A, Nothing>() {
    override val isSuccess = true
    override val isFailure = false
}

data class Failure<out B>(
    val cause: B,
) : Return<Nothing, B>() {
    override val isSuccess = false
    override val isFailure = true
}

@OptIn(ExperimentalContracts::class)
inline fun <A1, A2, B> Return<A1, B>.andThen(f: (A1) -> Return<A2, B>): Return<A2, B> {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Success -> f(this.value)
        is Failure -> this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A, B1, B2> Return<A, B1>.inCaseOfFailure(f: (B1) -> Return<A, B2>): Return<A, B2> {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Success -> this
        is Failure -> f(this.cause)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A1, A2, B> Return<A1, B>.map(f: (A1) -> A2): Return<A2, B> {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Success -> Success(f(this.value))
        is Failure -> this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A, B1, B2> Return<A, B1>.mapFailure(f: (B1) -> B2): Return<A, B2> {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Success -> this
        is Failure -> Failure(f(this.cause))
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A> returnCatching(f: () -> A): Return<A, Throwable> {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        Success(f())
    } catch (e: ReturnBindingException) {
        // This is an internal exception meant to be cached by the ReturnBindingImpl
        throw e
    } catch (e: Throwable) {
        Failure(e)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A, T> T.returnCatching(f: T.() -> A): Return<A, Throwable> {
    contract {
        callsInPlace(f, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        Success(f())
    } catch (e: ReturnBindingException) {
        // This is an internal exception meant to be cached by the ReturnBindingImpl
        throw e
    } catch (e: Throwable) {
        Failure(e)
    }
}

fun <A, B> Return<A, B>.getOrThrow(): A =
    when (this) {
        is Success -> this.value
        is Failure -> {
            if (cause is ReturnError) {
                error("expected Success but found Failure<ReturnError>: ${cause.errorDescription}")
            } else {
                error("expected Success but found Failure<*>")
            }
        }
    }

fun <A, B> Return<A, B>.getOrNull(): A? =
    when (this) {
        is Success -> this.value
        is Failure -> null
    }

fun <A, B> Return<A, B>.getOrDefault(defaultValue: A): A =
    when (this) {
        is Success -> this.value
        is Failure -> defaultValue
    }

@OptIn(ExperimentalContracts::class)
inline fun <A, B> Return<A, B>.getOrElse(f: (B) -> A): A {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Success -> value
        is Failure -> f(this.cause)
    }
}

fun <A, B> Return<A, B>.getFailureOrThrow(): B =
    when (this) {
        is Success -> error("can not unwrap an Error from a Success value")
        is Failure -> this.cause
    }

fun <A, B> Return<A, B>.getFailureOrNull(): B? =
    when (this) {
        is Success -> null
        is Failure -> this.cause
    }

fun <A, B> Return<A, B>.getFailureOrDefault(defaultValue: B): B =
    when (this) {
        is Success -> defaultValue
        is Failure -> this.cause
    }

@OptIn(ExperimentalContracts::class)
fun <A, B> Return<A, B>.getFailureOrElse(f: (A) -> B): B {
    contract {
        callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Success -> f(this.value)
        is Failure -> this.cause
    }
}
