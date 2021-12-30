package com.mlacker.samples.provider.api.client

import com.mlacker.samples.provider.api.model.UserModel
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(name = "provider/users")
interface UserClient {

    @GetMapping
    fun getUsers(): List<UserModel>
}