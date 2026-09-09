package com.bitdesal.taskit

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform