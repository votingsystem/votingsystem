## Sistema de Votación

[Wiki](https://github.com/jgzornoza/SistemaVotacion/wiki)


### Pasos necesarios para ejecutar las aplicaciones del servidor

#### Preparar la base de datos

Si no se modifican los archivos <b>grails-app/conf/DataSource.groovy</b> las aplicaciones web necesitan que se este
ejecutando en el servidor una instancia de la base de datos PostgreSQL según se explica en [este documento]
(https://github.com/jgzornoza/SistemaVotacion/wiki/Configuraci%C3%B3n-de-la-base-de-datos)

#### Instalación de Polymer
Algunos componentes de la aplicación necesitan Polymer (http://www.polymer-project.org/). Para poder construir la
aplicación es necesario tener instalado Bower(http://bower.io/) y ejecutar

    gradle installPolymer

#### Generación de los certificados de pruebas

Para generar los certificados de pruebas:
    
    SistemaVotacion$gradle installCerts

#### Construcción de las aplicaciones

Para construir las aplicaciones

    SistemaVotacion$gradle buildAll

Para generar los componentes de los distintos proyectos es necesario tener instalados certificados electrónicos
(para entornos de pruebas se pueden emplear los generados en el paso anterior)


#### Preparar el entorno de pruebas

Para preparar el entorno de pruebas hay que ejecutar:

    SistemaVotacion$gradle initDevEnvironment

De esta forma:
*   Se instalan los editores de texto empleados en las aplicaciones **Control de Acceso**, y **TestsWebApp**
*   Se instalan los certificados empleados por la aplicación para generar firmas y descifrar documentos.

Para que se ejecuten los editores en entornos Android hay que modificar el valor de la variable 'mobile' en el archivo 'ckeditor.js'

#### Servicio de sellado de tiempo

Los documentos firmados utilizados por la aplicación llevan incorporado un **sello de tiempo**. 

El proyecto incorpora un servidor de sellos de tiempo, la aplicación **TimeStampServer**. Para que el sistema funcione es necesario que **AccessControl** y **ControlCenter** tengan acceso a una instancia de **TimeStampServer**.

La dirección del servidor de sello de tiempo de las aplicaciones **AccessControl** y **ControlCenter** se configura en la propiedad 'VotingSystem.urlTimeStampServer' del archivo:

    grails-app/conf/Config.groovy


=======================================================================================================

* Para construir las aplicaciones se debe emplear un entorno con **Java 8**, **Grails 2.4.0** y **Gradle 1.11** 

