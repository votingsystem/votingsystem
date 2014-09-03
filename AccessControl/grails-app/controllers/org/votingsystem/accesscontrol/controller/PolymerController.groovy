package org.votingsystem.accesscontrol.controller

class PolymerController {

    def index() {
        render (view:params.element)
    }
}
