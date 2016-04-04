package de.geeksfactory.opacclient.networking;

/**
 * Raised when a security problem arised during the connection to the OPAC server.
 */
public class SSLSecurityException extends NotReachableException {

    private static final long serialVersionUID = 2959046371699876752L;


    public SSLSecurityException(String msg) {
        super(msg);
    }

    public SSLSecurityException() {

    }
}
