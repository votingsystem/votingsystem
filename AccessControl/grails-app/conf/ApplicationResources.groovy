modules = {

    jquery {
        resource url: 'js/jquery-1.11.0.min.js'
    }

    jquery_ui {
        dependsOn 'jquery'
        resource url: 'css/jquery-ui-1.10.4.custom.min.css'
        resource url: 'js/jquery-ui-1.10.4.custom.min.js'
        resource url: 'js/i18n/jquery.ui.datepicker-es.js'
    }

    bootstrap {
        dependsOn 'jquery'
        resource url: 'css/bootstrap.min.css'
        resource url: 'js/bootstrap.min.js'
    }

    dynatableModule {
        dependsOn 'jquery'
        resource url: 'css/jquery.dynatable.css'
        resource url: 'js/jquery.dynatable.js'
    }

    multilevel_menu {
        dependsOn 'jquery', 'bootstrap'
        resource url: 'css/jquery.multilevelpushmenu.css'
        resource url: 'js/jquery.multilevelpushmenu.min.js'
    }

    application {
        //if (isDevMode()) {}
        dependsOn 'jquery', 'bootstrap', 'jquery_ui'
        resource url: 'font-awesome/css/font-awesome.min.css'
        resource url: 'css/votingSystem.css'
        resource url: 'js/utils.js.gsp'
        resource url: 'js/pcUtils.js.gsp'
        resource url: 'js/deployJava.js'
    }

	applicationMobile {
        dependsOn 'jquery', 'bootstrap', 'jquery_ui', 'multilevel_menu'
        resource url: 'font-awesome/css/font-awesome.min.css'
        resource url: 'css/votingSystem.css'
        resource url: 'js/utils.js.gsp'
		resource url: 'js/mobileUtils.js.gsp'
	}
	
	textEditorPC {
		dependsOn 'application'
		resource url: 'ckeditor/ckeditor.js'
	}
	
	textEditorMobile {
		dependsOn 'applicationMobile'
		resource url: 'ckeditor/ckeditor.js'
	}
	
	charts {
        resource url: 'font-awesome/css/font-awesome.min.css'
		resource url: 'js/jsapi.js'
		resource url: 'css/charts.css'
	}
}

boolean isDevMode() { !Metadata.current.isWarDeployed() }