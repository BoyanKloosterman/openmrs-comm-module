package nl.openmrs.comm_module.common.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionServiceTest {

    private AesEncryptionService encryptionService;
    private AesEncryptionService wrongKeyService;

    @BeforeEach
    void setUp() {
        // 32-byte key
        String key = "this-is-a-strong-32-byte-key!!";
        String wrongKey = "this-is-a-wrong-32-byte-key!!";
        encryptionService = new AesEncryptionService(key);
        wrongKeyService = new AesEncryptionService(wrongKey);
    }

    @Test
    void encryptDecrypt_shouldReturnOriginalText() {
        String originalText = "This is a secret message.";
        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void encrypt_shouldHandleNullInput() {
        assertNull(encryptionService.encrypt(null));
    }

    @Test
    void decrypt_shouldHandleNullInput() {
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void decrypt_shouldThrowExceptionForInvalidCipherText() {
        String invalidCipherText = "this is not a valid ciphertext";
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt(invalidCipherText);
        });
    }

    @Test
    void decrypt_shouldThrowExceptionForWrongKey() {
        String originalText = "This is another secret.";
        String encrypted = encryptionService.encrypt(originalText);

        assertThrows(RuntimeException.class, () -> {
            wrongKeyService.decrypt(encrypted);
        });
    }

    @Test
    void constructor_shouldThrowExceptionForInvalidKeyLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AesEncryptionService("short-key");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new AesEncryptionService(null);
        });
    }
}
