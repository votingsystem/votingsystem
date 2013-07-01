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
	def eventoService
	
	private final ExecutorService requestExecutor = Executors.newFixedThreadPool(100);
	// Executors.newCachedThreadPool(null)
	
	public void onApplicationEvent1(final  VotingEvent votingEvent) {
		log.debug(" ----- onApplicationEvent")
		requestExecutor.execute(new Runnable() {
			@Override public void run() { }
		});
	}
	
	public void onApplicationEvent(final  VotingEvent votingEvent) {
		Long eventId = votingEvent?.getEvent()?.id
		log.debug(" ----- onApplicationEvent: ${votingEvent} - eventId: ${eventId}")
		if(eventId != null) {		
			Map<String, Object> eventMap = eventsMap.get(votingEvent?.getEvent().id);
			if(!eventMap)  eventMap = eventoService.getEventMetaInfMap(votingEvent?.getEvent())

			Long selectedOptionId = votingEvent.getOptionSelected()?.id
			Long  numVotesOption = 0

			Map<String, Object> repAccreditationsMap = eventMap.get("REPRESENTATIVE_ACCREDITATIONS")
			if(!repAccreditationsMap) {
				repAccreditationsMap = [:];
			}
			Long numRepresentedWithAccessRequest = 
				repAccreditationsMap.get("numRepresentedWithAccessRequest")
			if(!numRepresentedWithAccessRequest) 
				numRepresentedWithAccessRequest = new Long(0)
			Long numRepresentativesWithAccessRequest =
				repAccreditationsMap.get("numRepresentativesWithAccessRequest")
			if(!numRepresentativesWithAccessRequest) numRepresentativesWithAccessRequest = new Long(0)
			Long numRepresentativesWithVote = repAccreditationsMap.get("numRepresentativesWithVote")
			if(!numRepresentativesWithVote) numRepresentativesWithVote = new Long(0)
		
			
			Map<String, Object> backupMap = eventMap.get("BACKUP")
			if(!backupMap) {
				backupMap =[:];
				backupMap.numUserVotes = 0L
			}
			Long numUserVotes = backupMap.numUserVotes;
						
			Map<String, RepresentativeData> representativesMap = eventMap.get("representatives")
			
			switch(votingEvent) {
				case VotingEvent.ACCESS_REQUEST:
					break;
				case VotingEvent.ACCESS_REQUEST_REPRESENTATIVE:
					++numRepresentativesWithAccessRequest
					break;
				case VotingEvent.ACCESS_REQUEST_USER_WITH_REPRESENTATIVE:
					String representativeNIF = votingEvent.user.representative.nif
					RepresentativeData repData = representativesMap.get(representativeNIF)
					if(!repData) repData = new RepresentativeData() 
					++repData.numRepresentedWithVote
					representativesMap.put(representativeNIF, repData)
					++numRepresentedWithAccessRequest
					break;
				case VotingEvent.VOTE:
					++numUserVotes
					++numVotesOption
					break;
				case VotingEvent.REPRESENTATIVE_VOTE:
					String representativeNIF = votingEvent.user.nif
					RepresentativeData repData = representativesMap.get(representativeNIF) 
					if(!repData) repData = new RepresentativeData(votingEvent.user.id,
						representativeNIF, selectedOptionId)
					else {
						repData.optionSelectedId = selectedOptionId
						repData.nif = representativeNIF
						repData.id = votingEvent.user.id
					} 
					
					representativesMap.put(representativeNIF, repData)
					++numRepresentativesWithVote
					break;
			}
			
			representativesMap?.values()?.each {repData ->
				Usuario representative = Usuario.get(repData.id)
				if(representative) repData.numTotalRepresentations =
					Usuario.countByRepresentative(representative) + 1//plus the representative itself
			}
			
			if(selectedOptionId != null) {
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
			
			Long numAccessRequest = SolicitudAcceso.countByEstadoAndEventoVotacion(
				SolicitudAcceso.Estado.OK, votingEvent?.getEvent())
			
			backupMap.put("numAccessRequest", numAccessRequest)			
			eventMap.put("BACKUP", backupMap)
			
			repAccreditationsMap.put("numRepresentedWithAccessRequest",
				numRepresentedWithAccessRequest)
			repAccreditationsMap.put("numVotesRepresentedByRepresentatives",
				getNumVotesFromRepresentatives(representativesMap))
			repAccreditationsMap.put("numRepresentativesWithAccessRequest",
				numRepresentativesWithAccessRequest)
			repAccreditationsMap.put("numRepresentativesWithVote",
				numRepresentativesWithVote)
			eventMap.put("REPRESENTATIVE_ACCREDITATIONS", repAccreditationsMap)
			
			if(!eventsMap.replace(eventId, eventMap)) eventsMap.put(eventId, eventMap)

			eventoService.updateEventMetaInf(votingEvent?.getEvent(), eventMap)
			log.debug(" --- ${eventMap as JSON}")
		}
	}
	
	public Long getNumVotesFromRepresentatives(
			Map<String, RepresentativeData> representativeMap, Long optionId) {
		Long result = new Long(0)
		Collection<RepresentativeData> values = representativeMap.values()
		for(RepresentativeData representativeData:values) {
			if(optionId == representativeData.optionSelectedId) {
				result += representativeData.numTotalRepresentations - 
					representativeData.numRepresentedWithVote;
			}	
		}
		return result
	}
			
	public Long getNumVotesFromRepresentatives(
			Map<String, RepresentativeData> representativeMap) {
		Long result = new Long(0)
		Collection<RepresentativeData> values = representativeMap.values()
		for(RepresentativeData representativeData:values) {
			if(representativeData.optionSelectedId != null) {
				result = result + representativeData.numTotalRepresentations - 
					representativeData.numRepresentedWithVote;
			}
		}
		return result
	}


	
}