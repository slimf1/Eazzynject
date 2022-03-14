package fr.gravani.eazzynject.exceptions;

/**
 * Exception thrown when the implementation of an object cannot be found
 */
public class ImplementationNotFoundException extends Exception {
    /**
     * Constructor
     * @param message Exception message
     */
    public ImplementationNotFoundException(String message) {
        super(message);
    }
}
