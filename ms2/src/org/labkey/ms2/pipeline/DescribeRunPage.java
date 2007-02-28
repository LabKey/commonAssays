package org.labkey.ms2.pipeline;

import org.fhcrc.cpas.exp.xml.*;
import org.labkey.api.exp.MaterialSource;
import org.labkey.api.exp.Material;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.PathRelativizer;
import org.labkey.api.jsp.JspBase;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerManager;
import org.labkey.ms2.protocol.MassSpecProtocol;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipeRoot;
import org.apache.commons.lang.StringUtils;
import org.labkey.ms2.pipeline.FileStatus;

import javax.servlet.ServletException;
import java.io.*;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.sql.SQLException;

/**
 * Copyright (C) 2004 Fred Hutchinson Cancer Research Center. All Rights Reserved.
 * User: migra
 * Date: Dec 8, 2005
 * Time: 10:24:01 AM
 */
abstract public class DescribeRunPage extends JspBase
{
    protected ExperimentArchiveDocument[] xarDocs;
    protected PipelineController.MS2ExperimentForm form;
    protected Map<File, FileStatus> mzXmlFileStatus;
    protected MaterialSource[] materialSources;
    protected PipelineController controller;
    private Map<Integer, Material[]> _materialSourceMaterials;

    private static Set<String> ignoreParams = PageFlowUtil.set(
            "terms.fhcrc.org#XarTemplate.ApplicationLSID",
            "terms.fhcrc.org#XarTemplate.ApplicationName",
            "terms.fhcrc.org#XarTemplate.OutputMaterialLSID",
            "terms.fhcrc.org#XarTemplate.OutputMaterialName",
            "terms.fhcrc.org#XarTemplate.OutputDataLSID",
            "terms.fhcrc.org#XarTemplate.OutputDataDir",
            "terms.fhcrc.org#XarTemplate.OutputDataName",
            "terms.fhcrc.org#XarTemplate.OutputDataFile",
            "terms.fhcrc.org#XarTemplate.OutputMaterialPerInstanceExpression");

    public void setForm(PipelineController.MS2ExperimentForm form)
    {
        this.form = form;
    }

    public void setXarDocs(ExperimentArchiveDocument[] xarDocs)
    {
        this.xarDocs = xarDocs;
    }

    protected String renderXarInputs(int runIndex, String defaultDescription) throws Exception
    {
        StringBuilder builder = new StringBuilder();
        ExperimentArchiveType xar = xarDocs[runIndex].getExperimentArchive();

        MassSpecProtocol.RunInfo runInfo = form.getRunInfos()[runIndex];
        String runPrefix = "runInfos[" + runIndex + "]";
        MaterialBaseType[] inputMaterials = xar.getStartingInputDefinitions().getMaterialArray();

        builder.append("<tr><td class=\"ms-searchform\" colspan=2>Input Samples <span class=\"ms-vb\">(Enter sample ID and choose source for sample)</span></td></tr>");
        int genericRoleNameCount = 0;
        for (int iMaterial = 0; iMaterial < inputMaterials.length; iMaterial++)
        {
            MaterialBaseType inputMaterial = inputMaterials[iMaterial];
            String existingLSID = inputMaterial.getAbout();
            String roleName;
            if (existingLSID != null && existingLSID.startsWith("${Role:") && existingLSID.endsWith("}"))
            {
                roleName = existingLSID.substring("${Role:".length(), existingLSID.length() - "}".length());
            }
            else
            {
                genericRoleNameCount++;
                if (genericRoleNameCount == 1)
                {
                    roleName = "Material";
                }
                else
                {
                    roleName = "Material" + genericRoleNameCount;
                }
            }
            renderMaterialInput(runInfo, runPrefix, iMaterial, builder, defaultDescription, roleName);
        }
        builder.append("<input type=\"hidden\" name=\"materialCounts[").append(runIndex).append("]\" value=\"").append(inputMaterials.length).append("\">");

        int iParam = 0;
        ProtocolBaseType[] protocols = xar.getProtocolDefinitions().getProtocolArray();
        for (int iProtocol = 0; iProtocol < protocols.length; iProtocol++)
        {
            ProtocolBaseType protocol = protocols[iProtocol];
            iParam = renderProtocolParameters(runInfo.getParameterValues(), runPrefix, protocol, builder, iParam, null);
        }
        builder.append("<input type=\"hidden\" name=\"parameterCounts[").append(runIndex).append("]\" value=\"").append(iParam).append("\">");

        return builder.toString();
    }

