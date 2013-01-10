class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(view:"/index")
		"400"(controller:"error400", action:"procesar")
		"500"(controller:"error500", action:"procesar")
	}
}
