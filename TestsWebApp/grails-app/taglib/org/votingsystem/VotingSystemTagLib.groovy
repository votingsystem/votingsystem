package org.votingsystem

import org.votingsystem.util.StringUtils;
//http://sysgears.com/articles/tag-libraries-grails/
class VotingSystemTagLib {
	
	static namespace = "votingSystem"
	
	def simpleButton = { attrs, body ->		
		attrs.message = body()
        if(!attrs.id) attrs.id = StringUtils.getRandomAlphaNumeric(3)
		out << render(template: "/template/taglib/button", model:[attrs: attrs])
	}
	
	def textEditor = {attrs, body ->
		out << render(template: "/template/taglib/textEditorPC", model:[attrs: attrs])
	}
	
	def datePicker = { attrs, body ->		
		out << render(template: "/template/taglib/datePicker", model:[attrs: attrs])
	}
	
}
