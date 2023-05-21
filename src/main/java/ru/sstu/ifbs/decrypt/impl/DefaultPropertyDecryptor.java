package ru.sstu.ifbs.decrypt.impl;

import org.springframework.util.CollectionUtils;
import ru.sstu.ifbs.decrypt.PropertyDecryptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.sstu.ifbs.util.DecryptConstants.DECRYPT_MARKER;
import static ru.sstu.ifbs.util.DecryptConstants.ISO_CHARSET;

public class DefaultPropertyDecryptor extends DefaultDecryptor implements PropertyDecryptor {
    private final static Logger logger = Logger.getLogger(DefaultDecryptor.class.getName());
    private String secretKey;
    private String algorithm;

    public DefaultPropertyDecryptor(String secretKey, String algorithm) {
        this.secretKey = secretKey;
        this.algorithm = algorithm;
    }

    @Override
    public Map.Entry<String, String> decryptProperty(Map.Entry<String, Object> encryptedPropertyEntry) {
        if (encryptedPropertyEntry == null) {
            return null;
        }
        if (!encryptedPropertyEntry.getValue().toString().startsWith(DECRYPT_MARKER)) {
            return Map.entry(encryptedPropertyEntry.getKey(), encryptedPropertyEntry.getValue().toString());
        }
        try {
            final byte[] encryptedProperty = encryptedPropertyEntry.getValue().toString().substring(DECRYPT_MARKER.length()).getBytes(ISO_CHARSET);
            final byte[] decryptedProperty = decrypt(encryptedProperty, algorithm, secretKey.getBytes(ISO_CHARSET));
            return Map.entry(encryptedPropertyEntry.getKey(), new String(decryptedProperty, ISO_CHARSET));
        } catch (Exception e) {
            logger.warning("Got error during decrypting an encrypted property ");
            return Map.entry(encryptedPropertyEntry.getKey(), encryptedPropertyEntry.getValue().toString());
        }
    }

    @Override
    public List<Map.Entry<String, String>> decryptProperties(List<Map.Entry<String, Object>> encryptedProperties) {
        if (CollectionUtils.isEmpty(encryptedProperties)) {
            return Collections.unmodifiableList(new ArrayList<>());
        }
        return encryptedProperties.stream().map(encryptedProperty -> {
            try {
                return decryptProperty(encryptedProperty);
            } catch (Exception e) {
                throw new RuntimeException("Got unexpected error during decryption: " + e);
            }
        }).collect(Collectors.toList());
    }
}
