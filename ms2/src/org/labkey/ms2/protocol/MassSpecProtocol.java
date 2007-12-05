/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.protocol;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.fhcrc.cpas.exp.xml.*;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipelineProtocol;
import org.labkey.api.util.PageFlowUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * MassSpecProtocol class
 * <p/>
 * Created: Oct 9, 2005
 *
 * @author bmaclean
 */
public class MassSpecProtocol extends PipelineProtocol
{
    private static Logger _log = Logger.getLogger(MassSpecProtocol.class);
    private Map<String, String> tokenReplacements = new HashMap<String, String>();

    public MassSpecProtocol()
    {
    }

    public MassSpecProtocol(String name, String templateName)
    {
        setName(name);
        setTemplate(templateName);
    }

    public MassSpecProtocol(String name, String templateName, Map<String, String> replacements)
    {
        setName(name);
        setTemplate(templateName);
        this.tokenReplacements.putAll(replacements);
    }

    /**
     * We're not concerned with bean properties so we just put any "properties" into a map.
     */
    @Override
    public void setProperty(String propertyName, String value)
    {

        if ("name".equals(propertyName))
        {
            setName(value);
        }
        else
            tokenReplacements.put(propertyName, value);
    }

    @Override
    protected Map<String, String> getSaveProperties()
    {
        Map<String, String> saveProperties = new HashMap<String, String>(tokenReplacements);
        saveProperties.put("name", getName());
        return saveProperties;
    }

    public MassSpecProtocolFactory getFactory()
    {
        return MassSpecProtocolFactory.get();
    }

    public String getInstanceText(URI uriRoot)
    {
        Map<String, String> tokenMap = new HashMap<String, String>();
        tokenMap.put("PROTOCOL_NAME", PageFlowUtil.filter(getName()));
        tokenMap.putAll(tokenReplacements);

        return tokenReplace(getTemplateText(uriRoot), tokenMap);
    }

    //TODO: (marki) This should be cached & clone should be optional
    public ExperimentArchiveDocument getInstanceXar(URI uriRoot) throws XmlException
    {
        return ExperimentArchiveDocument.Factory.parse(getInstanceText(uriRoot));
    }

    public void saveInstance(URI uriRoot, File file, RunInfo runInfo) throws IOException, XmlException
    {
        Map<String, String> tokenMap = new HashMap<String, String>();
        File parentDir = file.getParentFile();
        if (!parentDir.exists())
        {
            if (!parentDir.mkdirs())
            {
                throw new IOException("Unable to create directory " + parentDir);
            }
        }

        //TODO: (marki) Don't use tokens here. Just edit as with other inputs
        tokenMap.put("RUN_FILENAME", "../" + runInfo.getRunFileName());
        tokenMap.put("RUN_NAME", runInfo.getRunName());
        String instanceText = getInstanceText(uriRoot);
        instanceText = tokenReplace(instanceText, tokenMap);


        ExperimentArchiveDocument xarDoc = ExperimentArchiveDocument.Factory.parse(instanceText);
        ExperimentArchiveType xar = xarDoc.getExperimentArchive();

        applyRunMaterials(xarDoc, runInfo);
        applyRunParameters(xarDoc, runInfo);

        xarDoc.save(file);
    }

    public String getTemplateText(URI uriRoot)
    {
        return getFactory().getTemplateText(uriRoot, getTemplate());
    }

    public ExperimentArchiveDocument getTemplateXar(URI uriRoot) throws XmlException
    {
        return ExperimentArchiveDocument.Factory.parse(getTemplateText(uriRoot));
    }


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


