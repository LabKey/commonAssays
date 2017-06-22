/*
 * Copyright (c) 2005-2017 LabKey Corporation
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

package org.labkey.flow.script;

import org.apache.commons.lang3.StringUtils;
import org.fhcrc.cpas.exp.xml.DataBaseType;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveType;
import org.fhcrc.cpas.exp.xml.ExperimentRunType;
import org.fhcrc.cpas.exp.xml.InputOutputRefsType;
import org.fhcrc.cpas.exp.xml.ProtocolApplicationBaseType;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.util.StringUtilsLabKey;
import org.labkey.api.util.URIUtil;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.FCSKeywordData;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.data.FlowProperty;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.SampleKey;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.FlowDataHandler;
import org.labkey.flow.persist.InputRole;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class KeywordsHandler extends BaseHandler
{
    Pattern _fcsFilePattern;

    protected boolean shouldUploadKeyword(String name)
    {
        if (true)
            return true;
        if (name.startsWith("$"))
        {
            return name.equals("$FIL") || name.equals("$DATE") || name.equals("$TOT") || name.startsWith("$P") && name.endsWith("V");
        }
        if (name.endsWith("DISPLAY"))
        {
            return false;
        }
        if (name.equals("SPILL") ||
                name.equals("WINDOW EXTENSION") ||
                name.equals("APPLY COMPENSATION") ||
                name.equals("CREATOR") ||
                name.equals("FSC ASF") ||
                name.equals("THRESHOLD"))
            return false;
        return true;
    }

    public KeywordsHandler(ScriptJob job)
    {
        super(job, FlowProtocolStep.keywords);
    }

    protected boolean isFCSFile(File file)
    {
        if (file.isDirectory())
            return false;
        if (_fcsFilePattern != null)
            return _fcsFilePattern.matcher(file.getName()).matches();
        return FCSAnalyzer.get().isFCSFile(file);
    }

    protected boolean isSupportedFCSVersion(File file){
        return FCSAnalyzer.get().isSupportedFCSVersion(file);
    }

    private boolean isEmpty(String str)
    {
        return str == null || str.length() == 0;
    }

    protected void addStatus(String status)
    {
        _job.addStatus(status);
    }

    protected void warn(String status)
    {
        _job.warn(status);
    }

    protected FlowRun addRun(File directory, List<FCSKeywordData> data) throws Exception
    {
        ExperimentArchiveDocument xarDoc = _job.createExperimentArchive();
        ExperimentArchiveType xar = xarDoc.getExperimentArchive();
        String runName = null;
        File runDirectory = _job.createAnalysisDirectory(directory, FlowProtocolStep.keywords);

        runName = directory.getName();

        ExperimentRunType run = _job.addExperimentRun(xar, runName);

        for (FCSKeywordData fileData : data)
        {
            ProtocolApplicationBaseType app = addProtocolApplication(run);
            String filename = URIUtil.getFilename(fileData.getURI());
            // Using AutoFileLSID will try to find an existing exp.data (say an FCS file uploaded via webdav)
            // otherwise will create a new LSID using the data's file url.
            String lsidFile = "${AutoFileLSID}";
            _job.addStartingInput(lsidFile, filename, new File(fileData.getURI().getPath()), null);
            InputOutputRefsType.DataLSID dataLSID = app.getInputRefs().addNewDataLSID();
            dataLSID.setDataFileUrl(fileData.getURI().toString());
            dataLSID.setStringValue(lsidFile);

            ProtocolApplicationBaseType.OutputDataObjects outputs = app.getOutputDataObjects();
            DataBaseType well = outputs.addNewData();
            well.setName(filename);
            well.setAbout(FlowDataObject.generateDataLSID(_job.getContainer(), FlowDataType.FCSFile));
            well.setCpasType(ExpData.DEFAULT_CPAS_TYPE);
            AttributeSet attrSet = new AttributeSet(fileData);
            attrSet.save(_job.decideFileName(runDirectory, filename, FlowDataHandler.EXT_DATA), well);
            SampleKey sampleKey = _job.getProtocol().makeSampleKey(runName, well.getName(), attrSet);
            ExpMaterial sample = _job.getSampleMap().get(sampleKey);
            if (sample != null)
            {
                _job.addStartingMaterial(sample);
                InputOutputRefsType.MaterialLSID mlsid = app.getInputRefs().addNewMaterialLSID();
                mlsid.setStringValue(sample.getLSID());
                // mlsid.setRoleName(InputRole.Sample.toString());
            }

            _job.addRunOutput(well.getAbout(), InputRole.FCSFile);
        }
        _job.finishExperimentRun(xar, run);
        _job.importRuns(xarDoc, directory, runDirectory, FlowProtocolStep.keywords);
        _job.deleteAnalysisDirectory(runDirectory.getParentFile());

        return FlowRun.fromLSID(run.getAbout());
    }

    protected FlowRun importRun(File directory, Container targetStudy) throws Exception
    {
        addStatus("Reading keywords from directory " + directory);
        File[] files = directory.listFiles();
        List<FCSKeywordData> lstFileData = new ArrayList();

        for (int i = 0; i < files.length; i ++)
        {
            File file = files[i];
            if (!isFCSFile(file))
                continue;

            if (!isSupportedFCSVersion(file))
            {
                warn("The FCS version " + FCS.getFcsVersion(file) + " is not supported for file " + file.getName() +
                ". Supported versions are " + StringUtils.join(FCS.supportedVersions,",") + ".");
            }
            addStatus("Reading keywords from file " + file.getName());
            lstFileData.add( getAnalyzer().readAllKeywords(file.toURI()));
        }
        if (lstFileData.size() == 0)
        {
            addStatus("No FCS files found");
            return null;
        }

        FlowRun run = addRun(directory, lstFileData);
        if (targetStudy != null)
        {
            addStatus("Setting target study on keyword run: " + targetStudy);
            run.setProperty(_job.getUser(), FlowProperty.TargetStudy.getPropertyDescriptor(), targetStudy.getId());
        }

        return run;
    }

    protected FCSAnalyzer getAnalyzer()
    {
        return FCSAnalyzer.get();
    }

    public void processRun(FlowRun srcRun, ExperimentRunType runElement, File workingDirectory) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    public FlowRun run(File directory, Container targetStudy) throws Exception
    {
        return importRun(directory, targetStudy);
    }


    public String getRunName(FlowRun srcRun)
    {
        throw new UnsupportedOperationException();
    }
}
