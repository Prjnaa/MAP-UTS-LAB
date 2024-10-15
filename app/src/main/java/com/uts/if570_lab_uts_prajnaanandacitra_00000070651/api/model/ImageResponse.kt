package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.api

data class ImageResponse(
    val id: String,
    val urls: Urls,
)

data class Urls(
    val small: String,
    val regular: String,
    val full: String
)
