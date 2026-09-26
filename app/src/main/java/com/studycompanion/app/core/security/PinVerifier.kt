package com.studycompanion.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Result of creating a new PIN verifier.
 */
data class PinVerifierResult(
    val saltBase64: String,
    val verifierBase64: String,
    val version: Int = 1
)

/**
 * Cryptographic PIN/Password verifier implementing PBKDF2WithHmacSHA256.
 * Guarantees:
 * - Zero plaintext storage of PINs/passwords
 * - Cryptographic salt per profile
 * - Constant-time comparison to prevent timing attacks
 * - Iteration count meeting security standards
 */
object PinVerifier {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    /**
     * Checks if PIN format is valid (4 to 6 numeric digits).
     */
    fun isValidPinFormat(pin: String): Boolean {
        return pin.length in 4..6 && pin.all { it.isDigit() }
    }

    /**
     * Hashes a new PIN with a freshly generated cryptographically secure salt.
     */
    fun hashPin(pin: String): PinVerifierResult {
        require(pin.isNotBlank()) { "PIN cannot be blank" }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)

        val hash = pbkdf2(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)

        return PinVerifierResult(
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            verifierBase64 = Base64.getEncoder().encodeToString(hash),
            version = 1
        )
    }

    /**
     * Verifies an input PIN against stored salt and verifier using constant-time comparison.
     */
    fun verifyPin(inputPin: String, saltBase64: String, verifierBase64: String): Boolean {
        if (inputPin.isBlank() || saltBase64.isBlank() || verifierBase64.isBlank()) {
            return false
        }

        return try {
            val salt = Base64.getDecoder().decode(saltBase64)
            val expectedHash = Base64.getDecoder().decode(verifierBase64)

            val actualHash = pbkdf2(inputPin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)

            // MessageDigest.isEqual provides constant-time comparison against timing attacks
            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (e: Exception) {
            false
        }
    }

    private fun pbkdf2(
        chars: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyLength: Int
    ): ByteArray {
        val spec = PBEKeySpec(chars, salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}
