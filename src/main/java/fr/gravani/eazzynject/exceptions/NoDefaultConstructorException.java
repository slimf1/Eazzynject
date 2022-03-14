package fr.gravani.eazzynject.exceptions;

/**
 * Exception thrown when a class is missing a default constructor
 */
public class NoDefaultConstructorException extends Exception {
    /**
     * Constructor
     * @param message Exception message
     */
    public NoDefaultConstructorException(String message) {
        super(message);
    }
}
