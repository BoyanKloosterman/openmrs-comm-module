package nl.openmrs.comm_module.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import nl.openmrs.comm_module.common.BeanUtil;
import nl.openmrs.comm_module.common.encryption.EncryptionService;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return BeanUtil.getBean(EncryptionService.class).encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return BeanUtil.getBean(EncryptionService.class).decrypt(dbData);
    }
}


