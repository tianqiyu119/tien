package com.underwood.model

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.Json
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

data class Result(val success: Boolean = true, val data: JsonObject,
                  val errorCode: Int? = null, val errorMsg: String? = null) {
    override fun toString(): String {
        val result = json {
            obj {
                "success" to success
                "data" to data.toString()
                "errorCode" to errorCode
                "errorMsg" to errorMsg
            }
        }
        return result.toString()
    }
}