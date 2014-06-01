class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.${format})?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller:"/app")
        "500"(view:'/error')

        "/certificateVS/vicket/hashHex/$hashHex" {
            controller = "certificateVS"
            action = "voteVS"
        }

        "/certificateVS/cert/$serialNumber"{
            controller = "certificateVS"
            action = "cert"
            constraints {
                serialNumber(matches:/\d*/)
            }
        }

        "/groupVS/$id"{
            controller = "groupVS"
            action = "index"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/groupVS/$id/subscribe"{
            controller = "groupVS"
            action = "subscribe"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/groupVS/$id/users"{
            controller = "groupVS"
            action = "users"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/groupVS/edit/$id"{
            controller = "groupVS"
            action = "edit"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/groupVS/cancel/$id"{
            controller = "groupVS"
            action = "cancel"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/groupVS/$id/users"{
            controller = "groupVS"
            action = "listUsers"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/groupVS/$id/user/$userId" {
            controller = "groupVS"
            action = "user"
            constraints {
                id(matches:/\d*/)
                userId(matches:/\d*/)
            }
        }

        "/messageSMIME/$id" {
            controller = "messageSMIME"
            action = [GET:"index"]
            constraints {
                id(matches:/\d*/)
            }
        }

        "/messageSMIME/receipt/$requestMessageId" {
            controller = "messageSMIME"
            action = "receipt"
        }

        "/subscriptionVS/$id/activate"{
            controller = "subscriptionVS"
            action = "activate"
            constraints {
                id(matches:/\d*/)
            }
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


        "/transaction/$id"{
            controller = "transaction"
            action = "get"
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

        "/userVS/$id"{
            controller = "userVS"
            action = "index"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/vicket/request" {
            controller = "vicket"
            action = [POST:"processRequestFileMap"]
        }

    }
}