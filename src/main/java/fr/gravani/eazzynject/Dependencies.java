package fr.gravani.eazzynject;

import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * List of the types of the dependencies registered into the container
 */
public class Dependencies {
    /**
     * Registered dependencies
     */
    private final List<ImplementationsLink> dependencies = new ArrayList<>();

    /**
     * Adds a new type to the list of dependencies
     * @param base The base abstract type of the dependency (interface or abstract class)
     * @param child The type of the implementation
     * @param tag The tag of the implementation
     * @throws ImplementationAmbiguityException If the tag of implementation already exists
     */
    public void put(Class<?> base, Class<?> child, String tag) throws ImplementationAmbiguityException {
        boolean found = false;
        for(ImplementationsLink implementationsLink : dependencies) {
            if(implementationsLink.getBaseClass().equals(base)) {
                implementationsLink.addImplementation(tag, child);
                found = true;
            }
        }

        if(!found) {
            dependencies.add(new ImplementationsLink(base, tag, child));
        }
    }

    /**
     * Finds the type from an abstract type (interface or abstract class)
     * @param baseClass The base type
     * @param tag The tag used to distinguish between implementations
     * @return The implementation if it has been found
     * @throws ImplementationNotFoundException Thrown if an implementation could not been found
     * @throws ImplementationAmbiguityException Thrown if we cannot distinguish between two or more implementations
     */
    public Class<?> findImplementationFromBaseClass(Class<?> baseClass, String tag)
            throws ImplementationNotFoundException, ImplementationAmbiguityException {

        for(ImplementationsLink implementationsLink : dependencies) {
            if(implementationsLink.getBaseClass().equals(baseClass)) {
                if(tag == null) {
                    if(implementationsLink.getImplementations().size() == 1) {
                        return implementationsLink
                                .getImplementations()
                                .entrySet()
                                .iterator()
                                .next()
                                .getValue();
                    } else {
                        // We cannot chose between the implementations
                        throw new ImplementationAmbiguityException(
                                String.format("Found %s conflicting tags for type %s",
                                        implementationsLink.getImplementations().size(), baseClass.getName()));
                    }
                } else {
                    if(implementationsLink.getImplementations().containsKey(tag)) {
                        return implementationsLink.getImplementations().get(tag);
                    }
                    // There's a tag but no implementation uses it
                    else {
                        throw new ImplementationNotFoundException(
                                String.format(
                                        "Tag not found even though found %d different implementations for base type %s",
                                        implementationsLink.getImplementations().size(), baseClass.getName()));
                    }
                }

            }
        }
        throw new ImplementationNotFoundException(
                String.format("Could not find any implementation for base type %s", baseClass.getName()));
    }
}
