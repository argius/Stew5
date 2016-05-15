package stew5;

/**
 * This is a kind of CommandException, only used when "Usage" error occurred.
 */
public final class UsageException extends CommandException {

    public UsageException(String message) {
        super(message);
    }

}
