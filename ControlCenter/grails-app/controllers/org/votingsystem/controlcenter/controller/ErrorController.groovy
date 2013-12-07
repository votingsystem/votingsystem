package org.votingsystem.controlcenter.controller

import org.votingsystem.model.ResponseVS

class ErrorController {

    def index() {
        //Exception exception = request.exception
        params.responseVS = new ResponseVS(ResponseVS.SC_EXCEPTION, message(code: 'requestWithErrors'))
    }

}
