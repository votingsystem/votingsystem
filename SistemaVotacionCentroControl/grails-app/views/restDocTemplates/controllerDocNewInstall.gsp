<% def escapedServerURLStr = "\${request.scheme}://\${request.serverName}:\${request.serverPort}\${request.getContextPath()}"
def controllerDescription = controllerDoc?.descController
def controllerActions = controllerDoc?.controllerActions %>

        <style type="text/css" media="screen">
         .controllerInfoHeader {
	     	color: #4c4c4c;
	     }
	     .params_result_div {
	     	margin:0px 0px 0px 20px;
	     }
	     .serviceURLS {
	     	margin:10px 0px 20px 200px;
	     	color: #4c4c4c;
	     }
        </style>

<h3 class="controllerInfoHeader"><u>${controllerDoc?.getInfoController()}</u></h3>

<g:if test="${controllerDescription}">${controllerDescription}</g:if>

<div>

	<HR>
	<g:each in="${controllerActions}">
		<% def commentDoc = it?.commentDoc
		   def paramsMap = commentDoc?.paramsMap
		   def eTagsMap = commentDoc?.getETagsMap()
		   def requestHeaderMap = commentDoc?.getRequestHeaderMap()
		   def responseHeaderMap = commentDoc?.getResponseHeaderMap()
		   def urlSufixMap = commentDoc?.getUrlSufixMap()
	       def httpMethod = commentDoc?.httpMethod?.toUpperCase()
	       def description = commentDoc?.description
	       def result = commentDoc?.result%>
		
			<p>
				<g:if test="${httpMethod}">- <u>${httpMethod}</u> - </g:if>
				<a href="${escapedServerURLStr}${it?.uri}">${it?.uri}</a><br/>
				<g:if test="${description}">${description}<br/></g:if>
			</p>
			<div class="params_result_div">
			
			<g:if test="${urlSufixMap}">
				<p>
					<b><g:message code="urlSufixMsg"/>:</b><br/>
					<g:each in="${urlSufixMap?.keySet()}" var="sufixKey">
						<u>${sufixKey}</u>: ${urlSufixMap.get(sufixKey)}<br/>
					</g:each>
				</p>
			</g:if>
			
			<g:if test="${paramsMap}">
				<p>
					<b><g:message code="paramsMsg"/>:</b><br/>
					<g:each in="${paramsMap?.keySet()}" var="paramKey">
						<u>${paramKey}</u>: ${paramsMap.get(paramKey)}<br/>
					</g:each>
				</p>
			</g:if>
			
			<g:if test="${eTagsMap}">
				<p>
					<b><g:message code="eTagsMsg"/>:</b><br/>
					<g:each in="${eTagsMap?.keySet()}" var="etagKey">
						<u>${etagKey}</u>: ${eTagsMap.get(etagKey)}<br/>
					</g:each>
				</p>
			</g:if>
				
			<g:if test="${requestHeaderMap}">
				<p>
					<b><g:message code="requestHeaderMsg"/>:</b><br/>
					<g:each in="${requestHeaderMap?.keySet()}"  var="requestHeaderKey">
						<u>${requestHeaderKey}</u>: ${requestHeaderMap.get(requestHeaderKey)}<br/>
					</g:each>
				</p>
			</g:if>
			
			<g:if test="${responseHeaderMap}">
				<p>
					<b><g:message code="responseHeaderMsg"/>:</b><br/>
					<g:each in="${responseHeaderMap?.keySet()}"  var="responseHeaderKey">
						<u>${responseHeaderKey}</u>: ${responseHeaderMap.get(responseHeaderKey)}<br/>
					</g:each>
				</p>
			</g:if> 
			
			<g:if test="${result}">
				<p><b><g:message code="responseMsg"/>:</b><br/>${result}</p>
			</g:if>
			</div>
		<HR>
	</g:each>

</div>