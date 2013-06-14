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
			@Override public void run() { }
		});
	}
	
	public void onApplicationEvent(final  VotingEvent votingEvent) {
		Long eventId = votingEvent?.getEvent()?.id
		log.debug(" ----- onApplicationEvent: ${votingEvent} - eventId: ${eventId}")
		if(eventId != null) {			
			Map<String, Object> eventMap = getEventMap(votingEvent.getEvent());
			
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
			Long numUserVotes = backupMap.get("numUserVotes")
			if(!numUserVotes) numUserVotes = new Long(0)
						
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
					++numUsersWithRepresentativeWithAccessRequest
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

	public Map getEventMap(Evento event) {
		Map<String, Object> eventMap = eventsMap.get(event.id);
		if(!eventMap) {
			File statisticsFile = filesService.getStatisticsMetaInf(event.id)
			if(statisticsFile.exists())	eventMap = JSON.parse(statisticsFile?.text)
		}
		if(!eventMap) {
			eventMap = new HashMap<String, Object>();
			eventMap.put("id", event.id);
			eventMap.put("serverURL", "${grailsApplication.config.grails.serverURL}")
			eventMap.put("subject", event.asunto)
			eventMap.put("type", Tipo.EVENTO_VOTACION.toString())
			eventMap.put("dateFinish", DateUtils.getStringFromDate(event.getDateFinish()))
			eventMap.put("representatives", new HashMap<String, RepresentativeData>())
			if(event instanceof EventoVotacion) {
				Set<OpcionDeEvento> opciones = ((EventoVotacion)event).opciones
				List optionList = new ArrayList()
				for(OpcionDeEvento opcion:opciones) {
					optionList.add([id:opcion.id,
						content:opcion.contenido,
						numUserVotes:0,
						numRepresentativeVotes:0])
				}
				eventMap.put("options", optionList) 
			}
		}
		return eventMap
	}
	
}