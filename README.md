## Sistema de Votación

[Wiki](https://github.com/votingsystem/votingsystem/wiki)


### Pasos necesarios para ejecutar las aplicaciones del servidor

#### Preparar la base de datos

Si no se modifican los archivos <b>grails-app/conf/DataSource.groovy</b> las aplicaciones web necesitan que se este
ejecutando en el servidor una instancia de la base de datos PostgreSQL según se explica en [este documento]
(https://github.com/votingsystem/votingsystem/wiki/Configuraci%C3%B3n-de-la-base-de-datos)

#### Instalación de las librerías web
Para poder construir la aplicación es necesario tener instalado Bower(http://bower.io/) y ejecutar

    gradle installAllBowerComponents

En los directorios '/web-app/bower_components/polymer.html' de cada aplicación web se ha cambiado la referencia a
'polymer.js' por 'polymer.min.js'

