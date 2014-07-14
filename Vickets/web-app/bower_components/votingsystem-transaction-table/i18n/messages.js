var locales = {
    es: {
        "typeLbl":"Tipo",
        "amountLbl":"Cantidad",
        "datecreatedLbl":"Fecha",
        "subjectLbl":"Asunto"
    },
    en: {
        "typeLbl":"Type",
        "amountLbl":"Amount",
        "datecreatedLbl":"Date",
        "subjectLbl":"Subject"
    }
}


//http://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
function _getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

var localizedMessages
var userLocale = _getParameterByName('locale') || navigator.language
if(locales[userLocale]) {
    localizedMessages = locales[userLocale]
} else localizedMessages = locales['es']




