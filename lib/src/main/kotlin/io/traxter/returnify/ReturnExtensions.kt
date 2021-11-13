package io.traxter.returnify

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <A, B> A?.toReturnOr(error: () -> B): Return<A, B> {
    contract {
        callsInPlace(error, InvocationKind.AT_MOST_ONCE)
    }

    return if (this == null) {
        Failure(error())
    } else {
        Success(this)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <A, B> Return<A?, B>.expectNotNullOr(error: () -> B): Return<A, B> {
    contract {
        callsInPlace(error, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Success -> if (this.value == null) {
            Failure(error())
        } else {
            Success(this.value)
        }
        is Failure -> this
    }
}

fun <A, B> Iterable<Return<A, B>>.allSuccessful(): Return<List<A>, B> {
    val resultValues = map {
        when (it) {
            is Success -> it.value
            is Failure -> return it
        }
    }
    return Success(resultValues)
}

fun <A, B> Iterable<Return<A, B>>.anySuccessful(): List<A> =
    filterIsInstance<Success<A>>().map { it.value }
