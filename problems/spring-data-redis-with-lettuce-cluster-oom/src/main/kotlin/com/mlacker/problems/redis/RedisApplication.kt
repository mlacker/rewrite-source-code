package com.mlacker.problems.redis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@RestController
class RedisApplication(
    private val template: RedisTemplate<String, String>
) {

    @GetMapping("/users/{id}")
    fun getUser(@PathVariable id: Long): String? {
        val user = template.opsForValue().get(userKey(id))
        return user
    }

    @PostMapping("/users")
    fun setUser(@RequestBody user: User) {
        template.opsForValue().set(userKey(user.id), user.name)
    }

    private fun userKey(userId: Long): String = "auth.users:{$userId}"
}

fun main(args: Array<String>) {
    runApplication<RedisApplication>(*args)
}