package com.underwood.web

import cn.hutool.core.util.ClassUtil
import com.underwood.config.TConfig
import com.underwood.constant.Constant
import com.underwood.model.Result
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.core.http.httpServerOptionsOf


fun RoutingContext.success(data: JsonObject) {
    val result = Result(data = data)
    this.response().end(result.toString())
}

fun RoutingContext.failed(data: JsonObject, errorCode: Int = 10086, errorMsg: String = "service error") {
    val result = Result(success = false, data = data, errorCode = errorCode, errorMsg = errorMsg)
    this.response().end(result.toString())
}

abstract class TWebVerticle: AbstractVerticle() {
    override fun start() {
        val config = TConfig.instance()
        val httpServerOptions = httpServerOptionsOf(port = config.getInteger("port", 8080))
        val server = vertx.createHttpServer(httpServerOptions)
        val router = Router.router(vertx);

        router.initHandlerConfig()
        router.registerHandler()

        server.requestHandler { router.accept(it) }.listen()
    }

    fun Router.initHandlerConfig() {
        this.route("/static/*").handler(StaticHandler.create()) //静态资源的handler
        this.route().handler(BodyHandler.create())
        this.route().handler(CookieHandler.create())
        this.route().handler(CorsHandler.create("*")
                .allowedMethods(setOf(HttpMethod.GET, HttpMethod.POST))
                .allowedHeaders(setOf("Access-Control-Allow-Origin", "x-requested-with", "Origin", "Content-Type", "Accept")))

        this.route().handler { context ->
            val resp = context.response()
            resp.isChunked = true
            if (context.acceptableContentType != null) {
                resp.putHeader("content-type", context.acceptableContentType)
            }
            context.next()
        }

        this.route("/test").handler {
            throw Exception("403")
        }

        this.route().last().handler { context -> //未匹配到router的请求都会进入此分支
            val resp = context.response()
            resp.statusCode = 404
            resp.end("404 not found")
        }.failureHandler { context -> //处理handler时如果抛出异常就会进入此分支
            val resp = context.response()
            val message = context.failure().message
            resp.statusCode = when (message) {
                "403" -> 403
                else -> 500
            }
            resp.end(when (resp.statusCode) {
                403 -> "403 forbidden"
                404 -> "404 not found"
                else -> "global error process"
            })
        }
    }

    fun Router.registerHandler() {
        doRegisterCommonHandler(this)
        val classSets = ClassUtil.scanPackage(Constant.PACKAGE_VERTICLE_PREFIX)
        classSets.forEach {
            try {
                val instance = Class.forName(it.name).newInstance()
                if (instance is TWebVerticle) {
                    instance.doRegisterHandler(this)
                }
            } catch (e: Exception) {
                //ClassUtil可能和kotlin不兼容，导致扫描出错误的className，newInstance()方法会抛出异常,暂时忽略它
            }
        }
    }

    fun doRegisterCommonHandler(router: Router) {
        router.get("/login").handler {  }
        router.post("/logout").handler {  }
        router.post("/register").handler {  }
    }

    abstract fun doRegisterHandler(router: Router)
}