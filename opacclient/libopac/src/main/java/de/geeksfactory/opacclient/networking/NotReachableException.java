package de.geeksfactory.opacclient.networking;

import java.net.SocketException;

/**
 * Raised when it is not possbile to get an appropriate answer from the OPAC server. This can
 * have a wide number of reasons.
 */
public class NotReachableException extends SocketException {

    private static final long serialVersionUID = 9209411947611368678L;

    public NotReachableException(String msg) {
        super(msg);
    }

    public NotReachableException() {

    }
}
