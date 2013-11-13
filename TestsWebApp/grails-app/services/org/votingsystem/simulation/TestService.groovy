package org.votingsystem.simulation

import grails.transaction.Transactional

@Transactional
class TestService {

	def simulationService
    
	def sendeMessageToBrowsers(String msg) {
		log.debug("############## sendeMessageToBrowsers")
		//simulationService.broadcast(msg)
    }
}
