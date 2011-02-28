/*
 * Copyright (c) 2010 LabKey Corporation
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

import org.apache.commons.io.IOUtils;
import org.labkey.api.attachments.AttachmentFile;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.attachments.FileAttachmentFile;
import org.labkey.api.exp.api.*;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.ObjectType;

import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * User: kevink
 */
public class RImportJob extends FlowExperimentJob
{
    public static final String SUMMARY_STATS_FILENAME = "summaryStats.txt";
    
    private File _summaryStats = null;
    private FlowExperiment _experiment = null;
    private FlowRun _keywordsRun = null;
    private String _analysisRunName = null;

    public RImportJob(ViewBackgroundInfo info, PipeRoot root, FlowExperiment experiment, FlowProtocol protocol, FlowRun keywordsRun, File summaryStats, String analysisRunName) throws Exception
    {
        super(info, root, experiment.getLSID(), protocol, experiment.getName(), FlowProtocolStep.analysis);
        _experiment = experiment;
        _keywordsRun = keywordsRun;
        _summaryStats = summaryStats;
        _analysisRunName = analysisRunName != null ? analysisRunName : _summaryStats.getParentFile().getName();
    }

    @Override
    public FlowExperiment getExperiment()
    {
        return _experiment;
    }

    // Read stats results summaryStats.txt file.
    private Map<String, AttributeSet> loadAnalysis() throws Exception
    {
        File dir = _summaryStats.getParentFile();
        String[] imageNames = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".png");
            }
        });
        
        TabLoader loader = new TabLoader(_summaryStats, true);

        Map<String, AttributeSet> results = new LinkedHashMap<String, AttributeSet>();
        for (Map<String, Object> row : loader)
        {
            final String sample = (String)row.get(ResultColumn.sample.name());
            AttributeSet attrs = results.get(sample);
            if (attrs == null)
            {
                attrs = new AttributeSet(ObjectType.fcsAnalysis, null);
                results.put(sample, attrs);
            }

            String statPopulation = (String)row.get(ResultColumn.population.name());
            SubsetSpec subset = SubsetSpec.fromString(statPopulation);

            Integer count = (Integer)row.get(ResultColumn.count.name());
            StatisticSpec countStat = new StatisticSpec(subset, StatisticSpec.STAT.Count, null);
            attrs.setStatistic(countStat, count.doubleValue());

            Double percent = (Double)row.get(ResultColumn.percent.name());
            StatisticSpec percentStat = new StatisticSpec(subset, StatisticSpec.STAT.Frequency, null);
            attrs.setStatistic(percentStat, percent);

            // Collect graph images.
            for (String imageName : imageNames)
            {
                if (!imageName.startsWith(sample))
                    continue;

                String[] parts = imageName.substring(sample.length()+1).split("_");
                GraphSpec graphSpec;
                if (parts.length == 3)
                {
                    String xaxis = parts[0];
                    String yaxis = parts[1];
                    String graphPopulation = parts[2];
                    SubsetSpec subsetSpec = SubsetSpec.fromString(graphPopulation);
                    graphSpec = new GraphSpec(subsetSpec, xaxis, yaxis);
                }
                else if (parts.length == 2)
                {
                    String xaxis = parts[0];
                    String graphPopulation = parts[1];
                    SubsetSpec subsetSpec = SubsetSpec.fromString(graphPopulation);
                    graphSpec = new GraphSpec(subsetSpec, xaxis);
                }
                else
                {
                    addStatus(String.format("Skipping graph '%s'; Expected image to be named: %s_<x-axis>_<y-axis>_<population>.png or %s_<x-axis>_<population>.png", imageName, sample, sample));
                    continue;
                }

                InputStream is = new FileInputStream(new File(dir, imageName));
                byte[] b = IOUtils.toByteArray(is);
                is.close();
                attrs.setGraph(graphSpec, b);
            }

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
        ExpRun run = svc.createExperimentRun(getContainer(), _analysisRunName);
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


        // Add run level graphs as attachments to the run
        FlowRun flowRun = new FlowRun(run);

        // XXX: sort BeforeNorm, AfterNorm images for each parameter
        File[] runImages = _summaryStats.getParentFile().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name)
            {
                return name.startsWith(_analysisRunName) && name.endsWith(".png");
            }
        });

        AttachmentService.Service att = AttachmentService.get();
        List<AttachmentFile> attachments = new LinkedList<AttachmentFile>();
        for (File runImage : runImages)
            attachments.add(new FileAttachmentFile(runImage));
        att.addAttachments(flowRun, attachments, getUser());

        return flowRun;
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
                addError(null, null, e.toString());
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
}
