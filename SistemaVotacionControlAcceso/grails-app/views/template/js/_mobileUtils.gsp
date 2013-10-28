<title>${message(code: 'nombreServidorLabel', null)}</title>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width" />
<meta name="HandheldFriendly" content="true" />
<script src="${resource(dir:'js',file:'jquery-1.10.2.min.js')}"></script>  
<script src="${resource(dir:'js',file:'jquery-ui-1.10.3.custom.min.js')}"></script>
<link rel="stylesheet" href="${resource(dir:'css',file:'jquery-ui-1.10.3.custom.min.css')}">  
<script src="${resource(dir:'js/i18n',file:'jquery.ui.datepicker-es.js')}"></script>
<link rel="stylesheet" href="${resource(dir:'css',file:'mobileVotingSystem.css')}">
<g:render template="/template/js/utils"/>
<script type="text/javascript">
			
//"yy/MM/dd 12:00:00"
var pickerOpts = {showOn: 'both', buttonImage: "${createLinkTo(dir: 'images', file: 'appointment.png')}", 
		buttonImageOnly: true, dateFormat: 'yy/MM/dd', defaultDate: null};


var VotingSystemClient = function () {

	this.setMessageToSignatureClient = function (message) {
		console.log("---- setMessageToSignatureClient: " + message);
		androidClient.setVotingWebAppMessage(message);
	}
}

var votingSystemClient = new VotingSystemClient()
</script>

<g:render template="/template/dialog/resultDialog"/>