package me.zoemartin.rubie.core.exceptions;

public class CommandArgumentException extends ReplyError {
    public CommandArgumentException() {
        super();
    }

    public CommandArgumentException(String message) {
        super(message);
    }
}
