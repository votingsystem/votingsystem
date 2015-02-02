//grails prod run-script src/scripts/genElectionBackup.groovy -DserverURL=http://www.sistemavotacion.org/AccessControl -DeventId=1 --stacktrace
import grails.util.Metadata
import org.votingsystem.accesscontrol.service.EventVSElectionService
import org.votingsystem.model.EventVSElection

String serverURL = System.getProperty('serverURL')
Long eventId = System.getProperty('eventId')? Long.valueOf(System.getProperty('eventId')) : null

println "genElectionBackup - serverURL: $serverURL - eventId:$eventId - Evironment: '${grails.util.Environment.current}' " +
        "- isWarDeployed: ${Metadata.current.isWarDeployed()}"

EventVSElectionService eventVSElectionService = ctx.getBean('eventVSElectionService')

EventVSElection eventVS = null;
EventVSElection.withTransaction {eventVS = EventVSElection.get(eventId)}
//representativeService.getAccreditationsBackupForEvent(eventVS)
eventVSElectionService.generateBackup(eventVS, serverURL)

println("genElectionBackup - generated backup for eventVS '${eventVS.id}'")