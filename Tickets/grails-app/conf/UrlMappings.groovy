class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.${format})?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller:"/serverInfo")
        "500"(view:'/error')


        "/certificateVS/ticket/hashHex/$hashHex" {
            controller = "certificateVS"
            action = "voteVS"
        }

        "/testing/$year/$month/$day" {
            controller = "testing"
            action = "index"
            constraints {
                year(matches:/\d*/)
                month(matches:/\d*/)
                day(matches:/\d*/)
            }
        }

        "/ticket/request" {
            controller = "ticket"
            action = [POST:"processRequestFileMap"]
        }

        "/messageSMIME/$id" {
            controller = "messageSMIME"
            action = [GET:"index"]
            constraints {
                id(matches:/\d*/)
            }
        }

        "/userVS" {
            controller = "userVS"
            action = [POST:"save"]
        }

        "/userVS/$year/$month/$day" {
            controller = "userVS"
            action = "userInfo"
            constraints {
                year(matches:/\d*/)
                month(matches:/\d*/)
                day(matches:/\d*/)
            }
        }

	}
}
