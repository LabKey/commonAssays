package org.fhcrc.cpas.flow.script;

import org.fhcrc.cpas.flow.data.*;
import org.fhcrc.cpas.flow.data.FlowDataType;
import org.fhcrc.cpas.flow.script.xml.AnalysisDef;
import org.fhcrc.cpas.flow.persist.AttributeSet;
import org.fhcrc.cpas.flow.persist.ObjectType;
import org.fhcrc.cpas.flow.persist.FlowDataHandler;
import org.fhcrc.cpas.exp.xml.*;
import org.labkey.api.util.URIUtil;
import org.w3c.dom.Element;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.net.URI;
import java.sql.SQLException;
import java.io.File;

import com.labkey.flow.model.CompensationMatrix;
import com.labkey.flow.model.Analysis;
import com.labkey.flow.model.SampleCriteria;
import com.labkey.flow.web.FCSAnalyzer;
import com.labkey.flow.web.StatisticSpec;
import com.labkey.flow.web.GraphSpec;
import com.labkey.flow.web.FCSRef;

public class AnalysisHandler extends BaseHandler
{
    AnalysisDef _analysis;
    Analysis _groupAnalysis;
    SampleCriteria _sampleCriteria;

    public AnalysisHandler(ScriptJob job, AnalysisDef analysis) throws Exception
    {
        super(job, FlowProtocolStep.analysis);
        _analysis = analysis;
        _groupAnalysis = FlowAnalyzer.makeAnalysis(_analysis);
        _sampleCriteria = SampleCriteria.readChildCriteria((Element) _analysis.getDomNode());
    }

    public boolean checkProcessWell(FCSRef ref) throws Exception
    {
        return FCSAnalyzer.get().matchesCriteria(_sampleCriteria, ref);
    }

    public DataBaseType addWell(ExperimentRunType runElement, FlowWell src, FlowCompensationMatrix flowComp) throws SQLException
    {
        ProtocolApplicationBaseType app = addProtocolApplication(runElement);
        DataBaseType ret = duplicateWell(app, src, FlowDataType.FCSAnalysis);
        if (flowComp != null)
        {
            _job.addInput(app, flowComp, InputRole.CompensationMatrix);
        }
        _job.addRunOutput(ret.getAbout());
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
        FlowWell[] wells = run.getWells();
        Set<StatisticSpec> statistics = new LinkedHashSet(_groupAnalysis.getStatistics());
        Set<GraphSpec> graphs = new LinkedHashSet(_groupAnalysis.getGraphs());

        for (int iWell = 0; iWell < wells.length; iWell ++)
        {
            FlowWell well = wells[iWell];
            if (_job.checkInterrupted())
            {
                throw new InterruptedException("User interrupted");
            }
            FCSRef ref = FlowAnalyzer.getFCSRef(well);
            URI uri = ref.getURI();
            if (!checkProcessWell(ref))
                continue;
            _job.addStatus("Processing well " + (iWell + 1) + "/" + wells.length + ":" + run.getName() + ":" + well.getName());


            List<FCSAnalyzer.StatResult> stats = FCSAnalyzer.get().calculateStatistics(uri, comp, _groupAnalysis, statistics);
            DataBaseType dbt = addWell(runElement, well, flowComp);
            AttributeSet attrs = new AttributeSet(ObjectType.fcsAnalysis, uri);
            addResults(dbt, attrs, stats);
            List<FCSAnalyzer.GraphResult> graphResults = FCSAnalyzer.get().generateGraphs(well.getFCSURI(), comp, _groupAnalysis, graphs);
            addResults(dbt, attrs, graphResults);
            attrs.save(_job.decideFileName(workingDirectory, URIUtil.getFilename(uri), FlowDataHandler.EXT_DATA), dbt);
        }
    }
}
