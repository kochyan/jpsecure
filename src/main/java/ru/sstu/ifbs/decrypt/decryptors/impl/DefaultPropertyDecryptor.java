package ru.sstu.ifbs.decrypt.decryptors.impl;

import org.springframework.util.CollectionUtils;
import ru.sstu.ifbs.decrypt.decryptors.PropertyDecryptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.sstu.ifbs.decrypt.util.DecryptConstants.DECRYPT_MARKER;
import static ru.sstu.ifbs.decrypt.util.DecryptConstants.ISO_CHARSET;

public class DefaultPropertyDecryptor extends DefaultDecryptor implements PropertyDecryptor {
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
        try {
            final byte[] encryptedProperty = encryptedPropertyEntry.getValue().toString().substring(DECRYPT_MARKER.length()).getBytes(ISO_CHARSET);
            final byte[] decryptedProperty = decrypt(encryptedProperty, algorithm, secretKey.getBytes(ISO_CHARSET));
            return Map.entry(encryptedPropertyEntry.getKey(), new String(decryptedProperty, ISO_CHARSET));
        } catch (Exception e) {
            throw new RuntimeException(e);
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
