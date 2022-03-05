package fr.gravani.eazzynject.exceptions;

public class CyclicDependenciesException extends Exception {
    public CyclicDependenciesException(String message) {
        super(message);
    }
}
