package com.redhat.service.bridge.infra.exceptions.definitions.platform;

public class OidcTokensNotInitializedException extends InternalPlatformException {

    public OidcTokensNotInitializedException(String message) {
        super(message);
    }

    public OidcTokensNotInitializedException(String message, Throwable cause) {
        super(message, cause);
    }
}
