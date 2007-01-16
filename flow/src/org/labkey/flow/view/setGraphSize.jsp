<%@ page import="org.labkey.flow.FlowPreference" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<% String graphSize = FlowPreference.graphSize.getValue(request);
    Map<String, String> sizes = new LinkedHashMap();
    sizes.put("300", "Large");
    sizes.put("200", "Medium");
    sizes.put("100", "Small");
%>

<style id="flow-graph">
    .flow-graph
    {
        height: <%=graphSize%>px;
        width: <%=graphSize%>px;
    }
</style>
<script>
    var urlUpdateSize = <%=q(FlowPreference.graphSize.urlUpdate())%>
    var currentSize = <%=graphSize%>;
    var fullSize = 300;
    var curImage;
    var zoomImage;

    function zoomOut()
    {
        zoomImage.style.visibility = "hidden";
    }
    document.body.onclick = function(event)
    {
        if (!event)
        {
            event = window.event;
        }
        var el = event.srcElement;
        if (!el)
            el = event.target;
        if (!el || el.className != 'flow-graph')
            return;
        if (currentSize == fullSize)
            return;
        if (!zoomImage)
        {
            zoomImage = document.createElement("img");
            document.body.appendChild(zoomImage);
            zoomImage.style.position = "absolute";
            zoomImage.style.height = fullSize;
            zoomImage.style.width = fullSize;
            zoomImage.onclick = zoomOut;
        }
        var scrollTop = 0;
        var scrollLeft = 0;
        if (window.pagexOffset != undefined)
        {
            scrollLeft = window.pagexOffset;
            scrollTop = window.pageyOffset;
        }
        else
        {
            scrollLeft = document.body.scrollLeft;
            scrollTop = document.body.scrollTop;
        }
        var offsetX, offsetY;
        if (event.offsetX != undefined)
        {
            offsetX = event.offsetX;
            offsetY = event.offsetY;
        }
        else
        {
            offsetX = 50;
            offsetY = 50;
        }

        zoomImage.style.left = (event.clientX - offsetX + scrollLeft);
        zoomImage.style.top = (event.clientY - offsetY + scrollTop);
        zoomImage.src = el.src;
        zoomImage.style.visibility="visible";
    }
    function setGraphSize(size)
    {
        var images = document.images;
        for (var i = 0; i < images.length; i ++)
        {
            var img = images[i];
            if (img.className == "flow-graph")
            {
                img.style.width = size;
                img.style.height = size;
            }
        }
        currentSize = size;
        document.getElementById("updateGraphSize").src = urlUpdateSize + currentSize;
    }
</script>
Size:<select onchange="setGraphSize(this.options[this.selectedIndex].value)">
<cpas:options value="<%=graphSize%>" map="<%=sizes%>" /> 
</select><img id="updateGraphSize" height="1" width="1" src="<%=request.getContextPath()%>/_.gif">