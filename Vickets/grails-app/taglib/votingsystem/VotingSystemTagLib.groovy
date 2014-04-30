package votingsystem

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

    def textEditor = {attrs, body ->
        out << render(template: "/template/taglib/textEditor", model:[attrs: attrs])
    }

    def feed = {attrs, body ->
        attrs.message = body()
        out << render(template: "/template/taglib/feed", model:[attrs: attrs])
    }

}
