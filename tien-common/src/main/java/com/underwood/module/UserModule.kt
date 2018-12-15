package com.underwood.module

import io.vertx.ext.web.RoutingContext

object UserModule {
    fun retrieveUserInfo(context: RoutingContext) {
        val userName = context.request().getParam("name")
    }
}