// Copyright (c) 2023 Andrejs Grišins, Anastasia Petrova. Unauthorized use prohibited.

import repositories.UserRepository
import services.AuthService
import config.authConfig
import services.TokenService
import util.JwtUtil
import controllers.authRouting
import config.RedisConfig
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.ClientOptions

fun main() {
    println("Hello, AAMessenger!")

    val redisClient = RedisConfig().redisClient()
    val connection = RedisConfig().redisConnection(redisClient)
    val redisCommands: RedisCommands<String, String> = RedisConfig().redisCommands(connection)

    val userRepository = UserRepository(authConfig)
    val jwtUtil = JwtUtil()
    val tokenService = TokenService(redisCommands, jwtUtil)
    val authService = AuthService(userRepository, authConfig, tokenService, jwtUtil)
    
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            route("/api") {
                authRouting(authService)
            }
        }
    }.start(wait = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        connection.close()
        redisClient.shutdown()
    })    
}