package org.votingsystem

//http://sysgears.com/articles/tag-libraries-grails/
class VotingSystemTagLib {
	
	static namespace = "votingSystem"
	
	def simpleButton = { attrs, body ->		
		attrs.message = body()
		out << render(template: "/template/taglib/button", model:[attrs: attrs])
	}
	
	def textEditor = {attrs, body ->
		out << render(template: "/template/taglib/textEditorPC", model:[attrs: attrs])
	}
	
	def datePicker = { attrs, body ->		
		out << render(template: "/template/taglib/datePicker", model:[attrs: attrs])
	}
	
}