    protected int renderProtocolParameters(String[] parameterValues, String runPrefix, ProtocolBaseType protocol, StringBuilder builder, int iParam, String onchange)
    {
        boolean headerWritten = false;
        SimpleValueType[] params = protocol.getParameterDeclarations().getSimpleValArray();
        for (SimpleValueType param : params)
        {
            if (!ignoreParams.contains(param.getOntologyEntryURI()))
            {
                if (!headerWritten)
                {
                    builder.append("<tr><td class=\"ms-searchform\" colspan=2>");
                    builder.append("Parameters for protocol ").append(protocol.getName());
                    builder.append("</td></tr>");
                    headerWritten = true;
                }
                builder.append("<tr><td class=\"ms-vb\">");
                builder.append(ColumnInfo.captionFromName(param.getName()));
                builder.append("</td><td class=\"ms-vb\">");
                builder.append("<input name=\"").append(runPrefix).append(".parameterValues[").append(iParam).append("]\" ");
                builder.append("value=\"");
                if (null != parameterValues && parameterValues.length > iParam && null != parameterValues[iParam])
                    builder.append(h(parameterValues[iParam]));
                else
                    builder.append(h(param.getStringValue()));
                builder.append("\" ");
                if (null != onchange)
                    builder.append("onchange=\"").append(onchange).append("\" ");
                builder.append(">");
                builder.append("</td></tr>");
                iParam++;
            }
        }
        return iParam;
    }

    private void renderMaterialInput(MassSpecProtocol.RunInfo runInfo, String runPrefix, int index, StringBuilder builder, String defaultDescription, String roleName) throws IOException
    {
        builder.append("<tr><td class=\"ms-searchform\">");
        builder.append(roleName);
        builder.append(" Sample ID</td><td class=\"ms-vb\">");
        String namePrefix = runPrefix + ".sampleIds";
        String nameSuffix = "[" + index + "]";
        builder.append("<input type=\"radio\" id=\"" + namePrefix + "NewType" + nameSuffix + "\" name=\"" + namePrefix + "Type" + nameSuffix + "\" checked=\"checked\" value=\"New\"/>");
        builder.append("<input type=\"text\" size=\"50\" name=\"" + namePrefix + "New" + nameSuffix + "\" onKeyUp=\"document.getElementById('" + namePrefix + "NewType" + nameSuffix + "').checked = true;\"" );
        String[] sampleIds = runInfo.getSampleIdsNew();
        String sampleId = sampleIds != null && index < sampleIds.length ? sampleIds[index] : null;

        builder.append("value=\"");
        if (null != StringUtils.trimToNull(sampleId))
        {
            builder.append(h(sampleId));
        }
        else
        {
            builder.append("Sample for ");
            builder.append(defaultDescription);
        }
        builder.append("\" ");

        Integer[] materialSourceIds = runInfo.getMaterialSourceIds();
        builder.append(" >");
        builder.append("<br>\n");
        builder.append("<input type=\"radio\" id=\"" + namePrefix + "ExistingType" + nameSuffix + "\" name=\"" + namePrefix + "Type" + nameSuffix + "\" value=\"Existing\"/>\n");
        builder.append("<select name=\"" + namePrefix + "Existing" + nameSuffix + "\" onchange=\"document.getElementById('" + namePrefix + "ExistingType" + nameSuffix + "').checked = true;\">");
        if (getMaterialSources().length > 0)
        {
            Material[] materials = _materialSourceMaterials.get(this.getMaterialSources()[0].getRowId());
            if (materials != null)
            {
                for (Material material : materials)
                {
                    builder.append("\n<option value=\"");
                    builder.append(material.getRowId());
                    builder.append("\">");
                    builder.append(h(material.getName()));
                    builder.append("</option>");
                }
            }
        }
        builder.append("</select>");
        builder.append("</td></tr><tr><td class=\"ms-searchform\">");
        builder.append(roleName);
        builder.append(" Sample Set</td><td class=\"ms-vb\">");
        String onchange = "updateSamples(document.describeForm['" + namePrefix + "Existing" + nameSuffix + "'], document.describeForm['" + runPrefix + ".materialSourceIds[" + index + "]'], document.getElementById('" + namePrefix + "NewType" + nameSuffix + "'), document.getElementById('" + namePrefix + "ExistingType" + nameSuffix + "'))";
        builder.append(materialSourceSelect(materialSourceIds, runPrefix, index, onchange));
        builder.append("</td></tr>");
    }

