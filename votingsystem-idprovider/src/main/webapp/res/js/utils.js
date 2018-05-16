var app = {}

function toJSON(message){
    if(message) {
        if(typeof message === 'string' ) return JSON.parse(message);
        else  return message
    }
}

app.httpPost = function (url, params, callbackOK, callbackError) {
    var xhr = new XMLHttpRequest();
    console.log("doPostRequest - url: ", url , " - params: ", params);
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xhr.onreadystatechange = function() {
        if(xhr.readyState == 4 && xhr.status == 200) {
            if(callbackOK) callbackOK(xhr.responseText)
        } else if(xhr.readyState == 4) {
            if(callbackError) callbackError(xhr.responseText)
        }
    }
    xhr.send(params);
}

app.httpGet = function(url, callback) {
    var xhr = new XMLHttpRequest()
    xhr.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            callback(this.responseText)
        }
    };
    xhr.open("GET", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send();
}


String.prototype.getDate = function() {
    var timeMillis = Date.parse(this)
    return new Date(timeMillis)
};

Number.prototype.getDate = function() {
    return new Date(this)
}

Date.prototype.urlFormatWithTime = function() {//YYYYMMDD_HHmm
    var curr_date = pad(this.getDate(), 2);
    var curr_month = pad(this.getMonth() + 1, 2); //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + curr_month + curr_date + "_" + ('0' + this.getHours()).slice(-2) + ('0' + this.getMinutes()).slice(-2)
};

Date.prototype.toISOStr = function() {
    return this.toISOString().slice(0, 10) + " " +
        ('0' + this.getHours()).slice(-2) + ":" + ('0' + this.getMinutes()).slice(-2) + ":" + ('0' + this.getSeconds()).slice(-2)
}

String.prototype.format = function() {
    var args = arguments;
    var str =  this.replace(/''/g, "'")
    return str.replace(/{(\d+)}/g, function(match, number) {
        return typeof args[number] != 'undefined' ? args[number] : match;
    });
};
