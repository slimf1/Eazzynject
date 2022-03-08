package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Inject;
import fr.gravani.eazzynject.annotations.Singleton;
import fr.gravani.eazzynject.annotations.Tag;
import fr.gravani.eazzynject.exceptions.CyclicDependenciesException;
import fr.gravani.eazzynject.exceptions.ImplementationAmbiguityException;
import fr.gravani.eazzynject.exceptions.ImplementationNotFoundException;
import fr.gravani.eazzynject.exceptions.NoDefaultConstructorException;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The dependency container
 */
public class Container {
    /**
     * Max recursive calls for the instantiate method.
     */
    private static final int MAX_RECURSIVE_INJECTIONS = 32;

    /**
     * Mappings to link the implementations with the interfaces.
     */
    private final Dependencies dependencies = new Dependencies();

    /**
     * Class instances for the singletons.
     */
    private final Map<Class<?>, Object> instanceCache = new HashMap<>();

    /**
     * Count of recursive calls of the instantiate method.
     */
    private final Map<Class<?>, Integer> injectionCounter = new HashMap<>();

    /**
     * Registers a type into the dependency container.
     * @param child The implementation type
     * @param base The base type
     * @throws ImplementationAmbiguityException Thrown when registering a class with a already existing tag
     */
    public void registerMapping(Class<?> child, Class<?> base) throws ImplementationAmbiguityException {
        String tag = child.isAnnotationPresent(Tag.class)
                ? child.getAnnotation(Tag.class).value() : null;
        dependencies.put(base, child, tag);
    }

    /**
     * Returns a new instance of an injectable type from the container.
     * Will inject the needed dependencies into the created instance.
     * @param type Base type of the dependency
     * @param <T> Type of the dependency
     * @return An instance of type <code>T</code>
     * @throws ImplementationNotFoundException Thrown when no implementation has been found
     * @throws NoDefaultConstructorException Thrown when an injectable type has no injectable constructor and no
     * default constructor
     * @throws ImplementationAmbiguityException Thrown when the container cannot distinguish between two implementation
     * classes
     * @throws CyclicDependenciesException Thrown when the container detects an infinite dependency cycle
     */
    public <T> T instantiate(Class<T> type)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {
        return instantiate(type, null);
    }

    /**
     * Returns a new instance of an injectable type from the container.
     * Will inject the needed dependencies into the created instance.
     * Uses the tag to distinguish between different implementations.
     * @param type Base type of the dependency
     * @param tag The tag of the needed implementation
     * @param <T> Type of the dependency
     * @return An new instance of type <code>T</code>
     * @throws ImplementationNotFoundException Thrown when no implementation has been found
     * @throws NoDefaultConstructorException Thrown when an injectable type has no injectable constructor and no
     * default constructor
     * @throws ImplementationAmbiguityException Thrown when the container cannot distinguish between two implementation
     * classes
     * @throws CyclicDependenciesException Thrown when the container detects an infinite dependency cycle
     */
    public <T> T instantiate(Class<T> type, String tag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {
        injectionCounter.clear();
        return instantiateByTag(type, tag);
    }

    /**
     * Recursive method used to create a new instance and inject the dependencies it needs.
     * @param type The type of the dependency
     * @param tag The tag used to distinguish between implementations
     * @param <T> The type of the dependency
     * @return A new instance with its dependencies
     */
    @SuppressWarnings("unchecked")
    private <T> T instantiateByTag(Class<T> type, String tag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {

        // Finding the correct implementation based on the base type and its tag
        var implementation = getImplementationFromBase(type, tag);

        // Increment the number of injections for this type to detect eventual cyclic dependencies
        if (injectionCounter.containsKey(implementation)) {
            injectionCounter.put(implementation, injectionCounter.get(implementation) + 1);
        } else {
            injectionCounter.put(implementation, 1);
        }

        // A cycle has been found
        if (injectionCounter.get(implementation) > MAX_RECURSIVE_INJECTIONS) {
            var classesInCycle = injectionCounter
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() >= MAX_RECURSIVE_INJECTIONS - 1)
                    .map(entry -> entry.getKey().getName())
                    .sorted()
                    .toList();
            throw new CyclicDependenciesException(
                    String.format("Found circular dependencies with classes: %s",
                            String.join(",", classesInCycle)));
        }

        // Caching for singleton types
        var isSingleton = implementation.isAnnotationPresent(Singleton.class);
        if (isSingleton && instanceCache.containsKey(implementation)) {
            return (T)instanceCache.get(implementation);
        }

        var instance = injectIntoClass(implementation);
        if (isSingleton) {
            instanceCache.put(implementation, instance);
        }
        return (T)instance;
    }

    /**
     * Method used to do the actual injection into an implementation type.
     * Used to the check the order of the injection. If an injectable constructor
     * exists, then we create the instance using it, while doing the actual injection.
     * We then inject the needed dependencies into the fields and setters.
     * @param cls An implementation type
     * @param <T> The type of the created instance
     * @return A new instance with its dependencies
     */
    private <T> T injectIntoClass(Class<T> cls)
            throws NoDefaultConstructorException, ImplementationNotFoundException, ImplementationAmbiguityException,
            CyclicDependenciesException {

        try {
            // Constructors that can be used to instantiate the object and fill in its dependencies
            var injectableConstructors = Arrays.stream(cls.getDeclaredConstructors())
                    .filter(c -> c.isAnnotationPresent(Inject.class))
                    .toList();

            T instance = null;
            if (!injectableConstructors.isEmpty()) {
                // Create a new instance from an injectable constructor
                instance = injectIntoConstructor(injectableConstructors.stream().findFirst().get());
            }

            if (instance == null) {
                // If an injectable constructor isn't available
                // Create an instance from the parameterless constructor
                // and inject only into the fields and setters
                instance = injectIntoFieldsAndSetter(cls);
            } else {
                // Continue the injection for the instance's fields and setters
                injectIntoFieldsAndSetter(cls, instance);
            }

            return instance;
        } catch (NoSuchMethodException e) {
            throw new NoDefaultConstructorException(
                    String.format("Could not find a default constructor or an " +
                            "injectable constructor for the injectable class %s", cls.getName()));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method used to inject dependencies into an already existing instance.
     * @param cls The type of the instance
     * @param instance The instance
     * @param <T> The type of the instance
     */
    private <T> void injectIntoFieldsAndSetter(Class<T> cls, T instance)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            ReflectiveOperationException, CyclicDependenciesException {

        injectIntoFields(cls, instance);
        injectIntoSetters(cls, instance);
    }

    /**
     * Dependency injection into an existing instance through its setters (or any method annotated with @Inject)
     * @param cls The type of the instance
     * @param instance The instance
     * @param <T> The type of the instance
     */
    private <T> void injectIntoSetters(Class<T> cls, T instance)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException, CyclicDependenciesException {

        for(Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Inject.class)) {
                method.setAccessible(true);
                var tag = getTag(method);
                var parameters = getParameters(method.getParameters(), tag);
                method.invoke(instance, parameters);
            }
        }
    }

    /**
     * Inject the dependencies into the fields of an instance
     * @param cls The type of the instance
     * @param instance The instance
     * @param <T> The type of the instance
     */
    private <T> void injectIntoFields(Class<T> cls, T instance)
            throws IllegalAccessException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException, CyclicDependenciesException {

        for(Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                var tag = getTag(field);
                field.set(instance, instantiateByTag(field.getType(), tag));
            }
        }
    }

