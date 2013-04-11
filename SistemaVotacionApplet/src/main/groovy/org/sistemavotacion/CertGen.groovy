package org.sistemavotacion

import java.io.File;
import groovy.json.JsonSlurper
import org.sistemavotacion.CertGenerator


def processCertsDataMap(args) {
	println "++++++++++++++++++++ CertGen.processCertsDataMap(...)"
	def cli = new CliBuilder(usage: 'CertGen.groovy -[h] [certsDataMap]')
	cli.with {
		h longOpt: 'help', "certsDataMap -> JSON document with this format -> " + 
			"{\"rootCertFile\":\"...\",\"rootSubjectDN\":\"...\",\"password\":\"...\"," + 
			"\"certs\":[{\"file\":\"...\",\"distinguishedName\":\"...\",\"alias\":\"...\"," + 
			"\"isTimeStampingCert\":\"...\"}]}"
	}
	
	def options = cli.parse(args)
	// Show usage text when -h or --help option is used.
	if (options.h) {
		cli.usage()
		return
	}
	
	def extraArguments = options.arguments()
	
	def jsonCertsData = new JsonSlurper().parseText(extraArguments[0])
	
	if(!jsonCertsData.rootCertFile || !jsonCertsData.rootSubjectDN
		|| !jsonCertsData.password || !jsonCertsData.certs) {
		if(!jsonCertsData.rootCertFile ) println "missing rootCertFile"
		if(!jsonCertsData.rootSubjectDN ) println "missing rootSubjectDN"
		if(!jsonCertsData.password ) println "missing password"
		cli.usage(); 
		return;
	}
	File rootCertFile = new File(jsonCertsData.rootCertFile);
	String rootSubjectDN = jsonCertsData.rootSubjectDN;
	String password  = jsonCertsData.password;
	
	CertGenerator cerGenerator = new CertGenerator(rootCertFile, rootSubjectDN, password);
	
	jsonCertsData.certs.each {certData ->
		if(!certData.file || !certData.distinguishedName
			|| !certData.alias || !certData.containsKey("isTimeStampingCert")) {
			println "certData: ${certData}"
			if(!certData.file) println "missing file"
			if(!certData.distinguishedName) println "missing distinguishedName"
			if(!certData.alias) println "missing alias"
			if(!certData.isTimeStampingCert) println "missing Time Stamp info"
			cli.usage(); 
			return;
		}
		File certFile = new File(certData.file);
		String distinguishedName = certData.distinguishedName;
		String alias = certData.alias;
		boolean isTimeStampingCert = certData.isTimeStampingCert
		if(isTimeStampingCert) {
			cerGenerator.genTimeStampingKeyStore(
				distinguishedName, certFile, alias);
		} else {
			cerGenerator.genUserKeyStore(
				distinguishedName, certFile, alias);
		}
	}
}

processCertsDataMap(args)

