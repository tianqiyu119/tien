package com.underwood

import com.underwood.config.TConfig
import com.underwood.constant.Constant
import com.underwood.web.TWebVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger(Constant.APPLICATION_NAME)
    val config = TConfig.instance()
    val workerPoolSize = config.getInteger("workerPoolSize", 32)
    val defaultEventLoopPoolSize = Runtime.getRuntime().availableProcessors() * 2
    val eventLoopPoolSize = config.getInteger("eventLoopPoolSize", defaultEventLoopPoolSize)

    val vertxOptions = vertxOptionsOf(workerPoolSize = workerPoolSize, eventLoopPoolSize = eventLoopPoolSize)
    val vertx = Vertx.vertx(vertxOptions)
    val deploymentOptions = deploymentOptionsOf(instances = config.getInteger("instance", 1))
    vertx.deployVerticle(TemplateVerticle(), deploymentOptions) { res ->
        if (res.succeeded()) {
            log.info("Deploy success. Deployment id is: ${res.result()}")
        } else {
            res.cause().printStackTrace()
            log.warn("Deploy failed.")
        }
    }
}

class TemplateVerticle : TWebVerticle() {
    override fun doRegisterHandler(router: Router) {
        //处理业务
    }

}

fun test() {
    val mRunnable = Runnable {
        run {
            for (i in 1..100) {
                Thread.sleep(1000)
                val config = TConfig.instance()
                println(config.getString("name"))
            }
        }
    }
    Thread(mRunnable).start()
}