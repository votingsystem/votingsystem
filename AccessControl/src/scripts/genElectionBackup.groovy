//grails run-script src/scripts/genElectionBackup.groovy --stacktrace
import grails.util.Metadata
import org.votingsystem.accesscontrol.service.EventVSElectionService
import org.votingsystem.accesscontrol.service.KeyStoreService
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.FileUtils

println  "genElectionBackup - Evironment: '${grails.util.Environment.current}' - isWarDeployed: ${Metadata.current.isWarDeployed()}"

EventVSElectionService eventVSElectionService = ctx.getBean('eventVSElectionService')

EventVSElection eventVS = null;
EventVSElection.withTransaction {eventVS = EventVSElection.get(18L)}
//representativeService.getAccreditationsBackupForEvent(eventVS)
eventVSElectionService.generateBackup(eventVS)

println("genElectionBackup - generated backup for eventVS '${eventVS.id}'")