package fr.gravani.eazzynject.exceptions;

/**
 * Exception thrown when registering more than one implementation without tag or with the same tag
 */
public class ImplementationAmbiguityException extends Exception {
    /**
     * Constructor
     * @param message Exception message
     */
    public ImplementationAmbiguityException(String message) {
        super(message);
    }
}
