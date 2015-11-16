<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/iron-ajax/iron-ajax.html" rel="import"/>

<dom-module name="vs-dashboard">

    <template>
        <style>
            .transaction {
                stroke: blue;
                stroke-width: 2;
            }
        </style>
        <iron-ajax auto="" id="ajax" url="{{url}}" handle-as="json" last-response="{{dashBoardDto}}" method="get"
                   content-type="application/json"></iron-ajax>
        <div id="transactionChart"></div>
    </template>
    <script>
        Polymer({
            is:'vs-dashboard',
            properties: {
                dashBoardDto: {type:Object, observer:'dashBoardDtoChanged'},
                url:{type:String, value: "/CurrencyServer/rest/transactionVS"}
            },
            ready: function() {
                console.log(this.tagName + " - ready - width: " + this.style.width);
                this.outerWidth = this.style.width || 500;
                this.outerHeight = this.style.height || 350;

                this.innerWidth  = this.outerWidth  - 30 - 30;
                this.innerHeight = this.outerHeight - 30 - 30;

                this.rMin = 5; // "r" stands for radius
                this.rMax = 20;

                this.svg = d3.select("#transactionChart").append("svg")
                        .attr("width", this.outerWidth)
                        .attr("height", this.outerHeight);

                this.xScale = d3.scale.linear().range([0, this.innerWidth]);
                this.yScale = d3.scale.linear().range([this.innerHeight, 0]);
                this.rScale = d3.scale.linear().range([this.rMin, this.rMax]);
                this.colorScale = d3.scale.category10();

            },
            render:function(data) {

                this.xScale.domain(d3.extent(data, function (d){
                    return new Date(d.dateCreated).getMinutes() }));
                this.yScale.domain(d3.extent(data, function (d){ return d.amount }));
                this.rScale.domain(d3.extent(data, function (d){ return d.amount }));

                var circles = this.svg.selectAll("circle").data(data);
                circles.enter().append("circle");
                circles.attr("class", "transaction")
                        .attr("cx",      function (d){ return   this.xScale(new Date(d.dateCreated).getMinutes())}.bind(this))
                        .attr("cy",      function (d){ return   this.yScale(d.amount) }.bind(this))
                        .attr("r",       function (d){ return   this.rScale(d.amount) }.bind(this))
                        .attr("fill",    function (d){ return   this.colorScale(d.type) }.bind(this))


                circles.exit().remove();
            },
            dashBoardDtoChanged:function() {
                this.render(this.dashBoardDto.resultList)
            }
        });
    </script>
</dom-module>