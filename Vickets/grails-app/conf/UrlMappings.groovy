class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.${format})?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller:"/app")
        "500"(view:'/error')

        "/balance/userVS/$userId" {
            controller = "balance"
            action = "userVS"
            constraints {
                userId(matches:/\d*/)
            }
        }

        "/balance/userVS/$userId/db" {
            controller = "balance"
            action = "db"
            constraints {
                userId(matches:/\d*/)
            }
        }

        "/balance/userVS/$userId/$year/$month/$day" {
            controller = "balance"
            action = "userVS"
            constraints {
                userId(matches:/\d*/)
                year(matches:/\d*/)
                month(matches:/\d*/)
                day(matches:/\d*/)
            }
        }

        "/balance/weekReport/$year/$month/$day" {
            controller = "balance"
            action = "weekReport"
            constraints {
                year(matches:/\d*/)
                month(matches:/\d*/)
                day(matches:/\d*/)
            }
        }


        "/certificateVS/cert/$serialNumber"{
            controller = "certificateVS"
            action = "cert"
            constraints {
                serialNumber(matches:/\d*/)
            }
        }

        "/certificateVS/model/hashHex/$hashHex" {
            controller = "certificateVS"
            action = "voteVS"
        }

        "/certificateVS/userVS/$userId" {
            controller = "certificateVS"
            action = "userVS"
            constraints {
                userId(matches:/\d*/)
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

        "/IBAN/from/$IBANCode" {
            controller = "IBAN"
            action = "index"
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

        "/messageSMIME/transactionVS/$id" {
            controller = "messageSMIME"
            action = "transactionVS"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/reports/week/$year/$month/$day" {
            controller = "reports"
            action = "week"
            constraints {
                year(matches:/\d*/)
                month(matches:/\d*/)
                day(matches:/\d*/)
            }
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


        "/transactionVS/$id"{
            controller = "transactionVS"
            action = "get"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/transactionVS"{
            controller = "transactionVS"
            action = "post"
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

        "/userVS/IBAN/$IBAN"{
            controller = "userVS"
            action = "index"
        }

        "/userVS/$id/balance"{
            controller = "userVSAccount"
            action = "balance"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/userVS/$id/transacionVS/$timePeriod"{
            controller = "transactionVS"
            action = "userVS"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/vicket/request" {
            controller = "vicket"
            action = [POST:"processRequestFileMap", GET:"request"]
        }

    }
}