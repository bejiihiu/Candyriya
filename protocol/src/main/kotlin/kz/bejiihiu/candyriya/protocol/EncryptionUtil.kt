package kz.bejiihiu.candyriya.protocol

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

public object EncryptionUtil {
    private const val RSA_KEY_SIZE = 1024

    public fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(RSA_KEY_SIZE)
        return gen.generateKeyPair()
    }

    public fun decryptRsa(privateKey: java.security.PrivateKey, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(data)
    }

    public fun getCipher(opmode: Int, sharedSecret: ByteArray): Cipher {
        val cipher = Cipher.getInstance("AES/CFB8/NoPadding")
        cipher.init(opmode, SecretKeySpec(sharedSecret, "AES"))
        return cipher
    }

    public fun generateServerId(serverId: String, sharedSecret: ByteArray, publicKey: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(serverId.toByteArray(Charsets.ISO_8859_1))
        digest.update(sharedSecret)
        digest.update(publicKey.encoded)
        return minecraftSha1(digest.digest())
    }

    // Copy of Velocity's minecraftSha1 — twos complement hex
    private fun minecraftSha1(digest: ByteArray): String {
        // BigInteger handles sign
        val bi = java.math.BigInteger(digest)
        return bi.toString(16)
    }

    public fun publicKeyFromBytes(bytes: ByteArray): PublicKey {
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }
}
