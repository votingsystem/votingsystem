class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.${format})?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller:"/app")
        "500"(view:'/error')

        "/app/userVS/last/$numHours" {
            controller = "app"
            action = "userVS"
            constraints {
                numHours(matches:/\d*/)
            }
        }

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

        "/balance/week/$year/$month/$day" {
            controller = "balance"
            action = "week"
            constraints {
                year(matches:/\d*/)
                month(matches:/\d*/)
                day(matches:/\d*/)
            }
        }

        "/balance/weekdb/$year/$month/$day" {
            controller = "balance"
            action = "weekdb"
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

        "/deviceVS/$nif/list" {
            controller = "deviceVS"
            action = "list"
        }

        "/deviceVS/$nif/connected" {
            controller = "deviceVS"
            action = "connected"
        }

        "/groupVS/$id"{
            controller = "groupVS"
            action = "index"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/groupVS/$id/balance"{
            controller = "groupVS"
            action = "balance"
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

        "/shop/$uuid"{
            controller = "shopExample"
            action = "paymentInfo"
        }

        "/shop/$uuid/payment"{
            controller = "shopExample"
            action = "payment"
        }

        "/shop/listenTransactionChanges/$shopSessionID"{
            controller = "shopExample"
            action = "listenTransactionChanges"
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

        "/transactionVS/from/$dateFrom/to/$dateTo"{
            controller = "transactionVS"
            action = "index"
        }

        "/userVS" {
            controller = "userVS"
            action = [POST:"save"]
        }

        "/userVS/userInfo/$NIF" {
            controller = "userVS"
            action = "userInfo"
        }

        "/userVS/$NIF/$year/$month/$day" {
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

        "/userVS/$userType/IBAN/$IBAN"{
            controller = "userVS"
            action = "index"
        }



        "/userVS/$id/balance"{
            controller = "cooinAccount"
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

        "/cooin/request" {
            controller = "cooin"
            action = [POST:"processRequestFileMap", GET:"request"]
        }

        "/cooin/state/$hashCertVSHex" {
            controller = "cooin"
            action = "status"
        }

    }
}