package com.mlacker.samples.provider.api.model

import com.mlacker.samples.NoArg

@NoArg
data class UserModel(
    val id: Long,
    var name: String,
    val enabled: Boolean
)
