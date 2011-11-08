/*
 * Copyright (c) 2005-2011 LabKey Corporation
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

import org.fhcrc.cpas.exp.xml.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.util.URIUtil;
import org.labkey.flow.analysis.model.FCSKeywordData;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.data.*;
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

    public KeywordsHandler(ScriptJob job) throws Exception
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

    private boolean isEmpty(String str)
    {
        return str == null || str.length() == 0;
    }

    protected void addStatus(String status)
    {
        _job.addStatus(status);
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
            ProtocolApplicationBaseType.OutputDataObjects outputs = app.getOutputDataObjects();
            String filename = URIUtil.getFilename(fileData.getURI());
            String lsidFile = ExperimentService.get().generateGuidLSID(_job.getContainer(), ExpData.class);
            _job.addStartingInput(lsidFile, URIUtil.getFilename(fileData.getURI()), null, null);
            app.getInputRefs().addNewDataLSID().setStringValue(lsidFile);

            DataBaseType well = outputs.addNewData();
            well.setName(filename);
            well.setAbout(FlowDataObject.generateDataLSID(_job.getContainer(), FlowDataType.FCSFile));
            well.setCpasType("Data");
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
        _job.deleteAnalysisDirectory(runDirectory);

        return FlowRun.fromLSID(run.getAbout());
    }

    protected FlowRun importRun(File directory) throws Exception
    {
        addStatus("Reading keywords from directory " + directory);
        File[] files = directory.listFiles();
        List<FCSKeywordData> lstFileData = new ArrayList();

        for (int i = 0; i < files.length; i ++)
        {
            File file = files[i];
            if (!isFCSFile(file))
                continue;
            addStatus("Reading keywords from file " + file.getName());
            lstFileData.add( getAnalyzer().readAllKeywords(file.toURI()));
        }
        if (lstFileData.size() == 0)
        {
            addStatus("No FCS files found");
            return null;
        }
        return addRun(directory, lstFileData);
    }

    protected FCSAnalyzer getAnalyzer()
    {
        return FCSAnalyzer.get();
    }

    public void processRun(FlowRun srcRun, ExperimentRunType runElement, File workingDirectory) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    public FlowRun run(File directory) throws Exception
    {
        return importRun(directory);
    }


    public String getRunName(FlowRun srcRun)
    {
        throw new UnsupportedOperationException();
    }
}
