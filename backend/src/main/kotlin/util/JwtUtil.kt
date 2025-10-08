package util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.*
import javax.crypto.SecretKey
import java.security.SecureRandom

class JwtUtil {
    val secretKey: SecretKey = Jwts.SIG.HS256.key().build()
    //private val secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)

    fun generateToken(username: String): String {
        return Jwts.builder()
            .issuer("aamessenger")
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600 * 1000))
            .signWith(secretKey)
            .compact()
    }

    fun getJti(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .toString()
    }

    fun getExpirationTime(token: String): Long {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .expiration
            .time
            .toLong()
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer("aamessenger")
                .build()
                .parseSignedClaims(token)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}