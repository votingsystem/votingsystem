class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.${format})?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller:"/serverInfo")
        "500"(view:'/error')


        "/timeStamp/$serialNumber" {
            controller = "timeStamp"
            action = [GET:"getBySerialNumber"]
            constraints {
                serialNumber(matches:/\d*/)
            }
        }

	}
}
