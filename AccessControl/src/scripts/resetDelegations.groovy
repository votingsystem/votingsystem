//grails run-script src/scripts/resetDelegations.groovy --stacktrace
import grails.util.Metadata
import org.votingsystem.accesscontrol.model.RepresentativeDocument
import org.votingsystem.model.RepresentationDocumentVS
import org.votingsystem.model.UserVS

println  "resetDelegations - Evironment: '${grails.util.Environment.current}' - isWarDeployed: ${Metadata.current.isWarDeployed()}"

List<UserVS> usersVS = UserVS.findAll()
usersVS.each { user ->
    user.type = UserVS.Type.USER
    user.representative = null
    user.metaInf = null
    List<RepresentationDocumentVS> repDocsFromUser
    RepresentationDocumentVS.withTransaction {
        repDocsFromUser = RepresentationDocumentVS.findAllWhere(userVS:user)
        repDocsFromUser.each { repDocFromUser ->
            repDocFromUser.dateCanceled = Calendar.getInstance().getTime()
            repDocFromUser.setState(RepresentationDocumentVS.State.CANCELLED).save()
        }
    }
    String userId = String.format('%05d', user.id)
    println("resetDelegations - user: ${userId} of ${usersVS.size()} - num. RepresentationDocumentVS: ${repDocsFromUser.size()}");
}

int numRepresentativeDocumentChanges = 0;
List<RepresentativeDocument> repDocs = RepresentativeDocument.createCriteria().list {
    inList("state", [RepresentativeDocument.State.OK, RepresentativeDocument.State.RENEWED])
}
for(RepresentativeDocument repDoc:repDocs) {
    repDoc.userVS.setType(UserVS.Type.USER).save()
    repDoc.setState(RepresentativeDocument.State.CANCELLED).save()
    numRepresentativeDocumentChanges++
}
println "resetDelegations - numRepresentativeDocumentChanges $numRepresentativeDocumentChanges"