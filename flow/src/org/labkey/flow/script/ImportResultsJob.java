/*
 * Copyright (c) 2010-2011 LabKey Corporation
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

import org.fhcrc.cpas.flow.script.xml.ScriptDocument;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.writer.FileSystemFile;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.AttrObject;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.AttributeSetHelper;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.persist.InputRole;
import org.labkey.flow.persist.ObjectType;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * Imports results from an external flow analysis.
 *
 */
public class ImportResultsJob extends AbstractExternalAnalysisJob
{
    private File _analysisPathRoot = null;
    private String _analysisRunName = null;

    public ImportResultsJob(ViewBackgroundInfo info,
                            PipeRoot root,
                            FlowExperiment experiment,
                            File analysisPathRoot,
                            File originalImportedFile,
                            File runFilePathRoot,
                            String analysisRunName,
                            boolean createKeywordRun,
                            boolean failOnError) throws Exception
    {
        super(info, root, experiment, originalImportedFile, runFilePathRoot, null, createKeywordRun, failOnError);

        _analysisPathRoot = analysisPathRoot;
        if (!_analysisPathRoot.isDirectory())
            throw new IllegalArgumentException("Import root directory doesn't exist");

        _analysisRunName = analysisRunName != null ? analysisRunName : _analysisPathRoot.getName();
    }

    @Override
    public String getDescription()
    {
        return "Import External Analysis " + _analysisRunName;
    }

    private Map<String, AttributeSet> loadAnalysis() throws Exception
    {
        FileSystemFile rootDir = new FileSystemFile(_analysisPathRoot);
        AnalysisSerializer serializer = new AnalysisSerializer(getLogger(), rootDir);
        return serializer.readAnalysis();
    }


    @Override
    protected FlowRun createExperimentRun() throws Exception
    {
        Map<String, AttributeSet> keywordsMap = new LinkedHashMap();
        //Map<String, CompensationMatrix> sampleCompMatrixMap = new LinkedHashMap();
        //Map<CompensationMatrix, AttributeSet> compMatrixMap = new LinkedHashMap();
        Map<String, AttributeSet> resultsMap = new LinkedHashMap();
        //Map<String, Analysis> analysisMap = new LinkedHashMap();
        //Map<Analysis, ScriptDocument> scriptDocs = new HashMap();
        //Map<Analysis, FlowScript> scripts = new HashMap();
        List<String> sampleLabels = new ArrayList<String>();

        Map<String, AttributeSet> analysis = loadAnalysis();

        // UNDONE: only import attrs that match the filter
        SimpleFilter filter = _protocol.getFCSAnalysisFilter();

        // Split analysis into keywords AttributeSet and stats/graphs AttributeSet. CONSIDER: change loadAnalysis() signature.
        for (Map.Entry<String, AttributeSet> entry : analysis.entrySet())
        {
            String sampleLabel = entry.getKey();
            sampleLabels.add(sampleLabel);

            AttributeSet attrs = entry.getValue();

            AttributeSet keywordAttrs = new AttributeSet(ObjectType.fcsKeywords, null);
            // UNDONE: set URI if runFilePathRoot/sample.fcs exists
            keywordAttrs.setKeywords(attrs.getKeywords());
            keywordAttrs.setKeywordAliases(attrs.getKeywordAliases());
            keywordsMap.put(sampleLabel, keywordAttrs);
            AttributeSetHelper.prepareForSave(keywordAttrs, getContainer());

            AttributeSet resultsAttrs = new AttributeSet(ObjectType.fcsAnalysis, null);
            resultsAttrs.setStatistics(attrs.getStatistics());
            resultsAttrs.setStatisticAliases(attrs.getStatisticAliases());
            resultsAttrs.setGraphs(attrs.getGraphs());
            resultsAttrs.setGraphAliases(attrs.getGraphAliases());
            resultsMap.put(sampleLabel, resultsAttrs);
            AttributeSetHelper.prepareForSave(resultsAttrs, getContainer());

            // UNDONE: comp matrix
        }

        File statisticsFile = new File(_analysisPathRoot, AnalysisSerializer.STATISTICS_FILENAME);

        return saveAnalysis(this, getUser(), getContainer(), getExperiment(),
                _analysisRunName, statisticsFile, getOriginalImportedFile(),
                getRunFilePathRoot(),
                keywordsMap,
                Collections.<String, CompensationMatrix>emptyMap(),
                Collections.<CompensationMatrix, AttributeSet>emptyMap(),
                resultsMap,
                Collections.<String, Analysis>emptyMap(),
                Collections.<Analysis, ScriptDocument>emptyMap(),
                Collections.<Analysis, FlowScript>emptyMap(),
                sampleLabels);
    }

    @Override
    protected ExpData createExternalAnalysisData(ExperimentService.Interface svc,
                                                 ExpRun externalAnalysisRun,
                                                 User user, Container container,
                                                 String analysisName,
                                                 File externalAnalysisFile,
                                                 File originalImportedFile) throws SQLException
    {
        addStatus("Saving External Analysis " + originalImportedFile.getName());
        ExpData data = svc.createData(container, new DataType("Flow-ExternalAnalysis"));
        data.setDataFileURI(originalImportedFile.toURI());
        data.setName(analysisName);
        data.save(user);

        ExpProtocolApplication startingInputs = externalAnalysisRun.addProtocolApplication(user, null, ExpProtocol.ApplicationType.ExperimentRun, "Starting inputs");
        startingInputs.addDataInput(user, data, InputRole.ExternalAnalysis.toString());

        return data;
    }

}
