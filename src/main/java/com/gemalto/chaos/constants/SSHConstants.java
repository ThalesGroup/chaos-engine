package com.gemalto.chaos.constants;

import java.math.BigInteger;

public class SSHConstants {
    public static final int DEFAULT_SSH_PORT = 22;
    public static final String TEMP_DIRECTORY = "/tmp/";
    public static final BigInteger DEFAULT_RSA_PUBLIC_EXPONENT = BigInteger.valueOf(65537);
    public static final int THIRTY_SECONDS_IN_MILLIS = 1000 * 30;

    private SSHConstants () {
    }
}
