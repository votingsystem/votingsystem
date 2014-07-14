var locales = {
    es: {
    "caption":"Búsqueda avanzada",
    "fromDate":"Desde",
    "toDate":"Hasta",
    "searchInputMessage":"Introduzca la cadena de búsqueda",
    "accept":"Aceptar",
    "dateLbl":"Fecha",
    "hourLbl": "Hora"
    },
    en: {
    "caption":"Avanced search",
    "fromDate":"From date",
    "toDate":"To date",
    "searchInputMessage":"Enter search text",
    "accept":"Accept",
    "dateLbl":"Date",
    "hourLbl": "Hour"
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



