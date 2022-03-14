# TP 2 Java Pro - Eazzynject

[![CI/CD](https://github.com/slimf1/Eazzynject/actions/workflows/maven.yml/badge.svg)](https://github.com/slimf1/Eazzynject/actions/workflows/maven.yml)

> Travail de Dorian GRAVEGEAL et Slimane FAKANI

Eazzynject est un conteneur d'injection de dépendances.

## Features :
* Un conteneur d'injection de dépendances (```Container.java```) : permet de gérer l'instanciation d'objets. 
  * La méthode ```registerMapping(implementation, abstraction)``` permet de lier une classe abstraite ou une interface à l'implémentation concrète à instancier.
  * la méthode ```getInstance(class, [tag])``` permet d'instancier un objet d'une classe donnée, en injectant automatiquement toutes les dépendances dont il a besoin pour fonctionner.  
* Un scanner de package : permet l'association automatique de chaque implémentation aux interfaces et classes abstraites parentes (```Eazzynject.java```).

Avec l'utilisation du scanner de package, il ne sera jamais nécessaire d'utiliser la méthode ```registerMapping(implementation, abstraction)``` manuellement.

## API :
### ``@Injectable``
Annotation à utiliser sur une classe qui est une implémentation. Cette annotation sera repérée par le scanner de package pour associer l'implémentation à toutes les classes abstraites et interfaces parentes de cette implémentation.
### ``@Inject``
Annotation à utiliser sur un constructeur, setter ou attribut d'une classe. Cette annotation spécifie au conteneur d'injection de dépendances que cet élément doit être instancié et injecté dans l'instance à construire. 
### ``@Singleton``
Annotation à utiliser avec l'annotation ``@Inject`` pour préciser que l'instance à injecter est unique (singleton). Lors d'une injection avec l'annotation singleton, si une instance de la classe à injecter est déjà présente dans le cache, elle sera injectée. Dans le cas contraire, une nouvelle instance sera créée à chaque injection.
### ``@Tag``
Annotation permettant de repérer une instance par un nom. Il ya deux cas d'usages à cette annotation : 
* Avec l'annotation ``@Injectable``, cette annotation permet de donner un nom à l'implémentation. Ainsi, il est possible de lier plusieurs implémentations à la même interface ou classe abstraite.
* Avec l'annotation ``@Inject``, cette annotation permet de préciser quelle implémentation utiliser pour l'injection de la dépendance dans le cas où plusieurs implémentations sont disponibles pour une même classe abstraite ou interface.
### ``Eazzynject.runApplication(class);``
Méthode à utiliser au point d'entrée du programme avec comme paramètre la classe qui sert de point d'entrée du programme. Cette méthode va lancer le scan du package et des sous-packages de cette classe afin de d'enregistrer de manière automatique les liens entres les interfaces et les implémentations. Pour cela, le scanner se base sur les annotation ``@Injectable`` et ``@Tag`` utilisées lors de la déclaration des classes.
### ``Eazzynject.getInstance(class, [tag]);``
Méthode permettant de récupérer l'instance d'une classe via le conteneur d'injection de dépendance. Le paramètre facultatif ``tag`` peut être utilisé pour préciser l'implémentation à utiliser.


## Choix techniques

### Stockage des dépendances (lien entre interfaces et implémentation)
Pour cette partie, nous avons fait plusieurs essais de structure de données avant de parvenir à une solution qui nous convienne.
* Première solution : Utiliser une ``HashMap`` avec pour clé le type de l'interface ou classe abstraite parente, et pour valeur de type de l'implémentation concrète. 
  Cette solution a pour principal défaut qu'il n'est pas possible d'associer plusieurs implémentations à la même abstraction, ce qui ne nous permettait pas de mettre en place le système de tag pour gérer de multiples implémentations.
* Nous avons ensuite pensé à inverser la clé et la valeur de cette ``HashMap``, c'est-à-dire d'utiliser l'implémentation comme clé, de manière à pour voir stocker plusieurs abstractions pour une même implémentation. Cependant, cette solution ne convenais toujours pas car il n'était plus possible d'associer plusieurs abstractions d'une hiérarchie à la même implémentation.
* Nous sommes donc finalement passés par une structure intermédiaire pour la gestion des dépendances. Ceci est géré par la classe ``Dependencies`` qui contient une liste de ``ImplementationLink``. La classe ``ImplementationLink`` permet de faire le lien entre une classe abstraite ou interface et ses implémentations. Les implémentations d'une classe abstraite sont alors stockées par tag dans une ``HashMap`` (cf. annotation ``@Tag``). Cette solution nous a permis de gérer tous les cas dont nous avions besoin, à savoir : 
    * Associer plusieurs classes abstraites ou interface à une même implémentation (hiérarchie de classes)
    * Associer plusieurs implémentations à une même classe abstraite ou interface (gestion par tag)


### Détection des cycles lors de l'injection
L'injection de dépendance étant récursive lors de la construction des instances, nous nous somme heurté au problème de gestion des dépendances circulaires, qui conduisent à une boucle infinie lors de l'instanciation d'un objet. Nous avons traité ce problème par l'utilisation d'un compteur qui est incrémenté à chaque appel récursif d'injection. Nous avons considéré qu'a partir de 32 injections récursives d'un même type, nous sommes dans le cas d'une dépendance circulaire. Nous lançons alors une exception pour couper la boucle. 

## Tests

Nous avons défini plusieurs cas de test que nous appliquons sur tous les types d'injection (setter, constructeur, field, mélangé) : 
* Divers injections recursives avec éventuellement plusieurs services à injecter 
* Dépendances circulaires
* Injection par tag

Nous testons également le singleton et le scan de package.

Nous arrivons à un taux de code coverage de 94%.

![Code coverage at 94 percent](https://github.com/slimf1/Eazzynject/blob/master/codecov.png?raw=true)


## Documentation

Une bibliothèque tierce se doit d'avoir une documentation bien fournie. Nous avons donc généré automatiquement la documentation avec JavaDoc. Elle est disponible dans le dossier ``/doc`` à la racine du projet.

## Sources

Nous nous sommes inspirés de ce tutoriel :
https://dev.to/jjbrt/how-to-create-your-own-dependency-injection-framework-in-java-4eaj
