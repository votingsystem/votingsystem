<input type="text" id='${attrs.id}' required readonly
						title='${attrs.title}'
						placeholder='${attrs.placeholder}'
	   					oninvalid='${attrs.oninvalid}'
	   					onchange='${attrs.onchange}'/>
<r:script>
//"yy/MM/dd 12:00:00"
$("#${attrs.id}").datepicker({showOn: 'both', buttonImage: "${createLinkTo(dir: 'images', file: 'appointment.png')}",
		buttonImageOnly: true, dateFormat: 'yy/MM/dd'});
</r:script>




