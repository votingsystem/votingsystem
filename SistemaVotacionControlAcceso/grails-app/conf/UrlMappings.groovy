/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class UrlMappings {

	static mappings = {
		
		/*"/applet/appletFirma.jnlp" {
			controller = "applet"
			action = "jnlpCliente"
		}*/
		
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
	}
}
