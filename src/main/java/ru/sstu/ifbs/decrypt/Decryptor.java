package ru.sstu.ifbs.decrypt;

import java.security.Key;

public interface Decryptor {
    byte[] decrypt(byte[] encrypted, String algorithm, byte[] secretKey);

    byte[] decrypt(byte[] encrypted, Key key);
}
