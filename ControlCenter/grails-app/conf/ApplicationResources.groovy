modules = {

	'jquery' {
		resource url: 'js/jquery-1.10.2.min.js'
		resource url: 'js/jquery-ui-1.10.3.custom.min.js'
		resource url: 'css/jquery-ui-1.10.3.custom.min.css'
		resource url: 'js/i18n/jquery.ui.datepicker-es.js'
	}
	
	'application' {
		//if (isDevMode()) {}
		dependsOn 'jquery'
        resource url: 'font-awesome/css/font-awesome.min.css'
        resource url: 'css/pcVotingSystem.css'
		resource url: 'js/utils.js.gsp'
		resource url: 'js/pcUtils.js.gsp'
		resource url: 'js/deployJava.js'
	}
	
	'applicationMobile' {
		dependsOn 'jquery'
        resource url: 'font-awesome/css/font-awesome.min.css'
        resource url: 'css/mobileVotingSystem.css'
		resource url: 'js/utils.js.gsp'
		resource url: 'js/mobileUtils.js.gsp'
	}

	'paginate' {
		dependsOn 'jquery'
		resource url: 'js/jqueryPaginate.js.gsp'
		resource 'css/jqueryPaginate.css'
	}
	
	'charts' {
		resource url: 'js/jsapi.js'
		resource 'css/charts.css'
	}
}

boolean isDevMode() { !Metadata.current.isWarDeployed() }