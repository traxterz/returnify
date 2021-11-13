package io.traxter.returnify

import kotlinx.coroutines.yield

fun saveDivide(a: Int, b: Int) =
    if (b != 0) {
        Success(a / b)
    } else {
        Failure("Error")
    }

suspend fun coSaveDivide(a: Int, b: Int): Return<Int, String> {
    yield()
    return saveDivide(a, b)
}

fun unsafeDivide(a: Int, b: Int) = a / b

suspend fun coUnsafeDivide(a: Int, b: Int): Int {
    yield()
    return unsafeDivide(a, b)
}
