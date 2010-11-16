package org.labkey.flow.script;

import org.labkey.api.exp.api.*;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.ObjectType;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: kevink
 */
public class ImportJob extends FlowExperimentJob
{
    private File _summaryStats = null;
    private FlowExperiment _experiment = null;
    private FlowRun _keywordsRun = null;
    private String _runName = null;

    public ImportJob(ViewBackgroundInfo info, PipeRoot root, FlowExperiment experiment, FlowProtocol protocol, FlowRun keywordsRun, File summaryStats) throws Exception
    {
        super(info, root, experiment.getLSID(), protocol, experiment.getName(), FlowProtocolStep.analysis);
        _experiment = experiment;
        _keywordsRun = keywordsRun;
        _summaryStats = summaryStats;
        _runName = _summaryStats.getParentFile().getName();
    }

    @Override
    public FlowExperiment getExperiment()
    {
        return _experiment;
    }

    // Read stats results summaryStats.txt file.
    private Map<String, AttributeSet> loadAnalysis() throws Exception
    {
        TabLoader loader = new TabLoader(_summaryStats, true);

        Map<String, AttributeSet> results = new LinkedHashMap<String, AttributeSet>();
        for (Map<String, Object> row : loader)
        {
            String sample = (String)row.get(ResultColumn.sample.name());
            AttributeSet attrs = results.get(sample);
            if (attrs == null)
            {
                attrs = new AttributeSet(ObjectType.fcsAnalysis, null);
                results.put(sample, attrs);
            }

            String population = (String)row.get(ResultColumn.population.name());
            SubsetSpec subset = SubsetSpec.fromString(population);

            Integer count = (Integer)row.get(ResultColumn.count.name());
            StatisticSpec countStat = new StatisticSpec(subset, StatisticSpec.STAT.Count, null);
            attrs.setStatistic(countStat, count.doubleValue());

            Double percent = (Double)row.get(ResultColumn.percent.name());
            StatisticSpec percentStat = new StatisticSpec(subset, StatisticSpec.STAT.Frequency, null);
            attrs.setStatistic(percentStat, percent);

            // XXX: add graphs

            attrs.prepareForSave(getContainer());
        }

        return results;
    }

    private FlowRun createRun(ExperimentService.Interface svc, Map<String, AttributeSet> analysisMap)
            throws Exception
    {
        // Fake file URI set on the FCSFile/FCSAnalsyis ExpData to ensure it's recognized by the FlowDataHandler.
        URI dataFileURI = new File(_summaryStats.getParentFile(), "attributes.flowdata.xml").toURI();

        // Create experiment run
        ExpRun run = svc.createExperimentRun(getContainer(), _runName);
        FlowProtocol flowProtocol = FlowProtocol.ensureForContainer(getUser(), getContainer());
        ExpProtocol protocol = flowProtocol.getProtocol();
        run.setProtocol(protocol);
        run.setFilePathRoot(_summaryStats.getParentFile());
        run.save(getUser());

        // CONSIDER: Create experiment data for summaryStats.txt file
        //ExpData summaryStats = svc.createData(getContainer(), new DataType("RFlow"));
        //summaryStats.setDataFileURI(_summaryStats.toURI());
        //summaryStats.setName(_summaryStats.getName());
        //summaryStats.save(getUser());


        FlowFCSFile[] fcsFiles = _keywordsRun.getFCSFiles();
        Map<String, FlowFCSFile> fcsFileMap = new LinkedHashMap<String, FlowFCSFile>();
        for (FlowFCSFile fcsFile : fcsFiles)
            fcsFileMap.put(fcsFile.getName(), fcsFile);

        // Add analysis to experiment run
        int iAnalysis = 0;
        for (Map.Entry<String, AttributeSet> entry : analysisMap.entrySet())
        {
            iAnalysis++;
            if (checkInterrupted())
                return null;

            String name = entry.getKey();
            FlowFCSFile fcsFile = fcsFileMap.get(name);
            AttributeSet results = entry.getValue();
            if (fcsFile != null && results != null)
            {
                ExpProtocolApplication paAnalysis = run.addProtocolApplication(getUser(),
                        FlowProtocolStep.analysis.getAction(protocol), ExpProtocol.ApplicationType.ProtocolApplication, null);
                results.setURI(fcsFile.getFCSURI());
                paAnalysis.addDataInput(getUser(), fcsFile.getData(), InputRole.FCSFile.toString());

                ExpData fcsAnalysis = svc.createData(getContainer(), FlowDataType.FCSAnalysis);
                fcsAnalysis.setName(flowProtocol.getFCSAnalysisName(fcsFile));
                fcsAnalysis.setSourceApplication(paAnalysis);
                fcsAnalysis.setDataFileURI(dataFileURI);

                addStatus("Saving FCSAnalysis " + iAnalysis + "/" + analysisMap.size() + ":" + fcsAnalysis.getName());
                fcsAnalysis.save(getUser());
                results.doSave(getUser(), fcsAnalysis);
            }
        }

        _experiment.getExperiment().addRuns(getUser(), run);

        FlowManager.get().updateFlowObjectCols(getContainer());

        return new FlowRun(run);
    }

    @Override
    protected void doRun() throws Throwable
    {
        FlowManager.vacuum();

        ExperimentService.Interface svc = ExperimentService.get();

        Map<String, AttributeSet> analysisMap = loadAnalysis();

        boolean transaction = false;
        try
        {
            svc.beginTransaction();
            transaction = true;

            FlowRun run = createRun(svc, analysisMap);

            svc.commitTransaction();
            transaction = false;

            addStatus("Import completed");
        }
        catch (Exception e)
        {
            if (transaction)
            {
                svc.rollbackTransaction();
                error("Import failed to complete", e);
                addError(null, null, e.getMessage());
            }
            FlowManager.analyze();
        }
    }

    @Override
    public String getDescription()
    {
        return "Import " + getExperiment().getName();
    }

    private enum ResultColumn
    {
        sample,
        population,
        percent,
        count
    }

    private static class SampleInfo
    {
        String label;
    }
}
