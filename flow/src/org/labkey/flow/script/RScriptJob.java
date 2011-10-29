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

import org.apache.commons.io.IOUtils;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.reports.ExternalScriptEngine;
import org.labkey.api.resource.Resource;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.Path;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.writer.FileSystemFile;
import org.labkey.flow.FlowModule;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.FlowJoWorkspace;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.persist.AnalysisSerializer;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 */
public class RScriptJob extends FlowExperimentJob
{
    private static final String WORKSPACE_PATH = "workspace-path";
    private static final String FCSFILE_DIRECTORY = "fcsfile-directory";
    private static final String OUTPUT_DIRECTORY = "output-directory";
    private static final String RUN_NAME = "run-name";

    private static final String GROUP_NAMES = "group-names";
    private static final String NORMALIZATION = "perform-normalization";
    private static final String NORM_REFERENCE = "normalization-reference";
    private static final String NORM_PARAMTERS = "normalization-parameters";

    private final FlowExperiment _experiment;
    private final File _workspaceFile;
    private final String _workspaceName;
    private final File _originalImportedFile;
    private final File _runFilePathRoot;
    private final List<String> _importGroupNames;
    private final boolean _performNormalization;
    private final String _normalizationReference;
    private final String _normalizationParameters;
    private final boolean _createKeywordRun;
    private final boolean _failOnError;

    public RScriptJob(ViewBackgroundInfo info,
                      PipeRoot root,
                      FlowExperiment experiment,
                      WorkspaceData workspaceData,
                      File originalImportedFile,
                      File runFilePathRoot,
                      List<String> importGroupNames,
                      boolean performNormalization,
                      String normalizationReference,
                      String normalizationParameters,
                      boolean createKeywordRun,
                      boolean failOnError) throws Exception
    {
        super(info, root, experiment.getLSID(), FlowProtocol.ensureForContainer(info.getUser(), info.getContainer()), experiment.getName(), FlowProtocolStep.analysis);
        _experiment = experiment;
        _originalImportedFile = originalImportedFile;
        _runFilePathRoot = runFilePathRoot;
        _importGroupNames = importGroupNames;
        _performNormalization = performNormalization;
        _normalizationReference = normalizationReference;
        _normalizationParameters = normalizationParameters;
        _createKeywordRun = createKeywordRun;
        _failOnError = failOnError;
        assert !_createKeywordRun || _runFilePathRoot != null;

        if (workspaceData.getPath() == null || originalImportedFile == null)
            throw new IllegalArgumentException("External R analysis requires workspace file from pipeline");

        String name = workspaceData.getName();
        if (name == null && workspaceData.getPath() != null)
        {
            String[] parts = workspaceData.getPath().split(File.pathSeparator);
            if (parts.length > 0)
                name = parts[parts.length];
        }
        if (name == null)
            name = "workspace";
        _workspaceName = name;

        // NOTE: may need to copy workspace file for clustered jobs
        _workspaceFile = originalImportedFile;
    }

    @Override
    public FlowExperiment getExperiment()
    {
        return _experiment;
    }

    private String getScript() throws IOException
    {
        Module flowModule = ModuleLoader.getInstance().getModule(FlowModule.NAME);
        String reportPath = "META-INF";
        Resource r = flowModule.getModuleResource(new Path(reportPath, "ranalysis.r"));
        InputStream is = null;
        try
        {
            is = r.getInputStream();
            return IOUtils.toString(is);
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }
    }

    private void runScript(File workingDir) throws IOException, ScriptException
    {
        ScriptEngine engine = ServiceRegistry.get().getService(ScriptEngineManager.class).getEngineByExtension("r");
        if (engine == null)
        {
            error("The R script engine is not available.  Please configure the R script engine in the admin console.");
            return;
        }

        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        Map<String, String> replacements = (Map<String, String>)bindings.get(ExternalScriptEngine.PARAM_REPLACEMENT_MAP);
        if (replacements == null)
            bindings.put(ExternalScriptEngine.PARAM_REPLACEMENT_MAP, replacements = new HashMap<String, String>());

        replacements.put(WORKSPACE_PATH, _workspaceFile.getAbsolutePath().replaceAll("\\\\", "/"));
        replacements.put(FCSFILE_DIRECTORY, _runFilePathRoot.getAbsolutePath().replaceAll("\\\\", "/"));
        replacements.put(OUTPUT_DIRECTORY, workingDir.getAbsolutePath().replaceAll("\\\\", "/"));
        replacements.put(RUN_NAME, _workspaceName);
        replacements.put(GROUP_NAMES, _importGroupNames != null && _importGroupNames.size() > 0 ? _importGroupNames.get(0) : "");
        replacements.put(NORMALIZATION, String.valueOf(_performNormalization));
        replacements.put(NORM_REFERENCE, _normalizationReference == null ? "" : _normalizationReference);
        replacements.put(NORM_PARAMTERS, _normalizationParameters == null ? "" : _normalizationParameters);

        String script = getScript();
        String output = (String)engine.eval(script);
        info(output);
    }

    private void writeCompensation(File workingDir) throws Exception
    {
        info("Writing compensation matrices...");
        FlowJoWorkspace workspace = FlowJoWorkspace.readWorkspace(new FileInputStream(_workspaceFile));
        Map<String, CompensationMatrix> matrices = new HashMap<String, CompensationMatrix>();
        for (FlowJoWorkspace.SampleInfo sampleInfo : workspace.getSamples())
        {
            CompensationMatrix matrix = sampleInfo.getCompensationMatrix();
            if (matrix == null)
                continue;

            matrices.put(sampleInfo.getLabel(), matrix);
        }

        FileSystemFile rootDir = new FileSystemFile(workingDir);
        AnalysisSerializer writer = new AnalysisSerializer(_logger, rootDir);
        writer.writeAnalysis(null, null, matrices);
    }

    @Override
    protected void doRun() throws Throwable
    {
        File workingDir = createAnalysisDirectory(getExperiment().getName(), FlowProtocolStep.analysis);
        runScript(workingDir);

        if (!hasErrors())
            writeCompensation(workingDir);

        if (!hasErrors())
        {
            ImportResultsJob importJob = new ImportResultsJob(getInfo(), getPipeRoot(), getExperiment(),
                    workingDir, _originalImportedFile, _runFilePathRoot,
                    _workspaceName, _createKeywordRun, _failOnError);
            importJob.setLogFile(getLogFile());
            importJob.setLogLevel(getLogLevel());
            importJob.setSubmitted();

            try
            {
                importJob.doRun();
                if (importJob.hasErrors())
                {
                    getLogger().error("Failed to import results from R analysis.");
                    setStatus(PipelineJob.ERROR_STATUS);
                }
            }
            catch (Exception e)
            {
                error("Import failed to complete", e);
            }
        }

        deleteAnalysisDirectory(workingDir);
    }

    @Override
    public String getDescription()
    {
        return "R Analysis " + getExperiment().getName();
    }
}
