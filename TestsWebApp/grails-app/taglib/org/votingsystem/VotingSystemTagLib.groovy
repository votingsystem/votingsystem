package org.votingsystem

import org.votingsystem.util.StringUtils;
//http://sysgears.com/articles/tag-libraries-grails/
class VotingSystemTagLib {
	
	static namespace = "votingSystem"
	
	def datePicker = { attrs, body ->		
		out << render(template: "/template/taglib/datePicker", model:[attrs: attrs])
	}
	
}
