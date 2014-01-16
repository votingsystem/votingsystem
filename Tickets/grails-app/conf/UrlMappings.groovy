class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.${format})?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller:"/serverInfo")
        "500"(view:'/error')


        "/ticket/request" {
            controller = "ticket"
            action = [POST:"processRequestFileMap"]
        }
	}
}
