package org.votingsystem

//http://sysgears.com/articles/tag-libraries-grails/
class VotingSystemTagLib {
	
	static namespace = "votingSystem"
	
	def simpleButton = { attrs, body ->		
		def button = [id:attrs.id, href: attrs.href, imgSrc:attrs.imgSrc, message:body(), 
			style:attrs.style, isButton:attrs.isButton]
		out << render(template: "/template/taglib/buttonTemplate", model:[button: button])
	}
	
}
