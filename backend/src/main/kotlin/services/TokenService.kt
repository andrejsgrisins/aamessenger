// Copyright (c) 2023 Andrejs Grišins, Anastasia Petrova. Unauthorized use prohibited.
package services

import util.JwtUtil
import io.lettuce.core.api.sync.RedisCommands

class TokenService(
    private val redisCommands: RedisCommands<String, String>,
    private val jwtUtil: JwtUtil
) {
    fun generateToken(username: String): String {
        val token = jwtUtil.generateToken(username)
        val jti = jwtUtil.getJti(token)

        redisCommands.setex(jti, jwtUtil.getExpirationTime(token), "revoked")
        return token
    }

    fun isTokenRevoked(token: String): Boolean {
        val jti = jwtUtil.getJti(token)
        return redisCommands.exists(jti) == 1L
    }

    fun revokeToken(token: String) {
        val jti = jwtUtil.getJti(token)
        redisCommands.del(jti)
    }
}