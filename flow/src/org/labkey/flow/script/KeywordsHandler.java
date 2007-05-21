package org.labkey.flow.script;

import org.labkey.flow.data.*;
import org.labkey.flow.data.FlowDataType;
import org.fhcrc.cpas.flow.script.xml.*;
import org.labkey.flow.persist.FlowDataHandler;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.api.util.URIUtil;
import org.fhcrc.cpas.exp.xml.*;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpMaterial;

import java.util.*;
import java.util.regex.Pattern;
import java.io.File;

import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.model.FCSKeywordData;

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

    protected void addRun(File directory, List<FCSKeywordData> data) throws Exception
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
            well.setSourceProtocolLSID(_step.getLSID(getContainer()));
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
    }

    protected void importRun(File directory) throws Exception
    {
        File[] files = directory.listFiles();
        List<FCSKeywordData> lstFileData = new ArrayList();

        for (int i = 0; i < files.length; i ++)
        {
            File file = files[i];
            if (!isFCSFile(file))
                continue;
            addStatus("Reading file " + file.getName());
            lstFileData.add( getAnalyzer().readAllKeywords(file.toURI()));
        }
        if (lstFileData.size() == 0)
        {
            addStatus("No FCS files found");
            return;
        }
        addRun(directory, lstFileData);
    }

    protected FCSAnalyzer getAnalyzer()
    {
        return FCSAnalyzer.get();
    }

    public void processRun(FlowRun srcRun, ExperimentRunType runElement, File workingDirectory) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    public void run(File directory) throws Exception
    {
        importRun(directory);
    }
}
