package org.votingsystem


//http://sysgears.com/articles/tag-libraries-grails/
class VotingSystemTagLib {
	
	static namespace = "votingSystem"
	
	def simpleButton = { attrs, body ->		
		attrs.message = body()
		out << render(template: "/template/taglib/buttonTemplate", model:[attrs: attrs])
	}
	
	def datePicker = { attrs, body ->
				
		out << render(template: "/template/taglib/datePickerTemplate", model:[attrs: attrs])
	}
	
}