    protected String getPathDescription() throws SQLException, ServletException, IOException
    {
        Iterator<File> mzXMLFiles = getMzXmlFileStatus().keySet().iterator();
        if (mzXMLFiles.hasNext())
        {
            File parentFile = mzXMLFiles.next().getParentFile();
            if (parentFile != null)
            {
                PipeRoot root = PipelineService.get().findPipelineRoot(getController().getContainer());
                if (root != null)
                {
                    File rootFile = new File(root.getUri(getController().getContainer()));
                    String result = PathRelativizer.relativizePathUnix(rootFile, parentFile);
                    if (result.length() > 0)
                    {
                        return "root/" + result;
                    }
                    return "root";
                }
            }
        }
        return "root";
    }

    protected String getStrippedFileName(File f)
    {
        String result = f.getName();
        int index = result.lastIndexOf(".");
        if (index != -1)
        {
            return result.substring(0, index);
        }
        return result;
    }

    protected String materialSourceSelect(Integer[] materialSourceIds, String runPrefix, int index, String onchange)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("<select name=\"").append(runPrefix).append(".materialSourceIds[").append(index).append("]\" ");
        if (null != onchange)
            builder.append("onchange=\"").append(onchange).append("\"");
        builder.append(">\n");

        boolean activeSource = true;
        for(MaterialSource source : materialSources)
        {
            builder.append("<option ");
            if (activeSource)
            {
                builder.append("selected ");
            }
            activeSource = false;

            builder.append("value=\"");
            builder.append(source.getRowId());
            builder.append("\">");
            builder.append(PageFlowUtil.filter(source.getName()));
            if (!source.getContainer().equals(form.getContainer().getId()))
            {
                String containerPath = ContainerManager.getForId(source.getContainer()).getPath();
                builder.append(" in ").append(containerPath);
            }

            builder.append("</option>\n");
        }
        builder.append("</select>");

        return builder.toString();
    }

    public ExperimentArchiveDocument[] getXarDocs()
    {
        return xarDocs;
    }

    public PipelineController.MS2ExperimentForm getForm()
    {
        return form;
    }

    public Map<File, FileStatus> getMzXmlFileStatus()
    {
        return mzXmlFileStatus;
    }

    public void setMzXmlFileStatus(Map<File, FileStatus> mzXmlFileStatus)
    {
        this.mzXmlFileStatus = mzXmlFileStatus;
    }

    public MaterialSource[] getMaterialSources()
    {
        return materialSources;
    }

    public void setMaterialSources(MaterialSource[] materialSources)
    {
        this.materialSources = materialSources;
    }

    public PipelineController getController()
    {
        return controller;
    }

    public void setController(PipelineController controller)
    {
        this.controller = controller;
    }

    public void setMaterialSourceMaterials(Map<Integer, Material[]> materialSourceMaterials)
    {
        _materialSourceMaterials = materialSourceMaterials;
    }

    public Map<Integer, Material[]> getMaterialSourceMaterials()
    {
        return _materialSourceMaterials;
    }
}
