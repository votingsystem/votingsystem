package org.sistemavotacion.controlacceso

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService
import org.sistemavotacion.util.*
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.utils.*
import grails.converters.JSON

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 
* TODO inicializar eventsMap en un arranque
*/
class StatisticsService {
	
	
	private final Map<Long, Map<String, Object>> eventsMap =
		new ConcurrentHashMap<Long, Set<String, Object>>();

	def grailsApplication	
	def filesService
	
	private final ExecutorService requestExecutor = Executors.newFixedThreadPool(100);
	// Executors.newCachedThreadPool(null)
	
	public void onApplicationEvent1(final  VotingEvent votingEvent) {
		log.debug(" ----- onApplicationEvent")
		requestExecutor.execute(new Runnable() {
			@Override public void run() {
				updateMetaInf(votingEvent);
			}
		});
	}
	
	public void onApplicationEvent(final  VotingEvent votingEvent) {
		Long eventId = votingEvent.getEvent().id
		log.debug(" ----- onApplicationEvent: ${votingEvent} - eventId: ${eventId}")
		if(eventId != null) {			
			Map<String, Object> eventMap = eventsMap.get(eventId);
			eventMap = getEventMap(votingEvent.getEvent())
			
			if(!eventMap) {
				eventMap = new HashMap<String, Object>();
				eventMap.put("id", eventId);
				eventMap.put("serverURL", "${grailsApplication.config.grails.serverURL}")
				eventMap.put("subject", votingEvent.getEvent().asunto)
				eventMap.put("type", Tipo.EVENTO_VOTACION.toString())
				eventMap.put("dateFinish", DateUtils.getStringFromDate(
					votingEvent.getEvent().getDateFinish()))
				//<representativeId , [optionSelectedId, numVotesFromRepresentative]>
				eventMap.put("representatives", new HashMap<String, RepresentativeData>())
			} 
			Long selectedOptionId = votingEvent.getOptionSelected()?.id
			Long  numVotesOption = 0

			Map<String, Object> repAccreditationsMap = eventMap.get("REPRESENTATIVE_ACCREDITATIONS")
			if(!repAccreditationsMap) {
				repAccreditationsMap = new HashMap<String, Object>();
			}
			Long numUsersWithRepresentativeWithAccessRequest = 
				repAccreditationsMap.get("numUsersWithRepresentativeWithAccessRequest")
			if(!numUsersWithRepresentativeWithAccessRequest) 
				numUsersWithRepresentativeWithAccessRequest = new Long(0)
			Long numRepresentativesWithAccessRequest =
				repAccreditationsMap.get("numRepresentativesWithAccessRequest")
			if(!numRepresentativesWithAccessRequest) numRepresentativesWithAccessRequest = new Long(0)
			Long numRepresentativesWithVote = repAccreditationsMap.get("numRepresentativesWithVote")
			if(!numRepresentativesWithVote) numRepresentativesWithVote = new Long(0)
		
			Map<String, Object> backupMap = eventMap.get("BACKUP")
			if(!backupMap) {
				backupMap = new HashMap<String, Object>();
			} 
			Long numAccessRequest = backupMap.get("numAccessRequest")
			if(!numAccessRequest) numAccessRequest = new Long(0)
			Long numUserVotes = backupMap.get("numUserVotes")
			if(!numUserVotes) numUserVotes = new Long(0)
						
			Map<String, RepresentativeData> representativesMap = eventMap.get("representatives")
			
			switch(votingEvent) {
				case VotingEvent.ACCESS_REQUEST:
					++numAccessRequest
					break;
				case VotingEvent.ACCESS_REQUEST_REPRESENTATIVE:
					++numAccessRequest
					++numRepresentativesWithAccessRequest
					break;
				case VotingEvent.ACCESS_REQUEST_USER_WITH_REPRESENTATIVE:
					String representativeNIF = votingEvent.user.representative.nif
					RepresentativeData repData = representativesMap.get(representativeNIF)
					if(!repData) repData = new RepresentativeData(null, 0) 
					repData.numRepresentations--
					representativesMap.put(representativeNIF, repData)
					++numAccessRequest
					++numUsersWithRepresentativeWithAccessRequest
					break;
				case VotingEvent.VOTE:
					++numUserVotes
					++numVotesOption
					break;
				case VotingEvent.REPRESENTATIVE_VOTE:
					String representativeNIF = votingEvent.user.nif
					RepresentativeData repData = representativesMap.get(representativeNIF) 
					if(!repData) repData = new RepresentativeData(
						selectedOptionId, votingEvent.numRepresentations)
					else {
						repData.optionSelectedId = selectedOptionId
						repData.numRepresentations += votingEvent.numRepresentations
					} 
					
					representativesMap.put(representativeNIF, repData)
					++numRepresentativesWithVote
					break;
			}
			
			if(selectedOptionId != null) {
				if(!eventMap.options) {
					Set<OpcionDeEvento> opciones = votingEvent.getEvent().opciones
					List optionList = new ArrayList()
					for(OpcionDeEvento opcion:opciones) {
						optionList.add([id:opcion.id,
							content:opcion.contenido,
							numUserVotes:0,
							numRepresentativeVotes:0])
					}
					eventMap.options = optionList
				}
				eventMap.options.each {option ->
					if(option.id == selectedOptionId) {
						option.numUserVotes += numVotesOption
					}
					option.numRepresentativeVotes = getNumVotesFromRepresentatives(representativesMap, option.id)
				}
			}
			
			backupMap.put("numUserVotes", numUserVotes)
			backupMap.put("numRepresentativesWithVote", numRepresentativesWithVote)
			
			eventMap.put("representatives", representativesMap)
			
			backupMap.put("numAccessRequest", numAccessRequest)			
			eventMap.put("BACKUP", backupMap)
			
			repAccreditationsMap.put("numUsersWithRepresentativeWithAccessRequest",
				numUsersWithRepresentativeWithAccessRequest)
			repAccreditationsMap.put("numVotesRepresentedByRepresentatives",
				getNumVotesFromRepresentatives(representativesMap))
			repAccreditationsMap.put("numRepresentativesWithAccessRequest",
				numRepresentativesWithAccessRequest)
			repAccreditationsMap.put("numRepresentativesWithVote",
				numRepresentativesWithVote)
			eventMap.put("REPRESENTATIVE_ACCREDITATIONS", repAccreditationsMap)
			
			if(!eventsMap.replace(eventId, eventMap))
				eventsMap.put(eventId, eventMap)
			filesService.updateStatisticsMetaInf(eventMap)
			log.debug(" ----- numAccessRequest: ${numAccessRequest} -  eventsMap.size(): ${eventsMap.size()} - ${eventMap as JSON}")
		}
	}
	
