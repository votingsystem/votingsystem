package org.votingsystem.controlcenter.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS

class ElementController {

    def index() {
        render (view:params.element)
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
