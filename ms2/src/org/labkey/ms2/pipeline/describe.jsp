<%@ page extends="org.labkey.ms2.pipeline.DescribeRunPage" %>
<%@ page import="org.fhcrc.cpas.exp.xml.ExperimentArchiveType"%>
<%@ page import="org.fhcrc.cpas.exp.xml.ProtocolBaseType"%>
<%@ page import="org.labkey.api.exp.api.ExpMaterial"%>
<%@ page import="org.labkey.api.pipeline.PipelineUrls"%>
<%@ page import="org.labkey.api.view.ThemeFont"%>
<%@ page import="org.labkey.ms2.pipeline.FileStatus"%>
<%@ page import="org.labkey.ms2.pipeline.MS2ExperimentForm"%>
<%@ page import="org.labkey.ms2.pipeline.MS2PipelineForm" %>
<%@ page import="org.labkey.ms2.pipeline.PipelineController" %>
<%@ page import="java.io.File"%>
<%@ page import="java.util.Map" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<p/>
<form method=post action="<%=urlFor(PipelineController.ShowDescribeMS2RunAction.class)%>" name="describeForm">
<%
    MS2ExperimentForm form = getForm();
    PipelineUrls up = urlProvider(PipelineUrls.class);

    int annotFileSize = 0;
    if (form.isProtocolFractions())
        annotFileSize = getMzXmlFileStatus().size();
    else
    {
        for (Map.Entry<File, FileStatus> entry : getMzXmlFileStatus().entrySet())
            if (entry.getValue() == FileStatus.UNKNOWN)
                annotFileSize++;
    }
%>
<input type="hidden" name="size" value="<%=annotFileSize%>">
<input type="hidden" name="stepString" value="describeSamples">
<input type="hidden" name="protocolSharingString" value="<%=form.getProtocolSharingString()%>">
<input type="hidden" name="sharedProtocol" value="<%=form.getSharedProtocol()%>">
<input type="hidden" name="fractionProtocol" value="<%=form.getFractionProtocol()%>">
<input type="hidden" name="<%=MS2PipelineForm.PARAMS.path%>" value="<%=h(form.getPath())%>">
<input type="hidden" name="<%=MS2PipelineForm.PARAMS.searchEngine%>" value="<%=h(form.getSearchEngine())%>">
<table border="0">
    <tr><td colspan=2 style="font-size:<%=ThemeFont.getThemeFont().getHeader_1Size()%>">
    Before searching describe how each mzXML file was created using the following information<br>
    </td>
    </tr>
    <tr><td style="padding-left:10px" >
        <b>Run Name</b> </td><td class="normal">Your name for this MS2 run</td>
    </tr>
    <tr>
        <td style="padding-left:10px"><b>Sample Set</b></td>
        <td class="normal">Set that contains the sample used in this analysis. <a href="<%=getViewContext().getActionURL().relativeUrl("showUploadMaterials.view", "", "Experiment")%>">Upload</a> sample information using the experiment module, and <a href="<%=getViewContext().getActionURL().relativeUrl("listMaterialSources.view", "", "Experiment")%>">set the active sample set</a>. Optional.</td>
    </tr>
    <tr>
        <td style="padding-left:10px"><b>Sample Id</b></td>
        <td class="normal">Unique Id of the sample within sample set.</td>
    </tr>
    <tr>
        <td style="padding-left:10px"><b>Protocol</b> </td>
    <td class="normal">Protocol used to prepare sample and run Mass Spec. <a href="<%=PipelineController.urlShowCreateMS2Protocol(getViewContext().getContainer(), form)%>">Create</a> a new protocol.</td>
    </tr>
    </table>
    <br>
    <labkey:button text="Submit"/>&nbsp;<labkey:button text="Cancel" href="<%=up.urlReferer(getViewContext().getContainer())%>"/>
    <br/>&nbsp;<br/>