	public Long getNumVotesFromRepresentatives(
			Map<String, RepresentativeData> representativeMap, Long optionId) {
		Long result = new Long(0)
		Collection<RepresentativeData> values = representativeMap.values()
		for(RepresentativeData representativeData:values) {
			if(optionId == representativeData.optionSelectedId &&
				representativeData.numRepresentations > 0) {
				result += representativeData.numRepresentations
			}	
		}
		return result
	}
			
	public Long getNumVotesFromRepresentatives(
			Map<String, RepresentativeData> representativeMap) {
		Long result = new Long(0)
		Collection<RepresentativeData> values = representativeMap.values()
		for(RepresentativeData representativeData:values) {
			if(representativeData.optionSelectedId != null && 
				representativeData.numRepresentations > 0) {
				result += representativeData.numRepresentations
			}
		}
		return result
	}
	
	public Map<String, Object> getEventMap (Evento event) {
		log.debug(" ----- getEventMap eventId: ${event.id}")		
		Map<String, Object> eventMap = eventsMap.get(event.id);
		if(!eventMap) {
			File statisticsFile = filesService.getStatisticsMetaInf(event.id)
			if(!statisticsFile.exists()) return null
			eventMap = parseMetaInf(statisticsFile?.text)
		}
		return eventMap;
	}
	
	
	private Map parseMetaInf(String metaInf) {
		log.debug(" ------  parseMetaInf")
		def metaInfJSON = JSON.parse(metaInf)
		Map resultEventMap = new HashMap()
		resultEventMap.put("id", metaInfJSON.id)
		resultEventMap.put("dateFinish", metaInfJSON.dateFinish)
		resultEventMap.put("subject", metaInfJSON.subject)
		resultEventMap.put("serverURL", metaInfJSON.serverURL)
		Map repAccreditationMap = new HashMap()
		repAccreditationMap.put("numUsersWithRepresentativeWithAccessRequest", 
			metaInfJSON.REPRESENTATIVE_ACCREDITATIONS.numUsersWithRepresentativeWithAccessRequest)
		if(metaInfJSON.REPRESENTATIVE_ACCREDITATIONS.numRepresentatives) {
			repAccreditationMap.put("numRepresentatives",
				metaInfJSON.REPRESENTATIVE_ACCREDITATIONS.numRepresentatives)
		}
		repAccreditationMap.put("numVotesRepresentedByRepresentatives",
			metaInfJSON.REPRESENTATIVE_ACCREDITATIONS.numVotesRepresentedByRepresentatives)
		repAccreditationMap.put("numRepresentativesWithAccessRequest",
			metaInfJSON.REPRESENTATIVE_ACCREDITATIONS.numRepresentativesWithAccessRequest)
		repAccreditationMap.put("numRepresentativesWithVote",
			metaInfJSON.REPRESENTATIVE_ACCREDITATIONS.numRepresentativesWithVote)
		repAccreditationMap.put("numRepresented",
			metaInfJSON.REPRESENTATIVE_ACCREDITATIONS.numRepresented)
		resultEventMap.put("REPRESENTATIVE_ACCREDITATIONS", repAccreditationMap)
		Map backupMap = new HashMap()
		backupMap.put("numVotes", metaInfJSON.BACKUP.numVotes)
		backupMap.put("numAccessRequest", metaInfJSON.BACKUP.numAccessRequest)
		resultEventMap.put("BACKUP", backupMap)
		List optionList = new ArrayList()
		metaInfJSON.options.each {option ->
			optionList.add([id:option.id,
				content:option.content,
				numUserVotes:option.numUserVotes,
				numRepresentativeVotes:option.numRepresentativeVotes])
		}
		
		resultEventMap.put("options", optionList)
		return resultEventMap
	}
	
	public Respuesta updateMetaInf(Evento event) {
		log.debug(" ------  updateMetaInf")
		
	}
	
}