package database;

/**
 * A General exception class to wrap any problem that may occur when interacting with the DataSource object. Should
 * whenever possible, be created with the constructor that takes a Throwable which is the original exception object
 * To aid debugging.
 */
public class DatabaseException extends Exception{
    public DatabaseException() {
        super();
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super (message, cause);
    }
}
