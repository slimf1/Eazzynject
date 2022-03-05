package fr.gravani.eazzynject;

import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Dependencies {
    @Getter
    private List<ImplementationsLink> dependencies;

    public Dependencies() {
        dependencies = new ArrayList<>();
    }

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

    public Class<?> findImplementationFromBaseClass(Class<?> baseClass, String tag) throws ImplementationNotFoundException, ImplementationAmbiguityException {
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
                        throw new ImplementationAmbiguityException(
                                String.format("Found %s conflicting tags for type %s",
                                        implementationsLink.getImplementations().size(), baseClass.getName()));
                    }
                } else {
                    if(implementationsLink.getImplementations().containsKey(tag)) {
                        return implementationsLink.getImplementations().get(tag);
                    }
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
