/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionMapping;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.HttpView;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.ms2.pipeline.MassSpecProtocol;
import org.labkey.ms2.pipeline.MassSpecProtocolFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

/**
 * <code>MS2ExperimentForm</code>
*/
public class MS2ExperimentForm extends MS2PipelineForm
{
    public enum Step
    {
        choosePath,
        pickProtocol,
        describeSamples
    }

    public enum ProtocolSharing
    {
        share,
        fractions,
        none
    }

    private Step step = Step.choosePath;
    private ProtocolSharing protocolSharing = ProtocolSharing.share;
    private String sharedProtocol;
    private String fractionProtocol;
    private String[] fileNames;
    private String[] protocolNames;
    private String[] runNames;
    private String[] errors;
    private MassSpecProtocol.RunInfo[] runInfos;

    // Calculated fields
    private File dirRoot;
    private File dirData;

    // On demand calculated fields
    private String[] protocolAvailableNames;
    private MassSpecProtocol[] protocols;
    private Map<File, FileStatus> mzXmlFileStatus;
    private Set<ExpRun> creatingRuns;
    private Set<File> annotationFiles;

    public void reset(ActionMapping am, HttpServletRequest request)
    {
        super.reset(am, request);
        int size = 0;
        try
        {
            size = Integer.parseInt(request.getParameter("size"));
        }
        catch (Exception e)
        {
        }

        fileNames = new String[size];
        protocolNames = new String[size];
        runNames = new String[size];
        errors = new String[size];
        runInfos = new MassSpecProtocol.RunInfo[size];
        for (int i = 0; i < size; i++)
        {
            String strCount = request.getParameter("parameterCounts[" + i + "]");
            if (null != StringUtils.trimToNull(strCount))
            {
                int parameterCount = Integer.parseInt(strCount);
                strCount = request.getParameter("materialCounts[" + i + "]");
                int materialCount = Integer.parseInt(strCount);

                runInfos[i] = new MassSpecProtocol.RunInfo(materialCount, parameterCount);
            }
            else
                runInfos[i] = new MassSpecProtocol.RunInfo(0, 0);
        }
    }

    public void calcFields() throws SQLException
    {
        PipeRoot pr = PipelineService.get().findPipelineRoot(getContainer());
        if (pr == null || !URIUtil.exists(pr.getUri()))
        {
            HttpView.throwNotFoundMV();
            return;
        }

        URI uriRoot = pr.getUri();
        URI uriData = URIUtil.resolve(pr.getUri(getContainer()), getPath());
        if (uriData == null)
        {
            HttpView.throwNotFoundMV();
            return;
        }

        dirRoot = new File(uriRoot);
        dirData = new File(uriData);
    }

    public boolean hasErrors()
    {
        for (String error : errors)
        {
            if (error != null)
                return true;
        }
        return false;
    }

    public String getError(int i)
    {
        if (null != errors)
            return errors[i];

        return null;
    }

    public void setError(int i, String error)
    {
        this.errors[i] = error;
    }

    public Step getStep()
    {
        return step;
    }

    public void setStep(Step step)
    {
        this.step = step;
    }

    public String getStepString()
    {
        return step.toString();
    }

    public void setStepString(String stepName)
    {
        this.step = Step.valueOf(stepName);
    }

    public String[] getFileNames()
    {
        return fileNames;
    }

    public void setFileNames(String[] fileNames)
    {
        this.fileNames = fileNames;
    }

    public String[] getProtocolNames()
    {
        return protocolNames;
    }

    public void setProtocolNames(String[] protocolNames)
    {
        this.protocolNames = protocolNames;
    }

    public String[] getRunNames()
    {
        return runNames;
    }

    public void setRunNames(String[] runNames)
    {
        this.runNames = runNames;
    }

    public MassSpecProtocol.RunInfo[] getRunInfos()
    {
        return runInfos;
    }

    public void setRunInfos(MassSpecProtocol.RunInfo[] runInfos)
    {
        this.runInfos = runInfos;
    }

    public boolean isProtocolShare()
    {
        return (protocolSharing == ProtocolSharing.share);
    }

