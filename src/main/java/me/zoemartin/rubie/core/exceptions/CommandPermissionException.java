package me.zoemartin.rubie.core.exceptions;

public class CommandPermissionException extends ReplyError {
    public CommandPermissionException() {
        super("Unspecified Permission Error");
    }

    public CommandPermissionException(String message) {
        super(message);
    }

    public CommandPermissionException(String format, Object... args) {
        super(format, args);
    }
}