    private void applyRunParameters(ExperimentArchiveDocument xarDoc, RunInfo runInfo)
    {
        ExperimentArchiveType xar = xarDoc.getExperimentArchive();
        ProtocolBaseType[] protocols = xar.getProtocolDefinitions().getProtocolArray();
        String[] parameterValues = runInfo.getParameterValues();
        int iParam = 0;
        for (ProtocolBaseType protocol : protocols)
        {
            //Find the protocol action number
            SimpleValueType[] params = protocol.getParameterDeclarations().getSimpleValArray();
            for (SimpleValueType param : params)
            {
                if (!ignoreParams.contains(param.getOntologyEntryURI()))
                {
                    ExperimentLogEntryType entry = getLogEntry(xar, protocol);
                    if (null == entry)
                    {
                        _log.info("Couldn't find log entry for protocol: " + protocol.getAbout() + ". Not applying value " + parameterValues[iParam] + " for " + param.getOntologyEntryURI());
                        iParam++; //We would have asked user to fill in, so
                        continue;
                    }

                    if (!entry.isSetCommonParametersApplied())
                        entry.addNewCommonParametersApplied();

                    SimpleValueCollectionType svct = entry.getCommonParametersApplied();
                    SimpleValueType appliedParam = null;
                    for (SimpleValueType svt : svct.getSimpleValArray())
                    {
                        if (svt.getOntologyEntryURI().equals(param.getOntologyEntryURI()))
                        {
                            appliedParam = svt;
                            break;
                        }
                    }
                    if (null == appliedParam)
                    {
                        appliedParam = svct.addNewSimpleVal();
                        appliedParam.setName(param.getName());
                        appliedParam.setOntologyEntryURI(param.getOntologyEntryURI());
                        appliedParam.setValueType(param.getValueType());
                    }
                    appliedParam.setStringValue(parameterValues[iParam]);
                    iParam++;
                }
            }
        }

    }

    private int getActionSequence(ExperimentArchiveType xar, ProtocolBaseType protocol)
    {
        ExperimentArchiveType.ProtocolActionDefinitions defs = xar.getProtocolActionDefinitions();
        ProtocolActionSetType[] pas = defs.getProtocolActionSetArray();

        for (ProtocolActionSetType setType : pas)
        {
            for (ProtocolActionType actionType : setType.getProtocolActionArray())
            {
                if (actionType.getChildProtocolLSID().equals(protocol.getAbout()))
                    return actionType.getActionSequence();
            }
        }

        return -1;
    }

    private ExperimentLogEntryType getLogEntry(ExperimentArchiveType xar, ProtocolBaseType protocol)
    {
        int seq = getActionSequence(xar, protocol);
        for (ExperimentLogEntryType entry : xar.getExperimentRuns().getExperimentRunArray(0).getExperimentLog().getExperimentLogEntryArray())
        {
            if (entry.getActionSequenceRef() == seq)
                return entry;
        }

        return null;
    }

