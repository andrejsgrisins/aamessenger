// Copyright (c) 2023 Andrejs Grišins, Anastasia Petrova. Unauthorized use prohibited.
package services

import org.apache.commons.validator.routines.EmailValidator
import java.security.MessageDigest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.util.*
import org.mindrot.jbcrypt.BCrypt
import config.AuthConfig
import repositories.UserRepository
import services.TokenService
import util.JwtUtil
import models.User

class AuthService(
    private val userRepository: UserRepository,
    private val config: AuthConfig,
    private val tokenService: TokenService,
    private val jwtUtil: JwtUtil
) {
    fun login(usernameOrEmail: String, password: String): String {
        val user = userRepository.findByUsernameOrEmail(usernameOrEmail)
         ?: return ""
        if (!verifyPassword(user.passwordHash!!, password)) {
            return ""
        }

        return tokenService.generateToken(user.username)
    }

    fun logout(token: String) {
        tokenService.revokeToken(token)
    }

    fun validateToken(token: String) {
        if(tokenService.isTokenRevoked(token)) {
            throw IllegalArgumentException("Token is revoked")
        }

        jwtUtil.validateToken(token)
    }

    fun registerUser(username: String, email: String, password: String): Boolean {
        validateInputs(username, email, password)

        if (userRepository.findByUsernameOrEmail(username) != null) {
            throw IllegalArgumentException("Username already taken")
        }
        if (userRepository.findByUsernameOrEmail(email) != null) {
            throw IllegalArgumentException("Email already registered")
        }
        
        val hashedPassword = hashPassword(password)
        
        return try {
            transaction {
                userRepository.insertUser(username, email, hashedPassword, isGuest = false)
            }
            true
        } catch (e: Exception) {
            if (e.message?.contains("duplicate key") == true) {
                throw IllegalArgumentException("Username or email already exists")
            }
            throw e
        }
    }

    fun registerGuest(guestUsername: String): Boolean {
        if (guestUsername.length < 3 || guestUsername.length > 20) {
            throw IllegalArgumentException("Guest username must be between 3 and 20 characters")
        }
        
        return try {
            transaction {
                userRepository.insertUser(
                    username = guestUsername, 
                    email = null, 
                    hashedPassword = null, 
                    isGuest = true
                )
            }
            true
        } catch (e: Exception) {
            if (e.message?.contains("duplicate key") == true) {
                throw IllegalArgumentException("Guest username already exists")
            }
            throw e
        }
    }

    private fun validateInputs(username: String, email: String, password: String) {
        if (username.isBlank()) throw IllegalArgumentException("Username cannot be empty")
        if (email.isBlank()) throw IllegalArgumentException("Email cannot be empty")
        if (password.length < 8) throw IllegalArgumentException("Password must be at least 8 characters")

        if (!EmailValidator.getInstance().isValid(email)) {
            throw IllegalArgumentException("Invalid email format")
        }

        if (password.length < 12) {
            throw IllegalArgumentException("Password must be at least 12 characters long")
        }

        if (password.all {it.isLetterOrDigit()}) {
            throw IllegalArgumentException("Password must contain at least one special character")
        }

        if (!password.any { it.isUpperCase() } || !password.any { it.isLowerCase() }) {
            throw IllegalArgumentException("Password must contain both uppercase and lowercase letters")
        }
    }

    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(12))
    }

    private fun verifyPassword(storedHash: String, inputPassword: String): Boolean {
        return BCrypt.checkpw(inputPassword, storedHash)
    }
}