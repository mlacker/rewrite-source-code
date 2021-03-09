package com.mlacker.samples

data class UserDto(
        val id: Long,
        var name: String
)

@NoArg
data class UserModel(
        val id: Long?,
        val name: String
)

@Target(AnnotationTarget.CLASS)
annotation class NoArg