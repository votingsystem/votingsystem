/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class UrlMappings {

	static mappings = {

		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(view:"/index")
		"500"(view:'/error500')

		"/android/VotingToolAndroid.apk" {
			controller = "android"
			action = "app"
		}

		"/voteVSCanceller" {
			controller = "voteVSCanceller"
			action = [POST:"post"]
		}

		"/voteVSCanceller/$hashHex" {
			controller = "voteVSCanceller"
			action = "index"
		}


		"/voteVSCanceller/voteVS/$id"{
			controller = "voteVSCanceller"
			action = "get"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/certificateVS/userVS/$userId" {
			controller = "certificateVS"
			action = "userVS"
			constraints {
				userId(matches:/\d*/)
			}
		}

		"/certificateVS/eventCA/$eventVS_Id" {
			controller = "certificateVS"
			action = "eventCA"
			constraints {
				eventVS_Id(matches:/\d*/)
			}
		}

		"/certificateVS/voteVS/hashHex/$hashHex" {
			controller = "certificateVS"
			action = "voteVS"
		}

		"/eventVS/$id?" {
			controller = "eventVS"
			action = "index"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVS/$id/checkDates" {
			controller = "eventVS"
			action = "checkDates"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVS/$id/statistics" {
			controller = "eventVS"
			action = "statistics"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSManifest" {
			controller = "eventVSManifest"
			action = [POST:"save"]
		}

		"/eventVSManifest/$id" {
			controller = "eventVSManifest"
			action = [GET:"index", POST:"save"]
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSManifest/$id/statistics" {
			controller = "eventVSManifest"
			action = "statistics"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSManifest/$id/signaturesInfo" {
			controller = "eventVSManifest"
			action = "signaturesInfo"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSManifest/signed/$id" {
			controller = "eventVSManifest"
			action = "signed"
			constraints {
				id(matches:/\d*/)
			}
		}


		"/eventVSClaim" {
			controller = "eventVSClaim"
			action = [POST:"save"]
		}

		"/eventVSClaim/$id?" {
			controller = "eventVSClaim"
			action = [GET:"index"]
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSClaim/$id/statistics" {
			controller = "eventVSClaim"
			action = "statistics"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSClaim/$id/signed" {
			controller = "eventVSClaim"
			action = "signed"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSClaim/$id/validated" {
			controller = "eventVSClaim"
			action = "validated"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSClaim/$id/signaturesInfo" {
			controller = "eventVSClaim"
			action = "signaturesInfo"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSElection" {
			controller = "eventVSElection"
			action = [POST:"save"]
		}

		"/eventVSElection/$id?" {
			controller = "eventVSElection"
			action = "index"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSElection/$id/statistics" {
			controller = "eventVSElection"
			action = "statistics"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSElection/$id/voteVSInfo" {
			controller = "eventVSElection"
			action = "statistics"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSElection/$id/validated" {
			controller = "eventVSElection"
			action = "validated"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/eventVSElection/$id/signed" {
			controller = "eventVSElection"
			action = "signed"
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

		"/messageSMIME/$id" {
			controller = "messageSMIME"
			action = "index"
		}

		"/messageSMIME/receipt/$requestMessageId" {
			controller = "messageSMIME"
			action = "receipt"
		}

		"/eventVSManifestCollector/$id?" {
			controller = "eventVSManifestCollector"
			action = "index"
			constraints {
				id(matches:/\d*/)
		    }
		}

		"/representative" {
			controller = "representative"
			action = [POST:"processFileMap"]
		}

        "/representative/anonymousDelegationRequest" {
            controller = "representative"
            action = [POST:"processAnonymousDelegationRequestFileMap"]
        }

		"/representative/$id?" {
			controller = "representative"
			action = "index"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/representative/nif/$nif" {
			controller = "representative"
			action = "getByNif"
		}

		"/representative/edit/$nif" {
			controller = "representative"
			action = "editRepresentative"
		}

		"/representative/image/$id" {
			controller = "representative"
			action = "image"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/representative/$representativeId/image" {
			controller = "representative"
			action = [GET:"image"]
			constraints {
				representativeId(matches:/\d*/)
			}
		}

		"/representative/accreditationsBackupForEvent/$id" {
			controller = "representative"
			action = "accreditationsBackupForEvent"
			constraints {
				id(matches:/\d*/)
			}
		}

		"/accessRequestVS" {
			controller = "accessRequestVS"
			action = [POST:"processFileMap"]
		}

		"/accessRequestVS/$id" {
			controller = "accessRequestVS"
			action = [GET:"index"]
		}

		"/accessRequestVS/eventVS/$eventId/nif/$nif" {
			controller = "accessRequestVS"
			action = [GET:"findByNif"]
		}

		"/accessRequestVS/hashHex/$hashHex" {
			controller = "accessRequestVS"
			action = [GET:"hashHex"]
		}

		"/backupVS/download/$id" {
			controller = "backupVS"
			action = [GET:"download"]
			constraints {
				id(matches:/\d*/)
			}
		}

		"/backupVS/$id" {
			controller = "backupVS"
			action = [GET:"get"]
			constraints {
				id(matches:/\d*/)
			}
		}

		"/subscriptionVS/claims/$feedType?"{
			controller = "subscriptionVS"
			action = "claims"
		}

		"/subscriptionVS/elections/$feedType?"{
			controller = "subscriptionVS"
			action = "elections"
		}

		"/subscriptionVS/manifests/$feedType?"{
			controller = "subscriptionVS"
			action = "manifests"
		}

		"/userVS" {
			controller = "userVS"
			action = [POST:"save"]
		}

		"/voteVS" {
			controller = "voteVS"
			action = [POST:"save"]
		}


		"/voteVS/$id" {
			controller = "voteVS"
			action = "get"
			constraints {
				id(matches:/\d*/)
			}
		}

	}
}
