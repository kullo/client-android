// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from errors.djinni

package net.kullo.libkullo.api;

public enum NetworkError {
    /** Client too old or otherwise blocked; should check for update */
    FORBIDDEN,
    /** Client and server protocol incompatible; should check for update */
    PROTOCOL,
    /** Bad credentials (address + master key) */
    UNAUTHORIZED,
    /** Server-side error */
    SERVER,
    /** Network connection couldn't be established */
    CONNECTION,
    /** Any other error */
    UNKNOWN,
    ;
}
