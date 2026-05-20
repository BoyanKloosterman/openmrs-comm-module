package nl.openmrs.comm_module.common.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PgCryptoService {

    private final JdbcTemplate jdbcTemplate;
    private final String encryptionKey;

    public PgCryptoService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.encryption.key}") String encryptionKey
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionKey = encryptionKey;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }

        return jdbcTemplate.queryForObject(
                "SELECT encode(pgp_sym_encrypt(?, ?), 'base64')",
                String.class,
                plainText,
                encryptionKey
        );
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }

        return jdbcTemplate.queryForObject(
                "SELECT pgp_sym_decrypt(decode(?, 'base64'), ?)",
                String.class,
                encryptedText,
                encryptionKey
        );
    }
}