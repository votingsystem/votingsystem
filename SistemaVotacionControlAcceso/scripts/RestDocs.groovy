includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsRun")


target(default: "Generates REST documentation from controllers") {
	depends(configureProxy, packageApp, classpath, loadApp, configureApp)
    println " ---- Generating REST documentation from controllers"
	def restDocumentationService = appCtx.getBean('restDocumentationService')
	//def environment = System.getProperty ("restDocEnv")//arg
	restDocumentationService.generateDocs()
}



