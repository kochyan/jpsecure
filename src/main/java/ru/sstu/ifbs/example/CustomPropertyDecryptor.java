package ru.sstu.ifbs.example;

import org.springframework.util.CollectionUtils;
import ru.sstu.ifbs.decrypt.decryptors.PropertyDecryptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CustomPropertyDecryptor implements PropertyDecryptor {
    private static Logger logger = Logger.getLogger(CustomPropertyDecryptor.class.getName());

    @Override
    public Map.Entry<String, String> decryptProperty(Map.Entry<String, Object> encryptedPropertyEntry) {
        return Map.entry(encryptedPropertyEntry.getKey(), encryptedPropertyEntry.getValue().toString().substring(6));
    }

    @Override
    public List<Map.Entry<String, String>> decryptProperties(List<Map.Entry<String, Object>> encryptedProperties) {
        logger.info(this.getClass().getName() + " is decrypting properties...");
        if (CollectionUtils.isEmpty(encryptedProperties)) {
            return Collections.unmodifiableList(new ArrayList<>());
        }

        return encryptedProperties.stream().map(this::decryptProperty).collect(Collectors.toList());
    }
}
