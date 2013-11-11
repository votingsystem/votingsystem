package org.votingsystem.controlcenter.controller

import grails.util.Metadata
import org.bouncycastle.jce.provider.BouncyCastleProvider

class TestingController {

	def grailsApplication
	
    def index() { 
		//render "${Metadata.current.isWarDeployed()}"
	}
	
	def android() {
		render org.bouncycastle.tsp.TSPAlgorithms.SHA256
		return false
	}
}
