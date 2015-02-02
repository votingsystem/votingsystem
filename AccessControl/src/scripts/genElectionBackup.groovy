//grails prod run-script src/scripts/genElectionBackup.groovy -DserverURL=http://www.sistemavotacion.org/AccessControl --stacktrace
import grails.util.Metadata
import org.votingsystem.accesscontrol.service.EventVSElectionService
import org.votingsystem.model.EventVSElection

println  "genElectionBackup - Evironment: '${grails.util.Environment.current}' - isWarDeployed: ${Metadata.current.isWarDeployed()}"

String serverURL = System.getProperty('serverURL')
EventVSElectionService eventVSElectionService = ctx.getBean('eventVSElectionService')

EventVSElection eventVS = null;
EventVSElection.withTransaction {eventVS = EventVSElection.get(18L)}
//representativeService.getAccreditationsBackupForEvent(eventVS)
eventVSElectionService.generateBackup(eventVS, serverURL)

println("genElectionBackup - generated backup for eventVS '${eventVS.id}'")