package com.dreamsforge;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A database of email addresses and secret values
 *
 * @author sweis@google.com (Steve Weis)
 */
public class AccountDb {

    private final Map<String, Account> mDatabase = new HashMap<>();

    public static final String GOOGLE_CORP_ACCOUNT_NAME = "Google Internal 2Factor";

    public void add(Account account) {
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

        public static OtpType getEnum(Integer i) {
            for (OtpType type : OtpType.values()) {
                if (type.value.equals(i)) {
                    return type;
                }
            }

            return null;
        }
    }

    /*
     * deleteAllData() will remove all rows. Useful for testing.
     */
    public boolean deleteAllData() {
        this.mDatabase.clear();
        return true;
    }

    public boolean nameExists(String email) {
        return mDatabase.containsKey(email);
    }

    public String getSecret(String email) {
        return mDatabase.get(email).secret;
    }

    static PasscodeGenerator.Signer getSigningOracle(String secret) {
        try {
            byte[] keyBytes = decodeKey(secret);
            final Mac mac = Mac.getInstance("HMACSHA1");
            mac.init(new SecretKeySpec(keyBytes, ""));

            // Create a signer object out of the standard Java MAC implementation.
            return new PasscodeGenerator.Signer() {
                @Override
                public byte[] sign(byte[] data) {
                    return mac.doFinal(data);
                }
            };
        } catch (Base32String.DecodingException | NoSuchAlgorithmException | InvalidKeyException error) {
            System.out.println(error.getMessage());
        }

        return null;
    }

    private static byte[] decodeKey(String secret) throws Base32String.DecodingException {
        return Base32String.decode(secret);
    }

    public Integer getCounter(String email) {
        return getAccount(email).counter;
    }

    void incrementCounter(String email) {
        Account account = getAccount(email);
        account.counter++;
    }

    public OtpType getType(String email) {
        Account account = getAccount(email);

        if (account != null) {
            return account.type;
        }

        return null;
    }


    /**
     * Finds the Google corp account in this database.
     *
     * @return the name of the account if it is present or {@code null} if the account does not exist.
     */
    public String findGoogleCorpAccount() {
        return nameExists(GOOGLE_CORP_ACCOUNT_NAME) ? GOOGLE_CORP_ACCOUNT_NAME : null;
    }

    public void delete(String email) {
        mDatabase.remove(email);
    }

    /**
     * Save key to database, creating a new user entry if necessary.
     *
     * @param email    the user email address. When editing, the new user email.
     * @param secret   the secret key.
     * @param oldEmail If editing, the original user email, otherwise null.
     * @param type     hotp vs totp
     * @param counter  only important for the hotp type
     */
    public void update(String email, String secret, String oldEmail,
                       OtpType type, Integer counter) {
        update(email, secret, oldEmail, type, counter, null);
    }

    /**
     * Save key to database, creating a new user entry if necessary.
     *
     * @param email         the user email address. When editing, the new user email.
     * @param secret        the secret key.
     * @param oldEmail      If editing, the original user email, otherwise null.
     * @param type          hotp vs totp
     * @param counter       only important for the hotp type
     * @param googleAccount whether the key is for a Google account or {@code null} to preserve
     *                      the previous value (or use a default if adding a key).
     */
    public void update(String email, String secret, String oldEmail,
                       OtpType type, Integer counter, Boolean googleAccount) {
        Account account = getAccount(email);

        if (account == null) {
            account = new Account();
            mDatabase.put(email, account);
        }

        account.secret = secret;
        account.type = type;
        account.counter = counter;
    }

    private Account getAccount(String email) {
        return mDatabase.get(email);
    }

    /**
     * Get list of all account names.
     *
     * @param result Collection of strings-- account names are appended, without
     *               clearing this collection on entry.
     * @return Number of accounts added to the output parameter.
     */
    public int getNames(Collection<String> result) {

        int nameCount = 0;

        for (Account a : mDatabase.values()) {
            result.add(a.name);
            nameCount++;
        }

        return nameCount;
    }
}
