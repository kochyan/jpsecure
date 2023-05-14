package ru.sstu.ifbs.decrypt.decryptors;

import java.util.List;
import java.util.Map;

public interface PropertyDecryptor {
    Map.Entry<String, String> decryptProperty(Map.Entry<String, Object> encryptedPropertyEntry);

    List<Map.Entry<String, String>> decryptProperties(List<Map.Entry<String, Object>> encryptedProperties);
}
