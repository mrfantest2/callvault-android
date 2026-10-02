package win.fantest.callvault.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.file.Files

class PortableBackupCryptoTest {
    @Test
    fun roundTripPreservesBytes() {
        val dir = Files.createTempDirectory("callvault_crypto_test").toFile()
        val source = dir.resolve("source.m4a")
        val encrypted = dir.resolve("source.cvbackup")
        val restored = dir.resolve("restored.m4a")
        val expected = ByteArray(8192) { index -> (index % 251).toByte() }
        source.writeBytes(expected)

        val crypto = PortableBackupCrypto()
        val passphrase = "test-passphrase".toCharArray()

        crypto.encrypt(source, encrypted, passphrase)
        crypto.decrypt(encrypted, restored, passphrase)

        assertArrayEquals(expected, restored.readBytes())
        dir.deleteRecursively()
    }
}
