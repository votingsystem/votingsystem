## Sistema de Votación

[Wiki](https://github.com/jgzornoza/SistemaVotacion/wiki)

Para construir el proyecto se necesita tener instalado la versión 1.8 del
[el entorno de construción Gradle](http://www.gradle.org/)

### Pasos necesarios para ejecutar las aplicaciones del servidor

#### Preparar la base de datos

Si no se modifican los archivos <b>grails-app/conf/DataSource.groovy</b> las aplicaciones web necesitan que se este
ejecutando en el servidor una instancia de la base de datos PostgreSQL según se explica en [este documento]
(https://github.com/jgzornoza/SistemaVotacion/wiki/Configuraci%C3%B3n-de-la-base-de-datos)

#### Preparar el entorno de pruebas

Para preparar el entorno de pruebas hay que ejecutar:

    SistemaVotacion$gradle initDevEnvironment

De esta forma:
*   Se instalan los editores de texto empleados en las aplicaciones **Control de Acceso**, y **TestsWebApp**
*   Se instalan los certificados empleados por la aplicación para generar firmas y descifrar documentos.


#### Generación de las aplicaciones

Para generar las aplicaciones

    SistemaVotacion$gradle buildAll

Para generar los componentes de los distintos proyectos es necesario tener instalados certificados electrónicos
(para entornos de pruebas se pueden emplear los generados en el paso anterior):
