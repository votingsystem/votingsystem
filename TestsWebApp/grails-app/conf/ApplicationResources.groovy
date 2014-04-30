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

    multilevel_menu {
        dependsOn 'jquery', 'bootstrap'
        resource url: 'css/jquery.multilevelpushmenu.css'
        resource url: 'js/jquery.multilevelpushmenu.min.js'
    }

    application {
        //if (isDevMode()) {}
        dependsOn 'jquery', 'bootstrap', 'jquery_ui'
        resource url: 'font-awesome/css/font-awesome.min.css'
        resource 'css/testWebApp.css'
        resource url: 'js/utils.js.gsp'
    }

	
	textEditorPC {
		resource url: 'ckeditor/ckeditor.js'
	}

}

boolean isDevMode() { !Metadata.current.isWarDeployed() }