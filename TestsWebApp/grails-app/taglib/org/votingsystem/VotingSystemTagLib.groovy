package org.votingsystem

import org.votingsystem.util.StringUtils;
//http://sysgears.com/articles/tag-libraries-grails/
class VotingSystemTagLib {
	
	static namespace = "votingSystem"
	
	def textEditor = {attrs, body ->
		out << render(template: "/template/taglib/textEditorPC", model:[attrs: attrs])
	}
	
	def datePicker = { attrs, body ->		
		out << render(template: "/template/taglib/datePicker", model:[attrs: attrs])
	}
	
}
