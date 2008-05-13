/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import org.fhcrc.cpas.exp.xml.*;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.jsp.JspBase;
import org.labkey.api.data.ColumnInfo;
import org.labkey.ms2.pipeline.MassSpecProtocol;
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
 * User: migra
 * Date: Dec 8, 2005
 * Time: 10:24:01 AM
 */
abstract public class DescribeRunPage extends JspBase
{
    protected ExperimentArchiveDocument[] xarDocs;
    protected MS2ExperimentForm form;
    protected Map<File, FileStatus> mzXmlFileStatus;
    protected ExpSampleSet[] sampleSets;
    private Map<Integer, ExpMaterial[]> _materialSourceMaterials;

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

    public void setForm(MS2ExperimentForm form)
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

        builder.append("<tr><td class=\"ms-searchform\" colspan=2>Input Samples <span class=\"normal\">(Enter sample ID and choose source for sample)</span></td></tr>");
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
                builder.append("<tr><td class=\"normal\">");
                builder.append(ColumnInfo.captionFromName(param.getName()));
                builder.append("</td><td class=\"normal\">");
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
        builder.append(" Sample ID</td><td class=\"normal\">");
        String namePrefix = runPrefix + ".sampleIds";
        String nameSuffix = "[" + index + "]";
        builder.append("<table><tr><td><input type=\"radio\" id=\"" + namePrefix + "NewType" + nameSuffix + "\" name=\"" + namePrefix + "Type" + nameSuffix + "\" checked=\"checked\" value=\"New\"/></td><td>\n");
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
        builder.append(" ></tr><tr><td>\n");
        builder.append("<input type=\"radio\" id=\"" + namePrefix + "ExistingType" + nameSuffix + "\" name=\"" + namePrefix + "Type" + nameSuffix + "\" value=\"Existing\"/></td><td>\n");
        builder.append("<select name=\"" + namePrefix + "Existing" + nameSuffix + "\" onchange=\"document.getElementById('" + namePrefix + "ExistingType" + nameSuffix + "').checked = true;\">");
        if (getSampleSets().length > 0)
        {
            ExpMaterial[] materials = _materialSourceMaterials.get(this.getSampleSets()[0].getRowId());
            if (materials != null)
            {
                for (ExpMaterial material : materials)
                {
                    builder.append("\n<option value=\"");
                    builder.append(material.getRowId());
                    builder.append("\">");
                    builder.append(h(material.getName()));
                    builder.append("</option>");
                }
            }
        }
        builder.append("</select></td></tr></table>");
        builder.append("</td></tr><tr><td class=\"ms-searchform\">");
        builder.append(roleName);
        builder.append(" Sample Set</td><td class=\"normal\">");
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
                PipeRoot root = PipelineService.get().findPipelineRoot(getViewContext().getContainer());
                if (root != null)
                {
                    File rootFile = new File(root.getUri(getViewContext().getContainer()));
                    String result = FileUtil.relativizeUnix(rootFile, parentFile);
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
        for(ExpSampleSet source : sampleSets)
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
            if (!source.getContainer().equals(form.getContainer()))
            {
                String containerPath = source.getContainer().getPath();
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

    public MS2ExperimentForm getForm()
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

    public ExpSampleSet[] getSampleSets()
    {
        return sampleSets;
    }

    public void setSampleSets(ExpSampleSet[] sampleSets)
    {
        this.sampleSets = sampleSets;
    }

    public void setMaterialSourceMaterials(Map<Integer, ExpMaterial[]> materialSourceMaterials)
    {
        _materialSourceMaterials = materialSourceMaterials;
    }

    public Map<Integer, ExpMaterial[]> getMaterialSourceMaterials()
    {
        return _materialSourceMaterials;
    }
}
