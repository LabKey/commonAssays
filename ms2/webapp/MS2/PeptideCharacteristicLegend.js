if (!LABKEY.ms2) {
    LABKEY.ms2 = {};
}

if (!LABKEY.ms2.PeptideCharacteristicLegend) {
    LABKEY.ms2.PeptideCharacteristicLegend = {

        addHeatMap: function (peptideCharacteristics, colors) {
            const rectHeight = document.getElementById('peptideMap').clientHeight/(peptideCharacteristics.length+1); // extra 1 for room
            const svgHeight = rectHeight * peptideCharacteristics.length;

            const svgContainer = d3.select(".heatmap").append("svg")
                    .attr("height", svgHeight)
                    .attr("width", 200);

            const rect = svgContainer.selectAll(".rect")
                    .data(peptideCharacteristics)
                    .enter()
                    .append("rect");
            rect.attr("y", (d,i) => rectHeight*i)
                    .attr("x", function (d, i) { return 0; })
                    .attr("height", rectHeight)
                    .attr("width", 20)
                    .style("fill", (d) => colors[d]);
            svgContainer.selectAll('.text')
                    .data(peptideCharacteristics)
                    .enter().append('text')
                    .text((d) => {
                        var str = "";
                        if (d.toString() > 0.0) {
                           str = d.toPrecision(3).toString();
                        }

                        return str;
                    })
                    .attr("y", (d,i) => (rectHeight*i) + rectHeight/2) // place text at the center of rect
                    .attr("x", 35);

        },

        changeView: function (settingName, elementId) {
            const viewBy = $("#" + elementId).val();
            const currentUrl = new URL(window.location.href);
            currentUrl.searchParams.set(settingName, viewBy)
            window.location = currentUrl.toString();
        }
    }

}