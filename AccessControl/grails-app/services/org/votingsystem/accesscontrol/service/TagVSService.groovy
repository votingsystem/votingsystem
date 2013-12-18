package org.votingsystem.accesscontrol.service

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.TagVS

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class TagVSService {
	
	static transactional = true

	Set<TagVS> save(JSONArray tags) {
		log.debug("save - tags: ${tags}")
		def tagSet = tags.findAll {it != JSONObject.NULL  && !it.toString().isEmpty()}.collect { tagItem ->
			tagItem = tagItem.toLowerCase().trim()
			def tag
			TagVS.withTransaction {
				tag = TagVS.findByName(tagItem)
				if (tag) {
					tag.frequency +=1
					tag.save(flush: true)
				} else {
					tag = new TagVS(name:tagItem, frequency:1)
					tag.save(flush: true)
				}
			}
			return tag;
		}
		return etiquetaSet
	}
	
}