    private void applyRunMaterials(ExperimentArchiveDocument xarDoc, RunInfo runInfo)
    {
        ExperimentArchiveType xar = xarDoc.getExperimentArchive();
        MaterialBaseType[] inputMaterials = xar.getStartingInputDefinitions().getMaterialArray();
        Map<String, List<String>> roleLSIDs = new HashMap<String, List<String>>();
        for (int i = 0; i < inputMaterials.length; i++)
        {
            String materialLSID;
            String materialName;

            MaterialBaseType inputMaterial = inputMaterials[i];
            String existingLSID = inputMaterial.getAbout();
            String roleName = null;
            if (existingLSID != null && existingLSID.startsWith("${Role:") && existingLSID.endsWith("}"))
            {
                roleName = existingLSID.substring("${Role:".length(), existingLSID.length() - "}".length());
            }
            if ("Material".equals(roleName))
            {
                roleName = null;
            }

            if ("Existing".equals(runInfo.getSampleIdsType()[i]) && runInfo.getSampleIdsExisting()[i] != null)
            {
                ExpMaterial material = ExperimentService.get().getExpMaterial(runInfo.getSampleIdsExisting()[i].intValue());
                materialLSID = material.getLSID();
                materialName = material.getName();
            }
            else
            {
                Integer matSourceId = runInfo.getMaterialSourceIds()[i];
                ExpSampleSet matSource = ExperimentService.get().getSampleSet(matSourceId);

                materialLSID = matSource.getMaterialLSIDPrefix() + runInfo.getSampleIdsNew()[i];
                materialName = runInfo.getSampleIdsNew()[i];
            }
            List<String> existingLSIDs = roleLSIDs.get(roleName);
            if (existingLSIDs == null)
            {
                existingLSIDs = new ArrayList<String>();
                roleLSIDs.put(roleName, existingLSIDs);
            }
            existingLSIDs.add(materialLSID);
            inputMaterials[i].setAbout(materialLSID);
            inputMaterials[i].setName(materialName);
        }

        ExperimentArchiveType.ExperimentRuns runs = xarDoc.getExperimentArchive().getExperimentRuns();
        if (runs != null)
        {
            for (ExperimentRunType run : runs.getExperimentRunArray())
            {
                if (run.getExperimentLog() != null && run.getExperimentLog().getExperimentLogEntryArray() != null)
                {
                    for (ExperimentLogEntryType logEntry : run.getExperimentLog().getExperimentLogEntryArray())
                    {
                        if (logEntry.getApplicationInstanceCollection() != null &&
                            logEntry.getApplicationInstanceCollection().getInstanceDetailsArray() != null)
                        {
                            for (InstanceDetailsType details : logEntry.getApplicationInstanceCollection().getInstanceDetailsArray())
                            {
                                if (details.getInstanceInputs() != null && details.getInstanceInputs().getMaterialLSIDArray() != null)
                                {
                                    for (InputOutputRefsType.MaterialLSID materialLSID : details.getInstanceInputs().getMaterialLSIDArray())
                                    {
                                        List<String> lsids = roleLSIDs.get(materialLSID.getRoleName());
                                        if (lsids != null && !lsids.isEmpty())
                                        {
                                            materialLSID.setStringValue(lsids.remove(0));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public List<SimpleValueType> getEditableParameters(ExperimentArchiveDocument xarDoc)
    {
        ExperimentArchiveType xar = xarDoc.getExperimentArchive();
        ProtocolBaseType[] protocols = xar.getProtocolDefinitions().getProtocolArray();
        List<SimpleValueType> editableParameters = new ArrayList<SimpleValueType>();
        for (ProtocolBaseType protocol : protocols)
        {
            SimpleValueType[] params = protocol.getParameterDeclarations().getSimpleValArray();
            for (SimpleValueType param : params)
            {
                if (!ignoreParams.contains(param.getOntologyEntryURI()))
                    editableParameters.add(param);
            }
        }

        return editableParameters;
    }

    /**
     * Replaces tokens surrounded by @@TOKEN_NAME@@ in an input stream
     */
    protected String tokenReplace(String s, Map<String, String> tokens)
    {
        StringBuilder sb = new StringBuilder(s);

        for (Map.Entry<String, String> entry : tokens.entrySet())
        {
            String replacement = PageFlowUtil.filter(entry.getValue(), false, false); 
            replaceString(sb, entry.getKey(), replacement);
        }

        return sb.toString();
    }

    private void replaceString(StringBuilder sb, String oldString, String newString)
    {
        oldString = "@@" + oldString + "@@";
        int index = sb.indexOf(oldString);
        while (index != -1)
        {
            sb.replace(index, index + oldString.length(), newString);
            index = sb.indexOf(oldString);
        }
    }

    public String validateSubstitution(URI uriRoot, RunInfo runInfo) throws XmlException
    {
        ExperimentArchiveDocument xarDoc = getInstanceXar(uriRoot);
        StringBuilder errorBuilder = new StringBuilder();
        String sep = "";

        int numInputs = xarDoc.getExperimentArchive().getStartingInputDefinitions().sizeOfMaterialArray();
        for (int i = 0; i < numInputs; i++)
        {
            if (null == runInfo.materialSourceIds[i])
            {
                errorBuilder.append("Please choose sample source for input ").append(i + 1);
                sep = "<br>";
            }
            if ("Existing".equals(runInfo.sampleIdsType[i]))
            {
                if (runInfo.sampleIdsExisting[i] == null)
                {
                    errorBuilder.append("Please enter a sample id for input ").append(i + 1);
                    sep = "<br>";
                }
                else
                {
                    ExpMaterial material = ExperimentService.get().getExpMaterial(runInfo.sampleIdsExisting[i]);
                    if (material == null)
                    {
                        errorBuilder.append("Please enter a sample id for input ").append(i + 1);
                        sep = "<br>";
                    }
                }
            }
            else
            {
                if (null == StringUtils.trimToNull(runInfo.sampleIdsNew[i]))
                {
                    errorBuilder.append("Please enter a sample id for input ").append(i + 1);
                    sep = "<br>";
                }
            }
        }

        List<SimpleValueType> paramDefinitions = getEditableParameters(xarDoc);
        //TODO: (migra) Required vs optional parameters? All optional for now
        String[] paramValues = runInfo.getParameterValues();
        for (int i = 0; i < paramDefinitions.size(); i++)
        {
            SimpleValueType svt = paramDefinitions.get(i);
            String paramVal = paramValues[i];
            if (null == StringUtils.trimToNull(paramVal))
                continue;

            SimpleTypeNames.Enum type = svt.getValueType();
            if (type != SimpleTypeNames.FILE_LINK && type != SimpleTypeNames.PROPERTY_URI)
            {
                Class javaClass = PropertyType.getFromXarName(svt.getValueType().toString()).getJavaType();
                try
                {
                    Object o = ConvertUtils.convert(paramVal, javaClass);
                }
                catch (ConversionException c)
                {
                    errorBuilder.append(sep).append("Could not convert property ").append(svt.getName()).append(" to type ").append(svt.getValueType());
                    sep = "<br>";
                }
            }
        }

        return errorBuilder.length() > 0 ? errorBuilder.toString() : null;
    }

    public static class RunInfo
    {
        //We use parallel arrays just to make postback easier.
        private Integer[] materialSourceIds;
        private String[] sampleIdsNew;
        private String[] sampleIdsType;
        private Integer[] sampleIdsExisting;
        private String[] parameterValues;
        private String runName;
        private String runFileName;
        
        public RunInfo(int materialCount, int parameterCount)
        {
            materialSourceIds = new Integer[materialCount];
            sampleIdsNew = new String[materialCount];
            sampleIdsType = new String[materialCount];
            sampleIdsExisting = new Integer[materialCount];
            parameterValues = new String[parameterCount];

        }

        public Integer[] getMaterialSourceIds()
        {
            return materialSourceIds;
        }

        public void setMaterialSourceIds(Integer[] materialSourceIds)
        {
            this.materialSourceIds = materialSourceIds;
        }

        public String[] getSampleIdsNew()
        {
            return sampleIdsNew;
        }

        public void setSampleIdsNew(String[] sampleIdsNew)
        {
            this.sampleIdsNew = sampleIdsNew;
        }

        public Integer[] getSampleIdsExisting()
        {
            return sampleIdsExisting;
        }

        public void setSampleIdsExisting(Integer[] sampleIdsExisting)
        {
            this.sampleIdsExisting = sampleIdsExisting;
        }

        public String[] getSampleIdsType()
        {
            return sampleIdsType;
        }

        public void setSampleIdsType(String[] sampleIdsType)
        {
            this.sampleIdsType = sampleIdsType;
        }

        public String[] getParameterValues()
        {
            return parameterValues;
        }

        public void setParameterValues(String[] parameterValues)
        {
            this.parameterValues = parameterValues;
        }

        public void setRunName(String runName)
        {
            this.runName = runName;
        }

        public void setRunFileName(String runFileName)
        {
            this.runFileName = runFileName;
        }

        public String getRunName()
        {
            return runName;
        }

        public String getRunFileName()
        {
            return runFileName;
        }
    }
}
