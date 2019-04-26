package com.dreamsforge;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * A database of email addresses and secret values
 *
 * @author sweis@google.com (Steve Weis)
 */
class AccountDb {

    private final Map<String, Account> mDatabase = new HashMap<>();

    void add(Account account) {
        mDatabase.put(account.email, account);
    }

    /**
     * Types of secret keys.
     */
    public enum OtpType {  // must be the same as in res/values/strings.xml:type
        TOTP(0),  // time based
        HOTP(1);  // counter based

        public final Integer value;  // value as stored in SQLite database

        OtpType(Integer value) {
            this.value = value;
        }
    }

    String getSecret(String email) {
        return mDatabase.get(email).secret;
    }

    static PasscodeGenerator.Signer getSigningOracle(String secret) {
        try {
            byte[] keyBytes = decodeKey(secret);
            final Mac mac = Mac.getInstance("HMACSHA1");
            mac.init(new SecretKeySpec(keyBytes, ""));

            // Create a signer object out of the standard Java MAC implementation.
            return mac::doFinal;

        } catch (Base32String.DecodingException | NoSuchAlgorithmException | InvalidKeyException error) {
            System.out.println(error.getMessage());
        }

        return null;
    }

    private static byte[] decodeKey(String secret) throws Base32String.DecodingException {
        return Base32String.decode(secret);
    }

    Integer getCounter(String email) {
        return getAccount(email).counter;
    }

    void incrementCounter(String email) {
        Account account = getAccount(email);
        account.counter++;
    }

    OtpType getType(String email) {
        Account account = getAccount(email);

        if (account != null) {
            return account.type;
        }

        return null;
    }


    private Account getAccount(String email) {
        return mDatabase.get(email);
    }

}
