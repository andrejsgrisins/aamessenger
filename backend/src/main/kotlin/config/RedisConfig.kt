// Copyright (c) 2023 Andrejs Grišins, Anastasia Petrova. Unauthorized use prohibited.
package config

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.ClientOptions


class RedisConfig {
    fun redisClient(): RedisClient {
        val client = RedisClient.create("redis://localhost:6379")
        client.setOptions(
            ClientOptions.builder()
            .autoReconnect(true)
            .build()
        )
        return client
    }

    fun redisConnection(redisClient: RedisClient): StatefulRedisConnection<String, String> {
        return redisClient.connect()
    }

    fun redisCommands(connection: StatefulRedisConnection<String, String>): RedisCommands<String, String> {
        return connection.sync()
    }
}