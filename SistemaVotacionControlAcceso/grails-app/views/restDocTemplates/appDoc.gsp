<% def escapedServerURLStr = "\${request.scheme}://\${request.serverName}:\${request.serverPort}\${request.getContextPath()}"%>
<!DOCTYPE html>
<html>
    <head>
        <title>${message(code: 'restDocMainPageTittle', null)}</title>
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
    </head>
    <body>

<g:each in="${controllerDocs}">

<% def controllerDescription = it.descController
def controllerActions = it.controllerActions %>

<u><h3 class="controllerInfoHeader">${it?.getInfoController()}</h3></u>

<g:if test="${controllerDescription}">${controllerDescription}</g:if>

<div>

	<HR>
	<g:each status="i" in="${controllerActions}" var="it">
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
					<g:each in="${paramsMap?.keySet()}"  var="paramKey">
						<u>${paramKey}</u>: ${paramsMap.get(paramKey)}<br/>
					</g:each>
				</p>
			</g:if>
			</p>
	
			<g:if test="${result}">
				<p><b><g:message code="responseMsg"/>:</b><br/>${result}</p>
			</g:if>
			</div>
		<HR ${ (i == controllerActions?.size() -1) ? "color='#4c4c4c'" : ""}>
	</g:each>

</div>
</g:each>
    
    </body>
</html>