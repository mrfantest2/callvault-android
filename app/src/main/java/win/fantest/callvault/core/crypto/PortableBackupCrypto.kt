package win.fantest.callvault.core.crypto

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class PortableBackupCrypto {
    fun encrypt(source: File, destination: File, passphrase: CharArray) {
        require(passphrase.size >= 6) { "Backup passphrase must be at least 6 characters" }

        destination.parentFile?.mkdirs()
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(IV_BYTES).also(secureRandom::nextBytes)
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))

        DataOutputStream(BufferedOutputStream(destination.outputStream())).use { header ->
            header.writeInt(MAGIC)
            header.writeInt(VERSION)
            header.writeInt(ITERATIONS)
            header.writeInt(salt.size)
            header.write(salt)
            header.writeInt(iv.size)
            header.write(iv)
            header.flush()

            CipherOutputStream(header, cipher).use { encrypted ->
                BufferedInputStream(source.inputStream()).use { input ->
                    input.copyTo(encrypted)
                }
            }
        }
    }

    fun decrypt(source: File, destination: File, passphrase: CharArray) {
        destination.parentFile?.mkdirs()

        DataInputStream(BufferedInputStream(source.inputStream())).use { header ->
            require(header.readInt() == MAGIC) { "Not a CallVault encrypted backup" }
            require(header.readInt() == VERSION) { "Unsupported backup version" }

            val iterations = header.readInt()
            val salt = ByteArray(header.readInt()).also(header::readFully)
            val iv = ByteArray(header.readInt()).also(header::readFully)
            val key = deriveKey(passphrase, salt, iterations)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))

            CipherInputStream(header, cipher).use { decrypted ->
                BufferedOutputStream(destination.outputStream()).use { output ->
                    decrypted.copyTo(output)
                }
            }
        }
    }

    private fun deriveKey(
        passphrase: CharArray,
        salt: ByteArray,
        iterations: Int = ITERATIONS
    ): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    companion object {
        private const val MAGIC = 0x4356424B
        private const val VERSION = 1
        private const val ITERATIONS = 210_000
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val KEY_BITS = 256
        private const val TAG_BITS = 128
        private const val KDF = "PBKDF2WithHmacSHA256"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val secureRandom = SecureRandom()
    }
}
