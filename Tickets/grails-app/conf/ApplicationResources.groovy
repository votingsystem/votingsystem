modules = {

    masterStyles {
        resource 'css/mobile.css'
        resource 'css/tickets.css'
    }

    jquery {
        resource url: 'js/jquery-1.11.0.min.js'
        resource url: 'js/jquery-ui-1.10.4.custom.min.js'
        resource url: 'css/jquery-ui-1.10.4.custom.min.css'
        resource url: 'js/i18n/jquery.ui.datepicker-es.js'
    }

    application {
        //if (isDevMode()) {}
        dependsOn 'masterStyles', 'jquery'
        resource url: 'js/utils.js.gsp'
        resource url: 'js/pcUtils.js.gsp'
    }

    multilevelmenu {
        dependsOn 'masterStyles', 'jquery'
        resource url: 'font-awesome/css/font-awesome.min.css'
        resource url: 'css/jquery.multilevelpushmenu.css'
        resource url: 'css/bootstrap.min.css'
        resource url: 'js/bootstrap.min.js'
        resource url: 'js/utils.js.gsp'
        resource url: 'js/pcUtils.js.gsp'
        resource url: 'js/jquery.multilevelpushmenu.min.js'

    }

    dynatableModule {
        dependsOn 'multilevelmenu'
        resource url: 'css/jquery.dynatable.css'
        resource url: 'js/jquery.dynatable.js'
        resource url: 'js/jquery.stickytableheaders.js'
    }
}

boolean isDevMode() { !Metadata.current.isWarDeployed() }