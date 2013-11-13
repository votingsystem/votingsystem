modules = {
	
	masterStyles {
		resource 'css/mobile.css'
		resource 'css/main.css'
	}
	
	jquery {
		resource url: 'js/jquery-1.10.2.min.js'
		resource url: 'js/jquery-ui-1.10.3.custom.min.js'
		resource url: 'css/jquery-ui-1.10.3.custom.min.css'
		resource url: 'js/i18n/jquery.ui.datepicker-es.js'
	}
	
	application {
		//if (isDevMode()) {}
		dependsOn 'masterStyles', 'jquery'
		resource url: 'js/utils.js.gsp'
		resource url: 'js/pcUtils.js.gsp'
	}
	
	
	textEditorPC {
		dependsOn 'masterStyles', 'jquery'
		resource url: 'ckeditor/ckeditor.js'
	}
	

}

boolean isDevMode() { !Metadata.current.isWarDeployed() }