package com.studycompanion.app.security

import com.studycompanion.app.core.security.PinVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinVerifierTest {

    @Test
    fun `hashPin generates valid salt and verifier`() {
        val pin = "1234"
        val result = PinVerifier.hashPin(pin)

        assertTrue(result.saltBase64.isNotBlank())
        assertTrue(result.verifierBase64.isNotBlank())
        assertEquals(1, result.version)
    }

    @Test
    fun `verifyPin returns true for matching PIN`() {
        val pin = "5678"
        val result = PinVerifier.hashPin(pin)

        val isValid = PinVerifier.verifyPin(pin, result.saltBase64, result.verifierBase64)
        assertTrue("Expected valid PIN verification", isValid)
    }

    @Test
    fun `verifyPin returns false for incorrect PIN`() {
        val pin = "1234"
        val result = PinVerifier.hashPin(pin)

        val isInvalid = PinVerifier.verifyPin("9999", result.saltBase64, result.verifierBase64)
        assertFalse("Expected incorrect PIN to fail verification", isInvalid)
    }

    @Test
    fun `different salts produce distinct verifiers for the same PIN`() {
        val pin = "1234"
        val result1 = PinVerifier.hashPin(pin)
        val result2 = PinVerifier.hashPin(pin)

        assertNotEquals(result1.saltBase64, result2.saltBase64)
        assertNotEquals(result1.verifierBase64, result2.verifierBase64)

        // Both verify correctly against their respective salts
        assertTrue(PinVerifier.verifyPin(pin, result1.saltBase64, result1.verifierBase64))
        assertTrue(PinVerifier.verifyPin(pin, result2.saltBase64, result2.verifierBase64))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hashPin throws on blank PIN`() {
        PinVerifier.hashPin("")
    }

    @Test
    fun `isValidPinFormat enforces 4 to 6 numeric digits`() {
        assertTrue(PinVerifier.isValidPinFormat("1234"))
        assertTrue(PinVerifier.isValidPinFormat("12345"))
        assertTrue(PinVerifier.isValidPinFormat("123456"))

        assertFalse(PinVerifier.isValidPinFormat("123")) // Too short
        assertFalse(PinVerifier.isValidPinFormat("1234567")) // Too long
        assertFalse(PinVerifier.isValidPinFormat("12a4")) // Non-digit
        assertFalse(PinVerifier.isValidPinFormat("")) // Empty
    }
}
