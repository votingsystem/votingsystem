/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class UrlMappings {

	static mappings = {
		
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}
		
		"/android/SistemaVotacion.apk" {
			controller = "android"
			action = "app"
		}
	
		"/"(view:"/index")
		"500"(view:'/error500')
	}
}