    /**
     * Creates a new instance and do the dependency injection through its fields and setters.
     * Used when the class doesn't have any constructor that can be used to instantiate the class
     * and do the injection.
     * @param cls The type of the new instance
     * @param <T> The type of the new instance
     * @return A new instance created with the default constructor and its needed dependencies
     */
    private <T> T injectIntoFieldsAndSetter(Class<T> cls)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException, CyclicDependenciesException {

        // If an injectable class doesn't have a constructor annotated with @Inject
        // we suppose that it has a default constructor (without parameters)
        var instance = cls.getDeclaredConstructor().newInstance();
        injectIntoFieldsAndSetter(cls, instance);
        return instance;
    }

    /**
     * Get the parameters needed to call a method or a constructor.
     * We resolve the dependencies of the created instances.
     * @param parameters The array of the parameters of a method or constructor
     * @param constructorTag The tag of the constructor, if it applies
     * @return The instances needed to invoke a method or a constructor
     */
    private Object[] getParameters(Parameter[] parameters, String constructorTag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {

        var parametersOutput = new ArrayList<>();
        for(var parameter : parameters) {
            var type = parameter.getType();
            var parameterTag = parameter.isAnnotationPresent(Tag.class)
                    ? parameter.getAnnotation(Tag.class).value() : null;
            // The constructor tag wins over the parameter tag
            var appliedTag = constructorTag == null ? parameterTag : constructorTag;
            parametersOutput.add(instantiateByTag(type, appliedTag));
        }
        return parametersOutput.toArray();
    }

    /**
     * Gets the tag of an accessible object (such as a method or a field)
     * @param accessibleObject The accessible object
     * @return The value of the tag if the annotation is present, <code>null</code> otherwise
     */
    private static String getTag(AccessibleObject accessibleObject) {
        return accessibleObject.isAnnotationPresent(Tag.class)
                ? accessibleObject.getAnnotation(Tag.class).value() : null;
    }

    /**
     * Creates a new instance through an injectable constructor.
     * @param constructor The constructor used to instantiate the object and the dependency injection
     * @param <T> The type of the instance
     * @return A new instance with its needed dependencies
     */
    @SuppressWarnings("unchecked")
    private <T> T injectIntoConstructor(Constructor<?> constructor)
            throws ReflectiveOperationException, ImplementationNotFoundException, NoDefaultConstructorException,
            ImplementationAmbiguityException, CyclicDependenciesException {

        var tag = getTag(constructor);
        var parameters = getParameters(constructor.getParameters(), tag);
        return (T)constructor.newInstance(parameters);
    }

    /**
     * Finds the implementation type from an interface or an abstract class.
     * We can use the tag to distinguish between implementation types.
     * @param baseClass The base abstract type (interface or abstract class)
     * @param tag The tag used to distinguish between implementations.
     * @return The actual implementation
     */
    private Class<?> getImplementationFromBase(Class<?> baseClass, String tag)
            throws ImplementationNotFoundException, ImplementationAmbiguityException {

        return dependencies.findImplementationFromBaseClass(baseClass, tag);
    }
}
