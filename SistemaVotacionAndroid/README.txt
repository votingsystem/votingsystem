--- Por favor, no hagas planes contando con este proyecto porque la información puede cambiar sin previo aviso. ---

- Prototipo de cliente de votación del sistema https://github.com/jgzornoza/SistemaVotacionControlAcceso/wiki/Control-de-Acceso 
para plataformas Android.

- Para ejecutar el proyecto hay que integrar la librería ActionBarSherlock -> http://actionbarsherlock.com/index.html
http://www.grokkingandroid.com/adding-actionbarsherlock-to-your-project/

- Para construir e instalar desde la consola de comandos hay modificar la ruta del sdk en el archivo 'local.properties' y ejecutar:
$ant debug install

- Al añadir la integración con Ant se pueden presentar problemas en Eclipse con la librería ActionbarSherlock. Para resolverlos:
- Crear el archivo build.xml en el proyecto ActionbarSherlock ejecutando:
	${ActionbarSherlock_Home}/library/android update project -p .
Refrescando y reconstruyendo los proyectos (si no funciona probar añadiendo y quitando la librería 
desde las propiedades del proyecto)

-------------------------------------------------------------------------------
Partido Político del Programa -> https://github.com/jgzornoza/SistemaVotacionControlAcceso/wiki/Partido-Pol%C3%ADtico-del-Programa 