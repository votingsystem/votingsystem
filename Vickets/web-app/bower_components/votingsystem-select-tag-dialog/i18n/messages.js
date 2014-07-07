var locales = {
    es: {
        "selectedTagsLbl":"Etiquetas seleccionadas",
        "tagLbl":"Etiqueta",
        "tagSearchLbl":"Buscar",
        "acceptLbl":"Aceptar",
        "closeLbl":"Cerrar"
    },
    en: {
        "selectedTagsLbl":"Selectes tags",
        "tagLbl":"Tag",
        "tagSearchLbl":"Search",
        "acceptLbl":"Accept",
        "closeLbl":"Close"
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



