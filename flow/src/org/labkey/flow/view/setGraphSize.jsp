<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<%@ page import="org.labkey.flow.FlowPreference" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% String graphSize = FlowPreference.graphSize.getValue(request);
    Map<String, String> sizes = new LinkedHashMap();
    sizes.put("300", "Large Graphs");
    sizes.put("200", "Medium Graphs");
    sizes.put("100", "Small Graphs");
%>

<style id="flow-graph">
    .flow-graph
    {
        height: <%=graphSize%>px;
        width: <%=graphSize%>px;
    }
    a.graphSizeCurrent
    {
        cursor:default;
        text-decoration:none;
        font-weight:bold;
    }
    a.graphSize
    {
        cursor:pointer;
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

    function setGraphClasses(name, className)
    {
        var nl = document.getElementsByName(name);
        for (var i = 0; i < nl.length; i ++)
        {
            nl.item(i).className = className;
        }
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

        setGraphClasses("graphSize" + currentSize, "graphSize");
        currentSize = size;
        setGraphClasses("graphSize" + currentSize, "graphSizeCurrent");
        document.getElementById("updateGraphSize").src = urlUpdateSize + currentSize;
    }
</script>
<% for (Map.Entry<String, String> entry : sizes.entrySet()) { %>
[<a class="<%=entry.getKey().equals(graphSize) ? "graphSizeCurrent" : "graphSize"%>" name="graphSize<%=entry.getKey()%>" onclick="setGraphSize(<%=entry.getKey()%>)"><%=h(entry.getValue())%></a>]
<% } %>
<img id="updateGraphSize" height="1" width="1" src="<%=request.getContextPath()%>/_.gif">
