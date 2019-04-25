package com.dreamsforge;

public class Program {
    public static void main(String[] args) throws OtpSourceException {
        var account = new Account();
        account.secret = args[0];
        account.counter = 0;
        account.email = "any";
        account.name = "any";
        account.type = AccountDb.OtpType.TOTP;

        var accountDb = new AccountDb();
        accountDb.add(account);

        var otpProvider = new OtpProvider(
                accountDb,
                new TotpClock());

        var nextCode  = otpProvider.getNextCode(account.email);
        System.out.println(nextCode);
    }
}