package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Tag;
import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;
import fr.gravani.eazzynject.exceptions.NoDefaultConstructorException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Container {
    private final Map<Class<?>, Class<?>> dependencies = new HashMap<>();
    private final Map<Class<?>, Object> instanceCache = new HashMap<>(); // Use @Singleton annotation

    public <T> void registerMapping(Class<? extends T> child, Class<T> base) {
        dependencies.put(child, base);
    }

    public <T> T instantiate(Class<T> inter)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException {
        return instantiate(inter, null);
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Class<T> inter, String tag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException {
        var implementation = getImplementationFromBase(inter, tag);

        if (instanceCache.containsKey(implementation)) {
            return (T)instanceCache.get(implementation);
        }

        var instance = injectIntoClass(implementation);
        instanceCache.put(implementation, instance);

        return (T)instance;
    }

    private <T> T injectIntoClass(Class<T> cls)
            throws NoDefaultConstructorException, ImplementationNotFoundException, ImplementationAmbiguityException {
        try {
            var injectableConstructors = Arrays.stream(cls.getDeclaredConstructors())
                    .filter(c -> c.isAnnotationPresent(Inject.class))
                    .toList();

            // FAIRE UN FOR EACH!! (ou si deux throw une exception ??) 2 bugs -> pas fields + constructor
            // deux méthodes pour fields
            T instance = null;
            if (!injectableConstructors.isEmpty()) {
                instance = injectIntoConstructor(injectableConstructors.stream().findFirst().get());
            }
            if (instance == null) {
                instance = injectIntoFields(cls);
            } else {
                injectIntoFields(cls, instance);
            }

            return instance;
        } catch (NoSuchMethodException e) {
            throw new NoDefaultConstructorException(
                    String.format("Could not find a default constructor or an " +
                            "injectable constructor for the injectable class %s", cls.getName()));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null; // TODO : do more?
        }
    }

    private <T> void injectIntoFields(Class<T> cls, Object instance)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            ReflectiveOperationException {

        for(Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                var tag = field.isAnnotationPresent(Tag.class)
                        ? field.getAnnotation(Tag.class).value() : null;
                field.set(instance, instantiate(field.getType(), tag));
            }
        }
    }

    private <T> T injectIntoFields(Class<T> cls)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException {

        // If an injectable class doesn't have a constructor annotated with @Inject
        // we suppose that it has a default constructor (without parameters)
        var instance = cls.getDeclaredConstructor().newInstance();
        injectIntoFields(cls, instance);
        return instance;
    }

    @SuppressWarnings("unchecked")
    private <T> T injectIntoConstructor(Constructor<?> constructor) throws ReflectiveOperationException {
        var parameters = Arrays.stream(constructor.getParameterTypes())
                .map(t -> {
                    try {
                        var tag = constructor.isAnnotationPresent(Tag.class)
                                ? constructor.getAnnotation(Tag.class).value() : null;
                        return instantiate(t, tag);
                    } catch (ImplementationNotFoundException | NoDefaultConstructorException
                            | ImplementationAmbiguityException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .toArray();
        return (T)constructor.newInstance(parameters);
    }

    private Class<?> getImplementationFromBase(Class<?> baseClass, String tag)
            throws ImplementationNotFoundException, ImplementationAmbiguityException {
        var implementations = dependencies
                .entrySet()
                .stream()
                .filter(item -> item.getValue().equals(baseClass))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (implementations.isEmpty()) {
            throw new ImplementationNotFoundException(
                    String.format("Could not find any implementation for base class %s", baseClass.getName()));
        }

        if (implementations.size() == 1) {
            return implementations.stream().findFirst().get();
        }

        if (tag == null) {
            throw new ImplementationAmbiguityException(
                    String.format(
                            "Tag not found even though found %d different implementations for base type %s",
                            implementations.size(), baseClass.getName()));
        }

        var validImplementations = implementations
                .stream()
                .filter(implementation -> {
                    var implementationTag = implementation.isAnnotationPresent(Tag.class)
                            ? implementation.getAnnotation(Tag.class).value() : null;
                    if (implementationTag != null) {
                        return implementationTag.equals(tag);
                    }
                    return false;
                })
                .toList();

        if (validImplementations.isEmpty()) {
            throw new ImplementationNotFoundException(
                    String.format("Could not find any valid implementation for type %s", baseClass.getName()));
        } else if (validImplementations.size() >= 2) {
            throw new ImplementationAmbiguityException(
                    String.format("Found %s conflicting tags for type %s",
                            validImplementations.size(), baseClass.getName()));
        } else {
            return validImplementations.stream().findFirst().get();
        }
        // https://dev.to/jjbrt/how-to-create-your-own-dependency-injection-framework-in-java-4eaj
        // Le mettre dans le cr/readme
    }
}
