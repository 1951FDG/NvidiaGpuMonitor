/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.pp.msk.openvpnstatus.exceptions;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings("SerializableHasSerializationMethods")
public class OpenVpnParseException extends Exception {

    private static final long serialVersionUID = -3387516993124229948L;

    public OpenVpnParseException() {
    }

    public OpenVpnParseException(String message) {
        super(message);
    }

    public OpenVpnParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
