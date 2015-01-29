//grails run-script src/scripts/resetDelegations.groovy --stacktrace
import grails.util.Metadata
import org.votingsystem.accesscontrol.model.RepresentativeDocument
import org.votingsystem.model.RepresentationDocumentVS
import org.votingsystem.model.UserVS

println  "resetDelegations - Evironment: '${grails.util.Environment.current}' - isWarDeployed: ${Metadata.current.isWarDeployed()}"

List<UserVS> userList = UserVS.findAll()
for(UserVS user : userList) {
    if(UserVS.Type.SYSTEM == user.type) continue
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
    println("resetDelegations - user: ${userId} of ${userList.size()} - num. RepresentationDocumentVS: ${repDocsFromUser.size()}");
}

int numRepresentativeDocumentChanges = 0;
RepresentationDocumentVS.withTransaction {
    List<RepresentativeDocument> repDocs = RepresentativeDocument.createCriteria().list {
        inList("state", [RepresentativeDocument.State.OK, RepresentativeDocument.State.RENEWED])
    }
    for(RepresentativeDocument repDoc:repDocs) {
        repDoc.userVS.setType(UserVS.Type.USER).save()
        repDoc.setState(RepresentativeDocument.State.CANCELLED).save()
        numRepresentativeDocumentChanges++
    }
}
println "resetDelegations - numRepresentativeDocumentChanges $numRepresentativeDocumentChanges"