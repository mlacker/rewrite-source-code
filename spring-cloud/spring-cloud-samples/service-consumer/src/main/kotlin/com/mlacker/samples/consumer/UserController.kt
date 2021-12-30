package com.mlacker.samples.consumer

import com.mlacker.samples.provider.api.client.UserClient
import com.mlacker.samples.provider.api.model.UserModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userClient: UserClient
) {

    @GetMapping
    fun getUsers(): List<UserModel> {
        return userClient.getUsers()
    }
}