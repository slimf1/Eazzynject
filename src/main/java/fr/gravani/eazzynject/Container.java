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
    private final Map<Class<?>, Object> instanceCache = new HashMap<>(); // Toujours en créer des nouvelles

    public <T> void registerMapping(Class<? extends T> child, Class<T> base) {
        dependencies.put(child, base);
    }

    public <T> T instantiate(Class<T> inter)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException {
        return instantiate(inter, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T instantiate(Class<T> inter, String tag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException {
        var implementation = getImplementationFromBase(inter, tag);

        if (instanceCache.containsKey(implementation)) {
            return (T)instanceCache.get(implementation);
        }

        var instance = injectIntoClass(implementation);
        instanceCache.put(implementation, instance);

        return (T)instance;
    }

    public <T> T injectIntoClass(Class<T> cls)
            throws NoDefaultConstructorException, ImplementationNotFoundException, ImplementationAmbiguityException {
        try {
            var injectableConstructors = Arrays.stream(cls.getDeclaredConstructors())
                    .filter(c -> c.isAnnotationPresent(Inject.class))
                    .toList();

            if (!injectableConstructors.isEmpty()) {
                return injectIntoConstructor(injectableConstructors.stream().findFirst().get());
            } else {
                // Check ici le constructor par défaut
                return injectIntoFields(cls);
            }
        } catch (NoSuchMethodException e) {
            throw new NoDefaultConstructorException(
                    String.format("Could not find a default constructor or an " +
                            "injectable constructor for the injectable class %s", cls.getName()));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null; // TODO : faire plus?
        }
    }

    private <T> T injectIntoFields(Class<T> cls)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException {
        // Dans le cas où la classe n'a pas de constructeur avec @Inject
        // on suppose qu'elle a un constructeur par défaut rpésent
        var instance = cls.getDeclaredConstructor().newInstance();
        for(Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                var tag = field.isAnnotationPresent(Tag.class)
                        ? field.getAnnotation(Tag.class).value() : null;
                field.set(instance, instantiate(field.getType(), tag));
            }
        }
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

    // TODO: passer par un tag
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
