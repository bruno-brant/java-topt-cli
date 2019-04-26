package com.dreamsforge;
/*
 * Copyright 2010 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.security.GeneralSecurityException;

/**
 * Class containing implementation of HOTP/TOTP.
 * Generates OTP codes for one or more accounts.
 *
 * @author Steve Weis (sweis@google.com)
 * @author Cem Paya (cemp@google.com)
 */
public class OtpProvider implements OtpSource {

    private static final int PIN_LENGTH = 6; // HOTP or TOTP

    @Override
    public String getNextCode(String accountName) throws OtpSourceException {
        return getCurrentCode(accountName);
    }

    private String getCurrentCode(String username) throws OtpSourceException {
        if (username == null) {
            throw new OtpSourceException("No account name");
        }

        AccountDb.OtpType type = mAccountDb.getType(username);
        String secret = getSecret(username);

        long otp_state = 0;

        if (type == AccountDb.OtpType.TOTP) {
            // For time-based OTP, the state is derived from clock.
            otp_state =
                    mTotpCounter.getValueAtTime(Utilities.millisToSeconds(mTotpClock.currentTimeMillis()));
        } else if (type == AccountDb.OtpType.HOTP) {
            // For counter-based OTP, the state is obtained by incrementing stored counter.
            mAccountDb.incrementCounter(username);
            Integer counter = mAccountDb.getCounter(username);
            otp_state = counter.longValue();
        }

        return computePin(secret, otp_state);
    }

    OtpProvider(AccountDb accountDb, TotpClock totpClock) {
        this(DEFAULT_INTERVAL, accountDb, totpClock);
    }

    private OtpProvider(int interval, AccountDb accountDb, TotpClock totpClock) {
        mAccountDb = accountDb;
        mTotpCounter = new TotpCounter(interval);
        mTotpClock = totpClock;
    }

    /**
     * Computes the one-time PIN given the secret key.
     *
     * @param secret    the secret key
     * @param otp_state current token state (counter or time-interval)
     * @return the PIN
     */
    private String computePin(String secret, long otp_state)
            throws OtpSourceException {
        if (secret == null || secret.length() == 0) {
            throw new OtpSourceException("Null or empty secret");
        }

        try {
            PasscodeGenerator.Signer signer = AccountDb.getSigningOracle(secret);
            PasscodeGenerator pcg = new PasscodeGenerator(signer,
                    PIN_LENGTH);

            return pcg.generateResponseCode(otp_state);
        } catch (GeneralSecurityException e) {
            throw new OtpSourceException("Cryptography failure", e);
        }
    }

    /**
     * Reads the secret key that was saved on the phone.
     *
     * @param user com.dreamsforge.Account name identifying the user.
     * @return the secret key as base32 encoded string.
     */
    private String getSecret(String user) {
        return mAccountDb.getSecret(user);
    }

    /**
     * Default passcode timeout period (in seconds)
     */
    private static final int DEFAULT_INTERVAL = 30;

    private final AccountDb mAccountDb;

    /**
     * Counter for time-based OTPs (TOTP).
     */
    private final TotpCounter mTotpCounter;

    /**
     * Clock input for time-based OTPs (TOTP).
     */
    private final TotpClock mTotpClock;
}
