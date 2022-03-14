package fr.gravani.eazzynject.exceptions;

/**
 * Exception thrown when detecting a cyclic dependency
 */
public class CyclicDependenciesException extends Exception {
    /**
     * Constructor
     * @param message exception message
     */
    public CyclicDependenciesException(String message) {
        super(message);
    }
}
