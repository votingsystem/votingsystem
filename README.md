Sistema de Votación
======================================

[Wiki](https://github.com/jgzornoza/SistemaVotacion/wiki)

Para construir el proyecto se necesita tener instalado la versión 1.8 del
[el entorno de construción Gradle](http://www.gradle.org/)

----

La aplicación de '''Control de Acceso''' utiliza en su editor la librería javascript [ckeditor](http://ckeditor.com/).
Para instalarla:

    SistemaVotacion/AccessControl$gradle installCkeditor

----

Para que las aplicaciones puedan generar las firmas necesitan tener instalados certificados electrónicos. 
Para trabajar en entornos de pruebas se pueden emplear los que genera la aplicación al ejecutar las tareas Gradle:
	
	SistemaVotacion$gradle genAppsCerts
	SistemaVotacion$gradel genWebAppsCerts


Para generar los componentes de los distintos proyectos es necesario tener instalados certificados electrónicos
(para entornos de pruebas se pueden emplear los generados en el paso anterior):
	
	SistemaVotacion$gradle buildAll


