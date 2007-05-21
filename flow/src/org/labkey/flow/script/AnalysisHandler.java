package org.labkey.flow.script;

import org.labkey.flow.data.*;
import org.labkey.flow.data.FlowDataType;
import org.fhcrc.cpas.flow.script.xml.AnalysisDef;
import org.fhcrc.cpas.flow.script.xml.SettingsDef;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.ObjectType;
import org.labkey.flow.persist.FlowDataHandler;
import org.fhcrc.cpas.exp.xml.*;
import org.labkey.api.util.URIUtil;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Table;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.query.QueryService;
import org.w3c.dom.Element;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import java.net.URI;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.io.File;

import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.SampleCriteria;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.FCSRef;
import org.labkey.flow.query.FlowSchema;

public class AnalysisHandler extends BaseHandler
{
    AnalysisDef _analysis;
    Analysis _groupAnalysis;
    SampleCriteria _sampleCriteria;
    int _wellIndex;

    public AnalysisHandler(ScriptJob job, SettingsDef settings, AnalysisDef analysis) throws Exception
    {
        super(job, FlowProtocolStep.analysis);
        _analysis = analysis;
        _groupAnalysis = FlowAnalyzer.makeAnalysis(settings, _analysis);
        _sampleCriteria = SampleCriteria.readChildCriteria((Element) _analysis.getDomNode());
    }

    public boolean checkProcessWell(FCSRef ref) throws Exception
    {
        return FCSAnalyzer.get().matchesCriteria(_sampleCriteria, ref);
    }

    synchronized public DataBaseType addWell(ExperimentRunType runElement, FlowWell src, FlowCompensationMatrix flowComp) throws SQLException
    {
        ProtocolApplicationBaseType app = addProtocolApplication(runElement);
        DataBaseType ret = duplicateWell(app, src, FlowDataType.FCSAnalysis);
        ret.setName(_job.getProtocol().getFCSAnalysisName(src));
        if (flowComp != null)
        {
            _job.addInput(app, flowComp, InputRole.CompensationMatrix);
        }
        _job.addRunOutput(ret.getAbout(), null);
        return ret;
    }

    public void processRun(FlowRun run, ExperimentRunType runElement, File workingDirectory) throws Exception
    {
        FlowCompensationMatrix flowComp = _job.findCompensationMatrix(run);
        CompensationMatrix comp = null;
        if (flowComp != null)
        {
            comp = flowComp.getCompensationMatrix();
        }
        if (comp == null && _groupAnalysis.requiresCompensationMatrix())
        {
            _job.addError(null, null, "No compensation matrix found.");
            return;
        }
        FlowSchema schema = new FlowSchema(_job.getUser(), getContainer());
        schema.setRun(run);
        TableInfo tblFCSFiles = schema.createFCSFileTable("FCSFiles");
        ColumnInfo colRowId = tblFCSFiles.getColumn("RowId");
        try
        {
            FlowWell[] wells = run.getWellsToBeAnalyzed(_job.getProtocol());
            if (wells.length == 0)
            {
                FlowWell[] allWells = run.getWells();
                if (allWells.length == 0)
                {
                    _job.addStatus("This run contains no FCS files");
                }
                else
                {
                    _job.addStatus("This run contains FCS files but they are all excluded by the Protocol's FCS Analysis Filter");
                }
                return;
            }
            _wellIndex = 0;
            Runnable[] tasks = new Runnable[wells.length];
            for (int iWell = 0; iWell < wells.length; iWell ++)
            {
                Runnable task = new AnalyzeTask(workingDirectory, run, runElement, wells[iWell], wells.length, flowComp, comp);
                tasks[iWell] = task;
            }
            FlowThreadPool.runTaskSet(new FlowTaskSet(tasks));
        }
        catch (SQLException e)
        {
            _job.addStatus("An exception occurred: " + e);
        }
    }

    synchronized int getNextWellIndex()
    {
        return ++_wellIndex;
    }

    private class AnalyzeTask implements Runnable
    {
        File _workingDirectory;
        FlowRun _run;
        FlowWell _well;
        int _wellCount;
        CompensationMatrix _comp;
        FlowCompensationMatrix _flowComp;
        ExperimentRunType _runElement;
        AnalyzeTask(File workingDirectory, FlowRun run, ExperimentRunType runElement, FlowWell well, int wellCount, FlowCompensationMatrix flowComp, CompensationMatrix comp)
        {
            _workingDirectory = workingDirectory;
            _run = run;
            _runElement = runElement;
            _well = well;
            _wellCount = wellCount;
            _flowComp = flowComp;
            _comp = comp;
        }
        public void run()
        {
            try
            {
                int iWell = getNextWellIndex();
                if (_job.checkInterrupted())
                    return;
                FCSRef ref = FlowAnalyzer.getFCSRef(_well);
                URI uri = ref.getURI();
                if (!checkProcessWell(ref))
                    return;

                String description = "well " + iWell + "/" + _wellCount + ":" + _run.getName() + ":" + _well.getName();
                _job.addStatus("Starting " + description);
                Set<GraphSpec> graphs = new LinkedHashSet(_groupAnalysis.getGraphs());
                List<FCSAnalyzer.StatResult> stats = FCSAnalyzer.get().calculateStatistics(uri, _comp, _groupAnalysis);
                DataBaseType dbt = addWell(_runElement, _well, _flowComp);
                AttributeSet attrs = new AttributeSet(ObjectType.fcsAnalysis, uri);
                addResults(dbt, attrs, stats);
                List<FCSAnalyzer.GraphResult> graphResults = FCSAnalyzer.get().generateGraphs(_well.getFCSURI(), _comp, _groupAnalysis, graphs);
                addResults(dbt, attrs, graphResults);
                attrs.save(_job.decideFileName(_workingDirectory, URIUtil.getFilename(uri), FlowDataHandler.EXT_DATA), dbt);
                _job.addStatus("Completed " + description);
            }
            catch (Throwable t)
            {
                _job.handleException(t);
            }

        }
    }
}
