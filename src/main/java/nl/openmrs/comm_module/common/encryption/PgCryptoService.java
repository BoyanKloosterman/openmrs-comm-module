package nl.openmrs.comm_module.common.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class PgCryptoService {

    private final JdbcTemplate jdbcTemplate;
    private final String encryptionKey;

    public PgCryptoService(
            DataSource dataSource,
            @Value("${app.encryption.key}") String encryptionKey
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.encryptionKey = encryptionKey;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }

        return jdbcTemplate.queryForObject(
                "SELECT encode(pgp_sym_encrypt(?::text, ?::text), 'base64')",
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
                "SELECT pgp_sym_decrypt(decode(?::text, 'base64'), ?::text)",
                String.class,
                encryptedText,
                encryptionKey
        );
    }
}