<g:if test="${request.getHeader("user-agent").toLowerCase().contains('android')}">
    <div class="userAdvert text-left">
		<ul>
			<li><g:message code="androidAppNeededMsg"/></li>
			<li><g:message code="androidAppDownloadMsg"  args="${[grailsApplication.config.grails.serverURL + "/android/SistemaVotacion.apk"]}"/></li>
			<li><g:message code="androidCertInstalledMsg"/></li>
			<li><g:message code="androidSelectAppMsg"/></li>
		</ul>		
	</div>	
</g:if>
<g:else>
    <div class="userAdvert text-left">
		<ul>
			<g:each in="${advices}">
				<li>${it}</li>
			</g:each>
			<li><g:message code="dniConnectedMsg"/></li>
			<li><g:message code="appletAdvertMsg"/></li>
			<li><g:message code="javaInstallAdvertMsg"/></li>
		</ul>
	</div>	
</g:else>


	
