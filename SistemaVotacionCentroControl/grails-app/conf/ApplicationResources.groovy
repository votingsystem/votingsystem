modules = {
	
	'masterStyles' {
		resource 'css/pcVotingSystem.css'
		resource 'css/votingSystem.css'
	}
	
	'masterStylesMobile' {
		resource 'css/mobileVotingSystem.css'
		resource 'css/votingSystem.css'
	}
	
	'jquery' {
		resource url: 'js/jquery-1.10.2.min.js'
		resource url: 'js/jquery-ui-1.10.3.custom.min.js'
		resource url: 'css/jquery-ui-1.10.3.custom.min.css'
		resource url: 'js/i18n/jquery.ui.datepicker-es.js'
	}
	
	'application' {
		//if (isDevMode()) {}
		dependsOn 'masterStyles', 'jquery'
		resource url: 'js/utils.js.gsp'
		resource url: 'js/pcUtils.js.gsp'
		resource url: 'js/deployJava.js'
	}
	
	'applicationMobile' {
		dependsOn 'masterStylesMobile', 'jquery'
		resource url: 'js/utils.js.gsp'
		resource url: 'js/mobileUtils.js.gsp'
	}
	
	'textEditorPC' {
		dependsOn 'masterStyles', 'application'
		resource url: 'ckeditor/ckeditor.js'
	}
	
	'textEditorMobile' {
		dependsOn 'masterStyles', 'applicationMobile'
		resource url: 'ckeditor/ckeditor.js'
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