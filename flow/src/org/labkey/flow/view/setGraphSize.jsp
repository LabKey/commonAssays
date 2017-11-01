<%
/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
    }
%>
<%
    String graphSize = FlowPreference.graphSize.getValue(request);
    Map<String, String> sizes = new LinkedHashMap();
    sizes.put("300", "Large Graphs");
    sizes.put("200", "Medium Graphs");
    sizes.put("150", "Small Graphs");
%>
<style type="text/css">
    .labkey-graph-size {
        display: inline-block;
        cursor: pointer;
    }
</style>

<script type="text/javascript">

    (function($) {

        var urlUpdateSize = <%=q(FlowPreference.graphSize.urlUpdate())%>;
        var currentSize = <%=q(graphSize)%>;
        var fullSize = 300;
        var curImage;
        var zoomImage;

        function zoomOut() {
            zoomImage.style.visibility = "hidden";
        }

        document.body.onclick = function(event) {
            if (!event)
            {
                event = window.event;
            }
            var el = event.srcElement;
            if (!el)
                el = event.target;
            if (!el || el.className != 'labkey-flow-graph')
                return;
            if (currentSize == fullSize)
                return;
            if (!zoomImage)
            {
                zoomImage = document.createElement("img");
                document.body.appendChild(zoomImage);
                zoomImage.style.position = "absolute";
                zoomImage.style.border = "2px solid gray";
                zoomImage.onclick = zoomOut;
            }
            var scrollTop = 0;
            var scrollLeft = 0;
            if (window.pageXOffset != undefined)
            {
                scrollLeft = window.pageXOffset;
                scrollTop = window.pageYOffset;
            }
            else
            {
                scrollLeft = document.documentElement.scrollLeft;
                scrollTop = document.documentElement.scrollTop;
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

            // Don't size the zoomed image -- let it be the image's natural width/height.
            zoomImage.style.left = (event.clientX - offsetX + scrollLeft) + "px";
            zoomImage.style.top = (event.clientY - offsetY + scrollTop) + "px";
            zoomImage.src = el.src;
            zoomImage.style.visibility="visible";
        };

        function setGraphClasses(name, className) {
            var nl = document.getElementsByName(name);
            for (var i = 0; i < nl.length; i ++) {
                nl.item(i).className = className;
            }
        }

        function setGraphSize(size) {
            var $ = jQuery;

            // resize both images and "No graph for..." span
            $('.labkey-flow-graph').each(function(i, el) {
                var graph = $(el);
                graph.width(size).height(size);
                graph.parent('span').width(size).height(size);
            });

            // update link style
            setGraphClasses("graphSize" + currentSize, "");
            currentSize = size;
            setGraphClasses("graphSize" + currentSize, "labkey-selected-link");

            // update user preference
            $('#updateGraphSize').attr('src', urlUpdateSize + currentSize + "&_dc=" + new Date().getTime());
        }

        // make global -- eesh
        window.setGraphSize = setGraphSize;

    })(jQuery);
</script>

<%
for (Map.Entry<String, String> entry : sizes.entrySet()) { %>
    <div class="labkey-graph-size">
        [<a class="<%=entry.getKey().equals(graphSize) ? "labkey-selected-link" : ""%>" name="graphSize<%=entry.getKey()%>" onclick="setGraphSize(<%=entry.getKey()%>)"><%=h(entry.getValue())%></a>]
    </div>
<% } %>
<img id="updateGraphSize" height="1" width="1" src="<%=getWebappURL("_.gif")%>">
