package com.dreamsforge;

/**
 * Abstraction for collection of OTP tokens.
 *
 * @author cemp@google.com (Cem Paya)
 */
public interface OtpSource {

    /**
     * Return the next OTP code for specified username.
     * Invoking this function may change internal state of the OTP generator,
     * for example advancing the counter.
     *
     * @param accountName Username, email address or other unique identifier for the account.
     * @return OTP as string code.
     */
    String getNextCode(String accountName) throws OtpSourceException;

}
