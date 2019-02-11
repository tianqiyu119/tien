@file:Suppress("SpellCheckingInspection")

package com.underwood.config

import com.underwood.constant.Constant
import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.config.configRetrieverOptionsOf
import io.vertx.kotlin.config.configStoreOptionsOf
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.slf4j.LoggerFactory

object TConfig {
    private val log = LoggerFactory.getLogger(Constant.APPLICATION_NAME)
    private var config: JsonObject = JsonObject()
    private val store = configStoreOptionsOf(
            type = "git",
            config = json {
                obj(
                        "url" to Constant.CONFIG_GIT_REPO_URL,
                        "path" to Constant.LOCAL_CONFIG_PATH,
                        "filesets" to array(obj("pattern" to "*.json"))
                )
            })
    private val retrieverOptions = configRetrieverOptionsOf(stores = listOf(store), scanPeriod = 100000)
    private val retriever = ConfigRetriever.create(Vertx.vertx(), retrieverOptions)

    fun init() {
        retriever.getConfig { ar ->
            if (ar.failed()) {
                log.warn("Failed to retrieve config. cause: \n${ar.cause()}")
            } else {
                config = ar.result()
            }
        }
        retriever.listen {
            log.info("config change: \n${it.newConfiguration}")
            config = it.newConfiguration
        }
        Thread.sleep(1000) //ensure retrieving config successfully for the first time
    }

    @Synchronized fun instance(): JsonObject {
        if (config.isEmpty) {
            init()
        }
        return config
    }
}