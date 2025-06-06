// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/utils/Resource.kt
package com.spidroid.starry.utils

class Resource<T> private constructor(val status: Status?, val data: T?, val message: String?) {
    enum class Status {
        SUCCESS,
        ERROR,
        LOADING,
        EMPTY
    }

    companion object {
        fun <T> success(data: T?): Resource<T?> {
            return Resource<T?>(Status.SUCCESS, data, null)
        }

        fun <T> error(msg: String?, data: T?): Resource<T?> {
            return Resource<T?>(Status.ERROR, data, msg)
        }

        fun <T> loading(data: T?): Resource<T?> {
            return Resource<T?>(Status.LOADING, data, null)
        }

        fun <T> empty(): Resource<T?> {
            return Resource<T?>(Status.EMPTY, null, null)
        }
    }
}