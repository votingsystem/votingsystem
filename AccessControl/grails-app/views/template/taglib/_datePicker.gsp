<input type="text" id='${attrs.id}' required readonly
					    style='${attrs.style}'
						title='${attrs.title}'
						placeholder='${attrs.placeholder}'
	   					oninvalid='${attrs.oninvalid}'
	   					onchange='${attrs.onchange}'/>
<r:script>
$("#${attrs.id}").datepicker(pickerOpts);
</r:script>



