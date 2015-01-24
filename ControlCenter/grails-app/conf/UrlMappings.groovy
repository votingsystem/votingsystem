class UrlMappings {


    static mappings = {
        "/$controller/$action?/$id?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller:"/eventVSElection")
        "500"(view:'/error500')


        "/voteVSCanceller/voteVS/${id}"{
            controller = "voteVSCanceller"
            action = "get"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/certificateVS/voteVS/hashHex/$hashHex"{
            controller = "certificateVS"
            action = "voteVS"
        }

        "/certificateVS/userVS/$userId" {
            controller = "certificateVS"
            action = "userVS"
        }

        "/eventVSElection"{
            controller = "eventVSElection"
            action = [POST:"save"]
        }

        "/eventVSElection/$id?"{
            controller = "eventVSElection"
            action = "index"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/eventVSElection/$id/checkDates"{
            controller = "eventVSElection"
            action = "checkDates"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/eventVSElection/$id/stats" {
            controller = "eventVSElection"
            action = "stats"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/messageSMIME/$id"{
            controller = "messageSMIME"
            action = "index"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/subscriptionVS/elections/$feedType"{
            controller = "subscriptionVS"
            action = "elections"
        }

        "/voteVS/hashHex/$hashHex" {
            controller = "voteVS"
            action = [GET:"hashCertVoteHex"]
        }

        "/voteVS/${id}" {
            controller = "voteVS"
            action = "get"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/errors/event/${id}" {
            controller = "voteVS"
            action = "errors"
            constraints {
                id(matches:/\d*/)
            }
        }

        "/eventVSElection/$id/votingErrors" {
            controller = "eventVSElection"
            action = "votingErrors"
            constraints {
                id(matches:/\d*/)
            }
        }
    }
}
