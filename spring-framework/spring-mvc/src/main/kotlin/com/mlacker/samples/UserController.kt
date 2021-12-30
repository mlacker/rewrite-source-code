package com.mlacker.samples

import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.noContent
import org.springframework.http.ResponseEntity.notFound
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController {

    private val users: MutableList<UserDto> = mutableListOf()

    @GetMapping
    fun findAll(@RequestParam size: Int = 10): List<UserDto> {
        return users.take(size)
    }

    @GetMapping("{id}")
    fun find(@PathVariable id: Long): UserDto? = findInternal(id)

    @PostMapping
    fun create(@RequestBody model: UserModel): UserDto {
        val user = UserDto((users.size + 1).toLong(), model.name)

        users.add(user)

        return user
    }

    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @RequestBody model: UserModel): ResponseEntity<UserDto> {
        return findInternal(id)?.also { it.name = model.name }?.let { noContent().build() } ?: notFound().build()
    }

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<UserDto> {
        val user = findInternal(id) ?: return notFound().build()
        users.remove(user)
        return noContent().build()
    }

    private fun findInternal(id: Long): UserDto? {
        return users.singleOrNull { it.id == id }
    }
}