    public boolean isProtocolFractions()
    {
        return (protocolSharing == ProtocolSharing.fractions);
    }

    public boolean isProtocolIndividual()
    {
        return (protocolSharing == ProtocolSharing.none);
    }

    public String getProtocolSharingString()
    {
        return protocolSharing.toString();
    }

    public void setProtocolSharingString(String protocolSharing)
    {
        this.protocolSharing = ProtocolSharing.valueOf(protocolSharing);
    }

    public String getSharedProtocol()
    {
        return sharedProtocol;
    }

    public void setSharedProtocol(String sharedProtocol)
    {
        this.sharedProtocol = sharedProtocol;
    }

    public String getFractionProtocol() {
        return fractionProtocol;
    }

    public void setFractionProtocol(String fractionProtocol) {
        this.fractionProtocol = fractionProtocol;
    }

    public File getDirRoot()
    {
        return dirRoot;
    }

    public File getDirData()
    {
        return dirData;
    }

    public Map<File, FileStatus> getMzXmlFileStatus() throws IOException
    {
        return mzXmlFileStatus;
    }

    public Set<ExpRun> getCreatingRuns()
    {
        return creatingRuns;
    }

    public Set<File> getAnnotationFiles()
    {
        return annotationFiles;
    }

    public String[] getProtocolAvailableNames()
    {
        if (protocolAvailableNames == null)
            protocolAvailableNames = MassSpecProtocolFactory.get().getProtocolNames(getDirRoot().toURI(), getDirData());
        return protocolAvailableNames;
    }

    public MassSpecProtocol[] getProtocols()
    {
        if (protocols == null)
        {
            MassSpecProtocol prot = null;

            URI uriRoot = dirRoot.toURI();
            if (isProtocolShare())
            {
                try
                {
                    prot = MassSpecProtocolFactory.get().load(uriRoot, getSharedProtocol());
                }
                catch (IOException eio)
                {
                    setError(0, "Failed to load protocol" + getSharedProtocol() + " - " + eio.toString());
                }

                protocols = new MassSpecProtocol[getFileNames().length];

                Arrays.fill(protocols, prot);
            }
            else if (isProtocolFractions())
            {
                try
                {
                    prot = MassSpecProtocolFactory.get().load(uriRoot, getFractionProtocol());
                }
                catch (IOException eio)
                {
                    setError(0, "Failed to load protocol " + getFractionProtocol() + " - " + eio);
                }

                protocols = new MassSpecProtocol[] {prot};
            }
            else
            {
                Map<String, MassSpecProtocol> protMap = new HashMap<String, MassSpecProtocol>();
                protocols = new MassSpecProtocol[getFileNames().length];
                for (int i =0; i < protocols.length; i++)
                {
                    String protocolName = getProtocolNames()[i];
                    if (null == protocolName)
                        continue;

                    prot = protMap.get(protocolName);
                    if (null == prot)
                    {
                        try
                        {
                            prot  = MassSpecProtocolFactory.get().load(uriRoot, protocolName);
                            protMap.put(protocolName, prot);
                        }
                        catch (IOException eio)
                        {
                            setError(i, "Couldn't load protocol " + protocolName + " - " + eio.toString());
                        }
                    }

                    protocols[i] = prot;
                }
            }
        }

        return protocols;
    }

    public void ensureMzXMLFileStatus() throws IOException
    {
        if (mzXmlFileStatus == null)
            mzXmlFileStatus = MS2PipelineManager.getAnalysisFileStatus(getDirData(), null, getContainer());

        creatingRuns = new HashSet<ExpRun>();
        annotationFiles = new HashSet<File>();
        for (File mzXMLFile : mzXmlFileStatus.keySet())
        {
            if (mzXmlFileStatus.get(mzXMLFile) == FileStatus.UNKNOWN)
                continue;

            ExpRun run = ExperimentService.get().getCreatingRun(mzXMLFile, getContainer());
            if (run != null)
            {
                creatingRuns.add(run);
            }
            File annotationFile = MS2PipelineManager.findAnnotationFile(mzXMLFile);
            if (annotationFile != null)
            {
                annotationFiles.add(annotationFile);
            }
        }
    }
}
