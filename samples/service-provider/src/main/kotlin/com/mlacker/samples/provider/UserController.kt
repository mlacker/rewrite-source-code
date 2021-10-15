package com.mlacker.samples.provider

import com.mlacker.samples.provider.api.client.UserClient
import com.mlacker.samples.provider.api.model.UserModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController : UserClient {

    private val users: List<UserModel> = mutableListOf(
        UserModel(1, "test1", true),
        UserModel(2, "test2", true),
        UserModel(3, "test3", true),
    )

    override fun getUsers(): List<UserModel> = users

    @GetMapping("/errors")
    fun errors() {
        println("request is coming..")
        // not retry
        throw RuntimeException("errors")
    }

    @GetMapping("/timeout")
    fun timeout() {
        println("request is coming..")
        Thread.sleep(5000)
    }
}