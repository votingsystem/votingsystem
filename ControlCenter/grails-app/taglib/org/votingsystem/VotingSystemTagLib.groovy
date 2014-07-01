package org.votingsystem

import org.votingsystem.util.StringUtils;
//http://sysgears.com/articles/tag-libraries-grails/
class VotingSystemTagLib {
	
	static namespace = "votingSystem"

    def timePicker = { attrs, body ->
        attrs.message = body()
        out << render(template: "/template/taglib/timePicker", model:[attrs: attrs])
    }

    def datePicker = { attrs, body ->
        attrs.message = body()
        out << render(template: "/template/taglib/datePicker", model:[attrs: attrs])
    }


}
