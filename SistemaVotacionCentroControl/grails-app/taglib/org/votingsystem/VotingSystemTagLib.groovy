package org.votingsystem

//http://sysgears.com/articles/tag-libraries-grails/
class VotingSystemTagLib {
	
	static namespace = "votingSystem"
	
	def simpleButton = { attrs, body ->		
		def button = [id:attrs.id, href: attrs.href, imgSrc:attrs.imgSrc, message:body(), 
			style:attrs.style, isButton:attrs.isButton]
		out << render(template: "/includes/buttonTemplate", model:[button: button])
	}
	
	def event = {attrs, body ->
		def event = [id: attrs.id, subject:attrs.subject, 
			user:attrs.user, dateInit:attrs.dateInit, elapsedTime:attrs.elapsedTime,
			state:attrs.state, isTemplate:attrs.isTemplate]		
		def template =  render(template: "/includes/eventTemplate",model: [event: event])
		out << template.replace("\n","")
	}
	
	def newField = {attrs, body ->
		out << render(template: "/includes/newFieldTemplate", model: [:]).replace("\n","")
	}
	
	def representative = {attrs, body ->
		out << render(template: "/includes/representativeTemplate", model: [:]).replace("\n","")
	} 
	
	def tabProgresTemplate = {attrs, body ->
		out << render(template: "/includes/tabProgresTemplate", model: [:]).replace("\n","")
	}
	
}
