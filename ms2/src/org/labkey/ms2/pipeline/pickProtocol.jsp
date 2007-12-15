<%@ page import="org.labkey.api.pipeline.PipelineUrls"%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.ms2.pipeline.FileStatus" %>
<%@ page import="org.labkey.ms2.pipeline.MS2ExperimentForm" %>
<%@ page import="org.labkey.ms2.pipeline.MS2PipelineForm" %>
<%@ page import="org.labkey.ms2.pipeline.PipelineController" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<p/>

<form method=post action="<%=urlFor(PipelineController.ShowDescribeMS2RunAction.class)%>">
<%
    MS2ExperimentForm form = (MS2ExperimentForm) getForm();
    PipelineUrls up = urlProvider(PipelineUrls.class);    

    int annotFileCount = 0;
    for (FileStatus status : form.getMzXmlFileStatus().values())
        if (status == FileStatus.UNKNOWN)
            annotFileCount++;
%>
<input type="hidden" name="stepString" value="pickProtocol">
<input type="hidden" name="size" value="<%=annotFileCount%>">
<input type="hidden" name="<%=MS2PipelineForm.PARAMS.path%>" value="<%=h(form.getPath())%>">
<input type="hidden" name="<%=MS2PipelineForm.PARAMS.searchEngine%>" value="<%=h(form.getSearchEngine())%>">
<br>

<p>You may optionally choose the experimental protocol that describes how the LC-MS/MS sample was processed to create the
mzXML file. This step will allow you to identify the sample used as input to the run, which you can then use for later
analysis inside of CPAS.</p>
<p>You may choose to skip this step. If so, you can enter this information later by initiating another search through
the pipeline.</p>

[<a href="<%=PipelineController.urlShowCreateMS2Protocol(getContainer(), form)%>">create a new protocol</a>]<%= PageFlowUtil.helpPopup("Create a new protocol", "Creating a new protocol lets you describe the steps taken to process a sample, or any special configuration for the mass spectrometer.") %>
&nbsp;&nbsp;&nbsp;[<a href="<%=PipelineController.urlSearch(getContainer(), form, true)%>">skip this step and continue</a>]
<br>
<br>
<%
    if(form.hasErrors())
    {
        out.write("<span class=\"labkey-error\">");
        out.write(form.getError(0));
        out.write("</span><br>");
    }
%>
<%
    String[] protocolAvailableNames = form.getProtocolAvailableNames();
    if (protocolAvailableNames != null && protocolAvailableNames.length > 0)
    {
%>
    <table border=0>
<%
    if (annotFileCount > 1)
    {
%>
        <tr>
            <td colspan=3>
                <input type="radio" <%=form.isProtocolShare() ? "checked" : "" %> id="protocolSharing.shared" name="protocolSharingString" value="share"><span class="heading-1"> The same protocol was used to create all files</span><br>
            </td>
        </tr>
        <tr><td>&nbsp;&nbsp;&nbsp;</td><td class="normal">Shared Protocol</td>
            <td>
                <select name="sharedProtocol" onclick="checkItem('protocolSharing.shared')">
                  <option></option>
                <%
                  for (String protocolName : protocolAvailableNames)
                  {
                      out.print("<option>");
                      out.print(h(protocolName));
                      out.print("</option>");
                  }
                %>
            </select>
        </td>
      </tr>
    <tr>
        <td colspan=2>
    <input type="radio" <%=form.isProtocolFractions() ? "checked" : "" %> id="protocolSharing.fractions" name="protocolSharingString" value="fractions"><span class="heading-1">  All files are fractions of the same sample</span><br>
        </td>
    </tr>
    <tr><td>&nbsp;&nbsp;&nbsp;</td><td class="normal">Fractionation Protocol</td>
        <td>
    <select name="fractionProtocol" onclick="checkItem('protocolSharing.fractions')">
      <option></option>
        <%
      for (String protocolName : protocolAvailableNames)
      {
          out.print("<option>");
          out.print(h(protocolName));
          out.print("</option>");
      }
        %>
  </select>
        </td>
      </tr>
<tr>
    <td colspan=3>
    <input type="radio" <%=form.isProtocolIndividual() ? "checked" : "" %> name="protocolSharingString" id="protocolSharing.none" value="none"> <span class="heading-1"> Each file was created with a different protocol</span><br>
    </td>
</tr>
    <%
    }
    else
    {
        %><input type="hidden" name="protocolSharingString" value="none"><%
    }
    %>
    <tr><td></td><td class="header">File</td><td class="header">Protocol</td></tr>
<%
int index = 0;
for (Map.Entry<File, FileStatus> entry : form.getMzXmlFileStatus().entrySet())
{
    if (entry.getValue() != FileStatus.UNKNOWN)
        continue;   // Already annotated.
        String protocolNameChecked = "";
        String error = "";
        if (index < form.getRunNames().length)
            {
            protocolNameChecked = form.getProtocolNames()[index];
            error = form.getError(index);
            }

%>
  <tr><td></td><td class="normal"><%=h(entry.getKey().getName())%></td>
        <td class="normal">
          <select name="protocolNames[<%=index%>]" onclick="checkItem('protocolSharing.none')" >
            <option></option>
<%
    for (String protocolName : protocolAvailableNames)
    {
        if (protocolName.equals(protocolNameChecked))
            out.print("<option selected>");
        else
            out.print("<option>");
        out.print(h(protocolName));
        out.print("</option>");
    }
%>
        </select>
      </td></tr>
<%
    index++;
} %>
</table>

<table>
    <tr>
        <td>
            <labkey:button text="Submit"/>
        </td>
        <td>
            <labkey:button text="Cancel" href="<%=up.urlReferer(getContainer())%>"/>
        </td>
    </tr>
</table>

</form>
<script type="">
    function checkItem(id)
    {
        try
        {
            var elem = document.getElementById(id);
            if (elem)
            {
                elem.checked = true;
            }
        }
        catch(e)
        {

        }
    }
</script>
<%
    }
    else
    {
%>
This step allows you to record additional information about the sample that
was used to generate the mzXML file and the specific steps ("protocol")
used to prepare the sample. 
<%
    }
%>
