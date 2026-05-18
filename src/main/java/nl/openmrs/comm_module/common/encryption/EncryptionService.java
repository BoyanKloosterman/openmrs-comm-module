package nl.openmrs.comm_module.common.encryption;

public interface EncryptionService {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
