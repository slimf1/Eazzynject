package fr.gravani.eazzynject;

import fr.gravani.eazzynject.annotations.Injectable;
import fr.gravani.eazzynject.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Eazzynject {

    /**
     * The dependency injection container
     */
    private static final Container CONTAINER = new Container();

    /**
     * Initializes the container from the types of a given package
     * @param packageName The name of the package
     * @throws IOException If an I/O error occurs while reading the package
     * @throws ClassNotFoundException If we cannot find a given class
     * @throws ImplementationAmbiguityException If two or injectable types use the same tag
     */
    public static void initContainer(String packageName)
            throws IOException, ClassNotFoundException, ImplementationAmbiguityException {
        List<Class<?>> allClassesInPackage = getClasses(packageName);
        var injectableClasses =  allClassesInPackage
                .stream()
                .filter(c -> c.isAnnotationPresent(Injectable.class)
                        && !c.isInterface() && !Modifier.isAbstract(c.getModifiers()))
                .toList();

        for(var injectableClass : injectableClasses) {
            CONTAINER.registerMapping(injectableClass, injectableClass);
            registerSuperclassesInterfaces(injectableClass, injectableClass);
        }
    }

    /**
     * Initializes the container from the types of a given package
     * @param rootClass A class from a package
     * @throws IOException If an I/O error occurs while reading the package
     * @throws ClassNotFoundException If we cannot find a given class
     * @throws ImplementationAmbiguityException If two or injectable types use the same tag
     */
    public static void initContainer(Class<?> rootClass)
            throws IOException, ImplementationAmbiguityException, ClassNotFoundException {
        initContainer(rootClass.getPackageName());
    }

    /**
     * Retrieves an instance from the container
     * @param type The type of the instance
     * @param <T> The type of the instance
     * @return An instance with its needed dependencies resolved
     * @throws ImplementationNotFoundException If we cannot find a needed implementation of an abstraction
     * @throws NoDefaultConstructorException If an injectable class has no injectable constructor
     * and no parameterless constructor
     * @throws ImplementationAmbiguityException If the container cannot distinguish between two implementations
     * @throws CyclicDependenciesException If we detect a dependency cycle
     */
    public static <T> T getInstance(Class<T> type)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {
        return CONTAINER.instantiate(type);
    }

    /**
     * Retrieves an instance from the container. A tag is used to distinguish between two implementations.
     * @param type The type of the instance
     * @param tag The tag of the implementation
     * @param <T> The type of the implementation
     * @return An instance with its needed dependencies resolved
     * @throws ImplementationNotFoundException If we cannot find a needed implementation of an abstraction
     * @throws NoDefaultConstructorException If an injectable class has no injectable constructor
     * and no parameterless constructor
     * @throws ImplementationAmbiguityException If the container cannot distinguish between two implementations
     * @throws CyclicDependenciesException If we detect a dependency cycle
     */
    public static <T> T getInstance(Class<T> type, String tag)
            throws ImplementationNotFoundException, NoDefaultConstructorException, ImplementationAmbiguityException,
            CyclicDependenciesException {
        return CONTAINER.instantiate(type, tag);
    }

    /**
     * Registers the implementations with the abstractions into the dependency injection container.
     * @param implementationClass The implementation
     * @param superClass The abstraction of the implementation
     * @throws ImplementationAmbiguityException If a tag of an implementation already exists
     */
    private static void registerSuperclassesInterfaces(Class<?> implementationClass, Class<?> superClass)
            throws ImplementationAmbiguityException {
        Class<?> cSuperClass = superClass.getSuperclass();
        Class<?>[] interfaces = superClass.getInterfaces();

        // Registering super classes
        if(cSuperClass != Object.class && cSuperClass != null) {
            CONTAINER.registerMapping(implementationClass, cSuperClass);
            registerSuperclassesInterfaces(implementationClass, cSuperClass);
        }
        // Registering interfaces
        for(Class<?> curInterface : interfaces) {
            CONTAINER.registerMapping(implementationClass, curInterface);
            registerSuperclassesInterfaces(implementationClass, curInterface);
        }
    }

    /**
     * Retrieves all the classes of a given package
     * @param packageName The name of the package
     * @return A list of all the types of the package being analyzed
     * @throws IOException If an IO error occurs
     * @throws ClassNotFoundException If a class cannot be found
     */
    private static List<Class<?>> getClasses(String packageName)
            throws IOException, ClassNotFoundException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if(classLoader == null) {
            throw new RuntimeException("ClassLoader not found for automatic package scanning.");
        }
        String filePath = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(filePath);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    /**
     * Retrieves the classes of a given directory and package
     * @param directory The directory
     * @param packageName The name of the package
     * @return All the classes found
     * @throws ClassNotFoundException If a given class cannot be found
     */
    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    assert !file.getName().contains(".");
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(
                            packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                }
            }
        }
        return classes;
    }
}
