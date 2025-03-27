package utils;

import java.util.UUID;

public class TokenGenerator {
    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20); // Role de cette ligne ?
    }
}
