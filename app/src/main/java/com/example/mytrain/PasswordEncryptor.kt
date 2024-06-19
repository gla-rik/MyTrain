package com.example.mytrain

import org.mindrot.jbcrypt.BCrypt

object PasswordEncryptor {
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
}
