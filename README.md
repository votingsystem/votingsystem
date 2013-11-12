Sistema de Votación
======================================

[Wiki](https://github.com/jgzornoza/SistemaVotacion/wiki)

Para construir el proyecto se necesita tener instalado la versión 1.8 del [el entorno de construción Gradle](http://www.gradle.org/)

Para importar los proyectos en Eclipse sin que de problemas el plugin-gradle para Android hay que configurar el espacio de trabajo para que use la versión 1.8 de Gradle ('Preferences > Gradle' ...) 

Para que las aplicaciones puedan generar las firmas necesitan tener instalados certificados electrónicos. 
Para trabajar en entornos de pruebas se pueden emplear los que genera la aplicación al ejecutar las tareas Gradle:
	
	SistemaVotacion$gradle genAppsCerts
	SistemaVotacion$gradel genWebAppsCerts


Para generar los componentes de los distintos proyectos es necesario tener instalados certificados electrónicos (para entornos de pruebas se pueden emplear los generados en el paso anterior):
	
	$gradle buildAll


