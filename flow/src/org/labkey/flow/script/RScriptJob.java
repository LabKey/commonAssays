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
import org.labkey.flow.FlowModule;
import org.labkey.flow.FlowSettings;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.persist.AnalysisSerializer;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 */
public class RScriptJob extends FlowExperimentJob
{
    private static final String WORKSPACE_PATH_REPLACEMENT = "workspace-path";
    private static final String FCSFILE_DIRECTORY_REPLACEMENT = "fcsfile-directory";
    private static final String OUTPUT_DIRECTORY_REPLACEMENT = "output-directory";
    private static final String RUN_NAME_REPLACEMENT = "run-name";

    private static final String GROUP_NAMES_REPLACEMENT = "group-names";
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
        _workspaceFile = File.createTempFile(_workspaceName, null, FlowSettings.getWorkingDirectory());

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(_workspaceFile));
        oos.writeObject(workspaceData.getWorkspaceObject());
        oos.flush();
        oos.close();
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

        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        Map<String, String> replacements = (Map<String, String>)bindings.get(ExternalScriptEngine.PARAM_REPLACEMENT_MAP);
        if (replacements == null)
            bindings.put(ExternalScriptEngine.PARAM_REPLACEMENT_MAP, replacements = new HashMap<String, String>());
        replacements.put(WORKSPACE_PATH_REPLACEMENT, _workspaceFile.getAbsolutePath()); // ? escape
        replacements.put(FCSFILE_DIRECTORY_REPLACEMENT, _runFilePathRoot.getAbsolutePath()); // ? escape
        replacements.put(OUTPUT_DIRECTORY_REPLACEMENT, workingDir.getAbsolutePath()); // ? escape
        replacements.put(RUN_NAME_REPLACEMENT, _workspaceName); // ? escape
        replacements.put(GROUP_NAMES_REPLACEMENT, _importGroupNames == null ? "" : _importGroupNames.toString());
        replacements.put(NORMALIZATION, String.valueOf(_performNormalization));
        replacements.put(NORM_REFERENCE, _normalizationReference);
        replacements.put(NORM_PARAMTERS, _normalizationParameters);

        String script = getScript();
        String output = (String)engine.eval(script);
        info(output);
    }

    @Override
    protected void doRun() throws Throwable
    {
        File workingDir = createAnalysisDirectory(getExperiment().getName(), FlowProtocolStep.analysis);
        runScript(workingDir);

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

        deleteAnalysisDirectory(workingDir);
    }

    @Override
    public String getDescription()
    {
        return "R Analysis " + getExperiment().getName();
    }
}
