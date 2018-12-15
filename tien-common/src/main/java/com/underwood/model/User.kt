package com.underwood.model

import java.util.*

data class User(val account: String) {
    val id: String = "";
    val email: String = "";
    val mobilePhoneNumber: String = ""
    val password: String = ""
    val nickName: String = ""
    val avatar: String = ""
    val wxOpenID: String = ""
    val age: Int = 0
    val birthDay: Date = Date()
}