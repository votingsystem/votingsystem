modules = {

    baseApp {
        dependsOn 'jquery'
        resource url: 'css/jquery.multilevelpushmenu.css'
        resource url: 'font-awesome/css/font-awesome.min.css'
        resource url: 'js/utils.js.gsp'
        resource url: 'css/bootstrap.min.css'
        resource url: 'js/bootstrap.min.js'
        resource url: 'js/jquery.multilevelpushmenu.min.js'
        resource 'css/testWebApp.css'
    }

    application {
        //if (isDevMode()) {}
        dependsOn 'baseApp'
        resource url: 'js/pcUtils.js.gsp'
    }

	jquery {
		resource url: 'js/jquery-1.11.0.min.js'
		resource url: 'js/jquery-ui-1.10.4.custom.min.js'
		resource url: 'css/jquery-ui-1.10.4.custom.min.css'
		resource url: 'js/i18n/jquery.ui.datepicker-es.js'
	}
	
	textEditorPC {
		dependsOn 'baseApp'
        resource url: 'js/pcUtils.js.gsp'
		resource url: 'ckeditor/ckeditor.js'
	}

}

boolean isDevMode() { !Metadata.current.isWarDeployed() }