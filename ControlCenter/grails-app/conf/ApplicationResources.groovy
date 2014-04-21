modules = {

	'jquery' {
		resource url: 'js/jquery-1.11.0.min.js'
		resource url: 'js/jquery-ui-1.10.4.custom.min.js'
		resource url: 'css/jquery-ui-1.10.4.custom.min.css'
		resource url: 'js/i18n/jquery.ui.datepicker-es.js'
	}

    'baseApp' {
        dependsOn 'jquery'
        resource url: 'css/jquery.multilevelpushmenu.css'
        resource url: 'font-awesome/css/font-awesome.min.css'
        resource url: 'js/utils.js.gsp'
        resource url: 'css/bootstrap.min.css'
        resource url: 'js/bootstrap.min.js'
        resource url: 'js/jquery.multilevelpushmenu.min.js'
    }

	'application' {
		//if (isDevMode()) {}
        dependsOn 'baseApp'
        resource url: 'css/pcVotingSystem.css'
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

    dynatableModule {
        resource url: 'css/jquery.dynatable.css'
        resource url: 'js/jquery.dynatable.js'
    }
	
	'charts' {
		resource url: 'js/jsapi.js'
		resource 'css/charts.css'
	}
}

boolean isDevMode() { !Metadata.current.isWarDeployed() }