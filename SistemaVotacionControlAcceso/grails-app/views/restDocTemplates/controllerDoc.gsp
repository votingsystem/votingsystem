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

<u><h3 class="controllerInfoHeader">${controllerDoc?.getInfoController()}</h3></u>

<g:if test="${controllerDescription}">${controllerDescription}</g:if>

<div>

	<HR>
	<g:each in="${controllerActions}">
		<% def commentDoc = it?.commentDoc
		   def paramsMap = commentDoc?.paramsMap		   
	       def httpMethod = commentDoc?.httpMethod?.toUpperCase()
	       def description = commentDoc?.description
	       def result = commentDoc?.result%>
		
			<p>
				<g:if test="${httpMethod}">- <u>${httpMethod}</u> - </g:if>
				<a href="${escapedServerURLStr}${it?.uri}">${it?.uri}</a><br/>
				<g:if test="${description}">${description}<br/></g:if>
			</p>
			<div class="params_result_div">
			<g:if test="${paramsMap}">
				<p>
					<b><g:message code="paramsMsg"/>:</b><br/>
					<g:each in="${paramsMap?.keySet()}">
						<u>${it}</u>: ${paramsMap.get(it)}<br/>
					</g:each>
				</p>
			</g:if>
			</p>
	
			<g:if test="${result}">
				<p><b><g:message code="responseMsg"/>:</b><br/>${result}</p>
			</g:if>
			</div>
		<HR>
	</g:each>

</div>