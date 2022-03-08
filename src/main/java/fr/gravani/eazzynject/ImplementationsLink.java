package fr.gravani.eazzynject;

import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a link between an implementation and an abstraction (abstract class or interface)
 */
public class ImplementationsLink {
    /**
     * The abstraction
     */
    @Getter
    private final Class<?> baseClass;

    /**
     * All the implementations of the abstraction, mapped with their tag
     */
    @Getter
    private final Map<String, Class<?>> implementations = new HashMap<>();

    /**
     * Creates a new link with a first implementation of the abstraction
     * @param baseClass The abstraction
     * @param tag The tag of the first implementation
     * @param firstImplementation The type of the first implentation
     */
    public ImplementationsLink(Class<?> baseClass, String tag, Class<?> firstImplementation) {
        this.baseClass = baseClass;
        implementations.put(tag, firstImplementation);
    }

    /**
     * Registers a new implementation of an abstraction
     * @param tag The tag of the implementation
     * @param implementation The implementation type
     * @throws ImplementationAmbiguityException If the given tag already exists
     */
    public void addImplementation(String tag, Class<?> implementation) throws ImplementationAmbiguityException {
        if(implementations.containsKey(tag)) {
            throw new ImplementationAmbiguityException("The tag " + tag + " already exists for the base class " + baseClass.getName());
        }
        implementations.put(tag, implementation);
    }
}
