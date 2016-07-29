package play.libs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import play.Play;
import play.exceptions.UnexpectedException;
import sun.security.jca.GetInstance;

/**
 * Cryptography utils
 */
public class Crypto {

    private static final java.lang.String HMACSHA1_CONST = "HmacSHA1";

    /**
     * Define a hash type enumeration for strong-typing
     */
    public enum HashType {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256"),
        SHA512("SHA-512");
        private String algorithm;
        HashType(String algorithm) { this.algorithm = algorithm; }
        @Override public String toString() { return this.algorithm; }
    }

    /**
     * Set-up MD5 as the default hashing algorithm
     */
    private static final HashType DEFAULT_HASH_TYPE = HashType.MD5;

    static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static Provider provider;
    static {
//        System.out.println("getProviders: "+Providers.getSunProvider().getName());
//        ProviderList list = Providers.getFullProviderList();
//        List<Provider> providerList=list.providers();
//        for (Provider provider1 : providerList) {
//            System.out.println(provider1.getName());
//        }


        List var1 = GetInstance.getServices("Mac", HMACSHA1_CONST);
        Iterator var2 = var1.iterator();

        Provider.Service var3;
        do {
            if(!var2.hasNext()) {
                throw new RuntimeException("Algorithm " + HMACSHA1_CONST + " not available");
            }

            var3 = (Provider.Service)var2.next();
            provider = var3.getProvider();
//            System.out.println(provider.getName());
        } while(searchAppropriateProvider(provider));
    }

    private static boolean searchAppropriateProvider(Provider provider) {
        boolean result = true;
        if(provider.getName().equals("SunJCE")) return false;
        return result;
    }

    public static void main(String[] args) {
        System.out.println("HI: "+provider.getName());
    }
    /**
     * Sign a message using the application secret key (HMAC-SHA1)
     */
    public static String sign(String message) {
        return sign(message, Play.secretKey.getBytes());
    }

    /**
     * Sign a message with a key
     * @param message The message to sign
     * @param key The key to use
     * @return The signed message (in hexadecimal)
     * @throws java.lang.Exception
     */
    public static String sign(String message, byte[] key) {

        if (key.length == 0) {
            return message;
        }

        try {
            Mac mac = Mac.getInstance(HMACSHA1_CONST, provider);
            SecretKeySpec signingKey = new SecretKeySpec(key, HMACSHA1_CONST);
            mac.init(signingKey);
            byte[] messageBytes = message.getBytes("utf-8");
            byte[] result = mac.doFinal(messageBytes);
            int len = result.length;
            char[] hexChars = new char[len * 2];


            for (int charIndex = 0, startIndex = 0; charIndex < hexChars.length;) {
                int bite = result[startIndex++] & 0xff;
                hexChars[charIndex++] = HEX_CHARS[bite >> 4];
                hexChars[charIndex++] = HEX_CHARS[bite & 0xf];
            }
            return new String(hexChars);
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }

    }

    /**
        * Create a password hash using the default hashing algorithm
        * @param input The password
        * @return The password hash
        */
    public static String passwordHash(String input)
    {
        return passwordHash(input, DEFAULT_HASH_TYPE);
    }

    /**
        * Create a password hash using specific hashing algorithm
        * @param input The password
        * @param hashType The hashing algorithm
        * @return The password hash
        */
    public static String passwordHash(String input, HashType hashType)
    {
        try {
            MessageDigest m = MessageDigest.getInstance(hashType.toString());
            byte[] out = m.digest(input.getBytes());
            return new String(Base64.encodeBase64(out));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encrypt a String with the AES encryption standard using the application secret
     * @param value The String to encrypt
     * @return An hexadecimal encrypted string
     */
    public static String encryptAES(String value) {
        return encryptAES(value, Play.configuration.getProperty("application.secret").substring(0, 16));
    }

    /**
     * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
     * @param value The String to encrypt
     * @param privateKey The key used to encrypt
     * @return An hexadecimal encrypted string
     */
    public static String encryptAES(String value, String privateKey) {
        try {
            byte[] raw = privateKey.getBytes();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return Codec.byteToHexString(cipher.doFinal(value.getBytes()));
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }

    /**
     * Decrypt a String with the AES encryption standard using the application secret
     * @param value An hexadecimal encrypted string
     * @return The decrypted String
     */
    public static String decryptAES(String value) {
        return decryptAES(value, Play.configuration.getProperty("application.secret").substring(0, 16));
    }

    /**
     * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
     * @param value An hexadecimal encrypted string
     * @param privateKey The key used to encrypt
     * @return The decrypted String
     */
    public static String decryptAES(String value, String privateKey) {
        try {
            byte[] raw = privateKey.getBytes();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return new String(cipher.doFinal(Codec.hexStringToByte(value)));
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }

}