<table border="0">
<%

    if (form.isProtocolFractions())
    {
    //todo check to see if already annotated?
/*
        for (File file : getMzXmlFileStatus().keySet())
            {
            String status = getMzXmlFileStatus().get(file);
            if (status != "")
                break;   // Already annotated.
            }
*/
        String protocolName = form.getFractionProtocol();
        String runName = "MS2 Sample Prep (" + getPathDescription() + "), (" + protocolName + ")";
        String error = form.getError(0);
%>
<tr><td colspan="2" class="heading-1">Run Settings (with Fractionation)</td></tr>
<tr><td>&nbsp;</td><td>
  <table border="0">
<%      if (error != null && error.length() > 0)
        { %>
  <tr><td class="labkey-error" colspan="2"><%=error%></td></tr>
<%      } %>
  <tr><td class="ms-searchform">Run Name</td>
    <td class="normal">
<%
        int index = 0;
        for (File file : getMzXmlFileStatus().keySet())
        { %>
        <input type="hidden" name="fileNames[<%=index%>]" value="<%=h(file.getName())%>">
<%
        index++;
        }
%>
        <input type="hidden" name="protocolNames[0]" value="<%=h(protocolName)%>">
        <input name="runNames[0]" size=50 value="<%=h(runName)%>"></td></tr>
    <tr><td colspan="2"><%=renderXarInputs(0, getPathDescription())%></td></tr>
  </table>
    <%
    }
    else
    {
        if (form.isProtocolShare())
        {
%>
    <tr><td colspan="2" class="heading-1">Default Settings for All Runs</td></tr>
    <tr ><td >&nbsp;</td><td>
        <table border="0">
    <tr><td class="ms-searchform">Sample Set</td><td><%=materialSourceSelect(null, "defaults", 0, "defaultMaterialSource(this)")%></td></tr>
<%
            StringBuilder builder = new StringBuilder();
            int iParam = 0;
            ExperimentArchiveType xar = xarDocs[0].getExperimentArchive();
            ProtocolBaseType[] protocols = xar.getProtocolDefinitions().getProtocolArray();
            for (ProtocolBaseType protocol : protocols)
            {
                iParam = renderProtocolParameters(null, "defaults", protocol, builder, iParam, "updateDefaultParam(this)");
            }
            out.write(builder.toString());
%>
        </table>
        </td>
        </tr>
<%
        }

        int index = 0;
        for (File file : getMzXmlFileStatus().keySet())
        {
            FileStatus status = getMzXmlFileStatus().get(file);
            if (status != FileStatus.UNKNOWN)
                continue;   // Already annotated.

            String protocolName = "";
            String error = "";
            String runName = null;
            if (index < form.getRunNames().length)
            {
                if (form.getRunNames()[index] != null)
                {
                    runName = form.getRunNames()[index];
                }
                protocolName = form.getProtocolNames()[index];
                error = form.getError(index);
            }
            if (runName == null)
            {
                if (form.isProtocolShare())
                {
                    protocolName = form.getSharedProtocol();
                }
                runName = "MS2 Sample Prep (" + getStrippedFileName(file) + "), (" + protocolName + ")";
            }
%>
  <tr><td colspan="2" class="heading-1"><%=h(file.getName())%></td></tr>
  <tr><td>&nbsp;</td><td>
    <table border="0">
<%          if (error != null && error.length() > 0)
            { %>
    <tr><td class="labkey-error" colspan="2"><%=error%></td></tr>
<%          } %>
    <tr><td class="ms-searchform">Run Name</td>
      <td class="normal"><input type="hidden" name="fileNames[<%=index%>]" value="<%=h(file.getName())%>">
                        <input type="hidden" name="protocolNames[<%=index%>]" value="<%=h(protocolName)%>">
                        <input name="runNames[<%=index%>]" size=50 value="<%=h(runName)%>"></td></tr>
    <tr><td colspan="2"><%=renderXarInputs(index, getStrippedFileName(file))%>
      </td></tr>
    </table>
<%
            index++;
            }
        }%>

    </td>
    </tr>

 </table>
<labkey:button text="Submit"/>&nbsp;<labkey:button text="Cancel" href="<%=up.urlReferer(getViewContext().getContainer())%>"/>
</form>
<script type="">
    function defaultMaterialSource(sel)
    {
        var form = sel.form;
        var selects = document.getElementsByTagName("SELECT");
        for (var i = 0; i < selects.length; i++)
        {
            var elem = selects[i];
            if (elem.name.match(/runInfos\[\d*]\.materialSourceIds\[\d*]/))
            {
                elem.options[sel.selectedIndex].selected = true;
                elem.onchange();
            }
        }
    }

    function updateDefaultParam(textInput)
    {
        var name = textInput.name;
        name = name.replace(/defaults/, "runInfos[\\d*]");
        name = name.replace(/\./g, "\\.");
        name = name.replace(/\[/g, "\\[");
        var re = new RegExp(name);
        var form = textInput.form;
        for (var i = 0; i < form.elements.length; i++)
        {
            var elem = form.elements[i];
            if (elem.name.match(re))
            {
                elem.value = textInput.value;
            }
        }
    }

    var materialSourceMaterials = new Object();
<%  for (Map.Entry<Integer, ExpMaterial[]> entry : getMaterialSourceMaterials().entrySet())
    { %>
        var materials = new Object();
<%
        for (int i = 0; i < entry.getValue().length; i++)
        { %>
            var material = new Object();
            material.name = '<%= entry.getValue()[i].getName() %>';
            material.rowId = <%= entry.getValue()[i].getRowId() %>;
            materials[<%= i %>] = material;
<%      } %>
        materials.materialCount = <%= entry.getValue().length %>;
        materialSourceMaterials[<%= entry.getKey() %>] = materials;
<%  } %>

    function updateSamples(samplesSelect, sourceSelect, newRadio, existingRadio)
    {
        samplesSelect.options.length = 0;
        var selectedMaterials = materialSourceMaterials[sourceSelect.value];
        if (selectedMaterials != null && selectedMaterials.materialCount != 0)
        {
            var newSelected = newRadio.checked;
            for (var i = 0; i < selectedMaterials.materialCount; i++)
            {
                samplesSelect.options[i] = new Option(selectedMaterials[i].name, selectedMaterials[i].rowId);
            }
            if (newSelected)
            {
                newRadio.checked = true;
            }
            else
            {
                existingRadio.checked = true;
            }
        }
    }
</script>
