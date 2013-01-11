--- Por favor, no hagas planes contando con este proyecto porque la información puede cambiar sin previo aviso. ---

- Prototipo de cliente de votación para plataformas Android del Sistema de Votación

- Para construir ejecutar desde la línea de comandos:
	$gradle release

--- DESARROLLO EN ECLIPSE ---
- El proyecto necesita la librería ActionBarSherlock -> http://actionbarsherlock.com/index.html
http://www.grokkingandroid.com/adding-actionbarsherlock-to-your-project/

- Al añadir la integración con Ant se pueden presentar problemas en Eclipse con la librería ActionbarSherlock. Para resolverlos:
- Crear el archivo build.xml en la librería del proyecto ActionbarSherlock ejecutando:
	${ActionbarSherlock_Home}/library/android update project -p .
Refrescando y reconstruyendo los proyectos (si no funciona probar añadiendo y quitando la librería 
desde las propiedades del proyecto)

