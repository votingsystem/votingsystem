<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/d3.html" rel="import"/>
<link href="../resources/bower_components/vs-checkbox-selector/vs-checkbox-selector.html" rel="import"/>


<dom-module id="transactions-scatter">
    <link rel="import" type="css" href="transactions-scatter.css">
    <style>
        :host {
            display: block;
            width:100%;
            height: 100%;
            margin: 0 7px 0 7px;
            position: relative;
            border: 1px solid #ccc;border-radius: 3px;
        }
        .tooltip {
            text-align: center;
            font-size: 0.8em;
            color: #888;
            padding: 3px;
            background: #fafafa;
            border: 1px solid #888;
            border-radius: 4px;
        }
    </style>
    <template>
        <div id="messageDiv" class="horizontal layout center center-justified flex" style="margin:20px;text-align: center;">${msg.withoutDataLbl}</div>
        <div id="chartContainerDiv" style="display: none;">
            <div class="horizontal layout" style="min-height: 60px;">
                <div id="resetButtonDiv" style="margin: 0 20px 0 0;display: none;">
                    <button id="resetButton">${msg.resetZoom}</button>
                </div>
                <div class="flex horizontal layout center center-justified">
                    <div id="tooltip" class="tooltip" style="display: none;">
                        <div style$="{{selectedTransactionTypeStyle}}">{{getTransactionDescription(selectedTransaction.type)}}</div>
                        <div style="font-size: 0.9em;margin: 0 0 2px 0;"><a href="{{selectedTransaction.messageSMIMEURL}}" target="_blank">
                            {{getDate(selectedTransaction.dateCreated)}}</a></div>
                        <div style$="{{selectedTransactionTagStyle}}">
                            {{selectedTransaction.amount}} {{selectedTransaction.currencyCode}} - {{selectedTransactionTag}}
                            <i hidden={{!selectedTransaction.timeLimited}} class="fa fa-clock-o" style="margin:0px 0px 0px 5px; color: red;" title="${msg.timeLimitedLbl}"></i>
                        </div>
                    </div>
                </div>
            </div>
            <div id="scatterPlotDiv" style="width: 100%; height: 100%;"></div>
        </div>
    </template>
    <script>
        (function() {
            Polymer({
                is: "transactions-scatter",
                properties: {
                    circlesGradientList:{type:Array, value:[]},
                    selectedTransaction:{type:Object, observer:'selectedTransactionChanged'},
                    margin:{type:Object, value:{top: 5, right: 10, bottom: 20, left: 50}},
                    chartData:{type:Object}
                },
                ready: function() {
                    console.log(this.tagName + " ready")
                    this.rMin = 5
                    this.rMax = 20
                    this.rScale = d3.scale.linear().range([this.rMin, this.rMax])
                    window.addEventListener('resize', function(event){
                        this.circlesGradientList = []
                        this.chart(this.chartData, this.color)
                    }.bind(this));
                },
                filterChart:function (transStats) {
                    d3.select(this).selectAll(".transaction").attr("display", function(d) {
                        var timeType = d.timeLimited ? 'timeLimited':'timeFree'
                        if(transStats.transactionTimeFilter.indexOf(timeType) > -1) return 'none'
                        else if(transStats.transactionTypeFilter.indexOf(d.type) > -1) return 'none'
                        else if(transStats.transactionTagFilter.indexOf(d.tags[0]) > -1) return 'none'
                        else if(transStats.transactionCurrencyFilter.indexOf(d.currencyCode) > -1) return 'none'
                        else return 'inline'
                    }.bind(this));
                },
                selectedTransactionChanged:function (transaction) {
                    this.selectedTransactionTag = transaction.tags[0]
                    this.selectedTransactionTypeStyle = "font-weight:bold;color:" + this.color(transaction.type) + ";";
                    this.selectedTransactionTagStyle = "font-size: 0.9em;color:" + this.color(transaction.tags[0]) + ";"
                },
                getTransactionDescription:function (transaction) {
                    return transactionsMap[transaction].lbl
                },
                getCircleGradientId:function (transaction) {
                    var gradId= transaction.type + "_" + transaction.tags[0]
                    gradId = gradId.replace(' ', '');
                    if(this.circlesGradientList.indexOf(gradId) < 0) {
                        var grad = d3.select(this).select("svg").append("defs").append("linearGradient").attr("id", gradId)
                                .attr("x1", "0%").attr("x2", "0%").attr("y1", "100%").attr("y2", "0%");
                        grad.append("stop").attr("offset", "50%").style("stop-color", this.color(transaction.tags[0]));
                        grad.append("stop").attr("offset", "50%").style("stop-color", this.color(transaction.type));
                        this.circlesGradientList.push(gradId)
                    }
                    return gradId
                },
                getDate:function (date) {
                    return new Date(date).formatWithTime()
                },
                chart:function (data, color) {
                    console.log(this.tagName + " - chart")
                    var hostElement = this
                    this.color = color
                    this.chartData = data
                    while (this.$.scatterPlotDiv.hasChildNodes()) {
                        this.$.scatterPlotDiv.removeChild(this.$.scatterPlotDiv.lastChild);
                    }
                    if(!data || data.length === 0) {
                        console.log(this.tagName + " - without root data")
                        this.$.messageDiv.style.display = 'block'
                        this.$.chartContainerDiv.style.display = 'none'
                        return
                    } else this.$.messageDiv.style.display = 'none'
                    this.$.chartContainerDiv.style.display = 'block'

                    var hostOffsets = this.getBoundingClientRect()
                    console.log("=========== hostOffsets: " + hostOffsets)
                    this.circlesGradientList = []
                    this.width = this.getBoundingClientRect().width
                    this.height = this.getBoundingClientRect().height
                    var svg = d3.select("#scatterPlotDiv").append("svg")
                            .attr('width', this.width)
                            .attr('height', this.height).datum(data).append("g")
                            .attr("transform", "translate(" + this.margin.left + " ," + this.margin.top + ")")
                            .on("click", function() {
                                this.$.tooltip.style.display = 'none'
                            }.bind(this));
                    this.height = this.height - this.margin.top - this.margin.bottom;
                    var x = d3.time.scale(),
                            y = d3.scale.linear(),
                            xAxis = d3.svg.axis().scale(x).orient('bottom').ticks(5),
                            yAxis = d3.svg.axis().scale(y).orient('left').ticks(5)
                    this.width = this.width - this.margin.left - this.margin.right
                    this.rScale.domain(d3.extent(data, function (d){ return d.amount }));
                    x = x.domain(d3.extent(data, function(d) { return d.dateCreated; })).range([0, this.width]).nice();
                    y = y.domain([0, d3.max(data, function(d) {
                        return d.amount; })]).range([this.height, 0]).nice();
                    var zoom = d3.behavior.zoom().x(x).y(y).scaleExtent([-100, 100]).on("zoom", zoomed)
                    var scatterWrap = svg.append("rect")
                            .attr("width", this.width)
                            .attr("height", this.height)
                            .attr("class", "scatterRect").call(zoom);

                    var clip = svg.append("clipPath")
                            .attr("id", "clip")
                            .append("rect")
                            .attr("x", 0)
                            .attr("y", 0)
                            .attr("width", this.width)
                            .attr("height", this.height)
                    var chartBody = svg.append("g").attr("clip-path", "url(#clip)");

                    resetButton.onclick= function(){
                        zoom.translate([0, 0]).scale(1)
                        zoomed()
                        hostElement.$.resetButtonDiv.style.display = 'none'
                    }
                    var circles = chartBody.selectAll(".transaction").data(data);
                    circles.enter().append("circle").attr("class", "transaction");
                    var circles = circles
                            .attr("cx",      function (d){ return   x(new Date(d.dateCreated))})
                            .attr("cy",      function (d){ return   y(d.amount) })
                            .attr("r",       function (d){ return hostElement.rScale(d.amount) })
                            .attr("style",    function (d){
                                var gradientId = hostElement.getCircleGradientId(d)
                                var stroke = "stroke:#fefefe;"
                                if(d.timeLimited) stroke = "stroke:red;stroke-width:2px;"
                                return   "fill:url(#" + gradientId + ")" + ";" + stroke}.bind(this))
                            .on("mouseover", function(d) {
                                hostElement.$.tooltip.style.display = 'block'
                                //var offsets = hostElement.getBoundingClientRect();
                                //hostElement.$.tooltip.style.top = (d3.event.pageY - offsets.top + hostElement.rScale(d.amount)) + "px"
                                //hostElement.$.tooltip.style.left = (d3.event.pageX - 30) + "px"
                                hostElement.selectedTransaction = d
                                var circle = d3.select(this);
                                circle.transition()
                                        .duration(800).style("opacity", 1)
                                        .attr("r", hostElement.rScale(d.amount) + 3).ease("elastic");
                                svg.append("g")
                                        .attr("class", "guide")
                                        .append("line")
                                        .attr("x1", circle.attr("cx"))
                                        .attr("x2", circle.attr("cx"))
                                        .attr("y1", circle.attr("cy"))
                                        .attr("y2", hostElement.height)
                                        .style("stroke", hostElement.color(d.tags[0]))
                                svg.append("g")
                                        .attr("class", "guide")
                                        .append("line")
                                        .attr("x1", circle.attr("cx"))
                                        .attr("x2", 0)
                                        .attr("y1", circle.attr("cy"))
                                        .attr("y2", circle.attr("cy"))
                                        .style("stroke", hostElement.color(d.tags[0]))
                            })
                            .on("mouseout", function(d) {
                                var circle = d3.select(this);
                                circle.transition().duration(800).style("opacity", .5).attr("r", hostElement.rScale(d.amount)).ease("elastic");
                                d3.selectAll(".guide").transition().duration(100).styleTween("opacity", function() { return d3.interpolate(.5, 0); }).remove()
                            })

                    svg.append("g")
                            .attr("class", "x axis")
                            .attr("transform", "translate(0," + (this.height) + ")")
                            .call(xAxis)
                            .append("text")
                            .attr("class", "label")
                            .attr("x", this.width)
                            .attr("y", -6)
                            .style("text-anchor", "end");
                    svg.append("g")
                            .attr("class", "y axis")
                            .call(yAxis)
                            .append("text")
                            .attr("class", "label")
                            .attr("transform", "rotate(-90)")
                            .attr("y", 6)
                            .attr("x", 0)
                            .attr("dy", ".71em")
                            .style("text-anchor", "end")
                            .style("size", "end")
                            .text("${msg.amountLbl}");
                    function zoomed() {
                        //console.log("zoom.scale: " + zoom.scale() + " - x.domain: " + x.domain()[0])
                        hostElement.$.resetButtonDiv.style.display = 'block'
                        svg.select(".x.axis").call(xAxis);
                        svg.select(".y.axis").call(yAxis);
                        circles.attr("cx", function (d){ return x(new Date(d.dateCreated))})
                                .attr("cy", function (d){ return y(d.amount) })
                    }
                    //hack to refresh styles, I haven't found other way
                    Polymer.dom(this.$.scatterPlotDiv).appendChild(Polymer.dom(this.$.scatterPlotDiv).childNodes[0])
                }
            });
        })();
    </script>
</dom-module>