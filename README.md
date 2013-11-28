Sistema de Votación
======================================

[Wiki](https://github.com/jgzornoza/SistemaVotacion/wiki)

Para construir el proyecto se necesita tener instalado la versión 1.8 del
[el entorno de construción Gradle](http://www.gradle.org/)

### Pasos necesarios para ejecutar las aplicaciones del servidor

#### Preparar la base de datos

Si no se modifican los archivos <b>grails-app/conf/DataSource.groovy</b> las aplicaciones web necesitan que se este
ejecutando en el servidor una instancia de la base de datos PostgreSQL según se explica en [este documento]
(https://github.com/jgzornoza/SistemaVotacion/wiki/Configuraci%C3%B3n-de-la-base-de-datos)

#### Instalar los editores de las aplicaciones

Para instalar los editores de texto empleados en las aplicaciones **Control de Acceso**, y **TestsWebApp** hay que ejecutar :

    <code>SistemaVotacion$gradle installEditor</code>


#### Instalar los certificados electrónicos

Para que las aplicaciones puedan generar firmas y descifrar documentos necesitan tener instalados certificados electrónicos.
Para trabajar en entornos de pruebas se pueden emplear los que genera la aplicación al ejecutar:


	<code>SistemaVotacion$gradle installCerts</code>


#### Generación de las aplicaciones

Para generar las aplicaciones

    ´´´SistemaVotacion$gradle buildAll´´´

Para generar los componentes de los distintos proyectos es necesario tener instalados certificados electrónicos
(para entornos de pruebas se pueden emplear los generados en el paso anterior):
