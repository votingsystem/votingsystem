package org.votingsystem

class VotingSystemDialogTagLib {
	
	static namespace = "votingSystem"

	def browserWithoutJavaDialog = {attrs, body ->
		out << render(template: "/includes/dialog/browserWithoutJava", model: [:]).replace("\n","")
	}
	
	def loadingAppletDialog = {attrs, body ->
		out << render(template: "/includes/dialog/loadingApplet", model: [:]).replace("\n","")
	}
	
	def workingWithAppletDialog = {attrs, body ->
		out << render(template: "/includes/dialog/workingWithApplet", model: [:]).replace("\n","")
	}
	
}
