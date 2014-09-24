package org.votingsystem.vicket.controller

class ElementController {

    def index() {
        render (view:params.element)
    }
}
