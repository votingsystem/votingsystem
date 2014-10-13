// File: src/scripts/appstatus.groovy
import grails.util.Metadata
import org.votingsystem.model.UserVS

println  "Evironment: '${grails.util.Environment.current}' - isWarDeployed: ${Metadata.current.isWarDeployed()}"
UserVS user = UserVS.get(1L)
println "User: " + user.getName()