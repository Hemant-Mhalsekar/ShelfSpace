package com.shelfspace.auth;

/** Same message whether the email or the password is wrong, so attackers can't tell which emails exist. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
