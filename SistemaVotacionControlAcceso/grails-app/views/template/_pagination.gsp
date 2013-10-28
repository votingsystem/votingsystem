<div>
	<div style="margin:20px auto 20px auto;" id="paginationDiv" ></div>
</div>
<script>
var offsetPage

function printPaginate (offset, numItems, numMaxItemsForPage) {
	var numPages = ( (numItems - numItems%numMaxItemsForPage)/numMaxItemsForPage) + 
		((numItems%numMaxItemsForPage == 0)?0:1)
	offsetPage = ( (offset -offset%numMaxItemsForPage)/numMaxItemsForPage) + 1
	console.log("/template/_pagination - offsetItem:" + offset + " - num. total items: " + numItems + 
			" - offsetPage: " + offsetPage + " - numPages:" + numPages)
	$("#paginationDiv").paginate({
		count 		: numPages,
		start 		: offsetPage,
		display     : 9,
		border					: true,
		border_color			: '#769ab5',
		text_color  			: '#769ab5',
		background_color    	: '#f1f5f8',	
		background_hover_color  : '#dae7f1',
		border_hover_color		: '#0066cc',
		text_hover_color  		: '#0066cc',
		images					: false,
		mouse					: 'press', 
		onChange				: paginate
	});
}
</script>