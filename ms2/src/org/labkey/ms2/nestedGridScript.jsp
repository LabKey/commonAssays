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
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%
    String dataRegionName = ((JspView<String>)HttpView.currentView()).getModelBean();
%>
<script type="text/javascript"><!--

var requestedURLs = new Object();

function callback(elementName, req, url)
{
    if (req.readyState == 4)
    {
        if (req.status == 200)
        {
            var rowElement = document.getElementById("<%=dataRegionName%>-Row" + elementName);
            var toggleElement = document.getElementById("<%=dataRegionName%>-Handle" + elementName);
            var contentElement = document.getElementById("<%=dataRegionName%>-Content" + elementName);

            contentElement.innerHTML = req.responseText;
            toggleElement.src = "<%= request.getContextPath() %>/_images/minus.gif";
            rowElement.style.display = "";
        }
        else
        {
            requestedURLs[url] = null;
        }
    }
}

function toggleNestedGrid(url, elementName)
{
    var contentElement = document.getElementById("<%=dataRegionName%>-Content" + elementName);
    var rowElement = document.getElementById("<%=dataRegionName%>-Row" + elementName);
    var toggleElement = document.getElementById("<%=dataRegionName%>-Handle" + elementName);

    if (contentElement.innerHTML == "")
    {
        if (requestedURLs[url] == null)
        {
            requestedURLs[url] = elementName;
            if (window.XMLHttpRequest)
            {
                req = new XMLHttpRequest();
            }
            else if (window.ActiveXObject)
            {
                req = new ActiveXObject("Microsoft.XMLHTTP");
            }
            req.open("GET", url, true);
            var callbackObject = function()
            {
                callback(callbackElementName, callbackRequest, callbackURL);
            };
            var callbackElementName = elementName;
            var callbackRequest = req;
            var callbackURL = url;
            req.onreadystatechange = callbackObject;
            req.send(null);
        }
        return;
    }

    if (rowElement.style.display == "none")
    {
        rowElement.style.display = "";
        toggleElement.src = "<%= request.getContextPath() %>/_images/minus.gif";
    }
    else
    {
        rowElement.style.display = "none";
        toggleElement.src = "<%= request.getContextPath() %>/_images/plus.gif";
    }
}

--></script>
