package ru.sstu.ifbs.decrypt.decryptors.impl;

import ru.sstu.ifbs.decrypt.decryptors.Decryptor;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class DefaultDecryptor implements Decryptor {
    @Override
    public byte[] decrypt(byte[] encrypted, String algorithm, byte[] secretKey) {
        return decrypt(encrypted, new SecretKeySpec(secretKey, algorithm));
    }

    @Override
    public byte[] decrypt(byte[] encrypted, Key key) {
        try {
            final Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
