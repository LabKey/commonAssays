/*
 * Copyright (c) 2010-2017 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.reports.ExternalScriptEngine;
import org.labkey.api.resource.Resource;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Path;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.writer.FileSystemFile;
import org.labkey.flow.FlowModule;
import org.labkey.flow.analysis.model.CompensationMatrix;
import org.labkey.flow.analysis.model.SampleIdMap;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.controllers.WorkspaceData;
import org.labkey.flow.controllers.executescript.AnalysisEngine;
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowFCSFile;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
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
    private static final String R_ANALYSIS_DIRECTORY = "r-analysis-directory";
    private static final String NORMALIZED_DIRECTORY = "normalized-directory";
    private static final String RUN_NAME = "run-name";

    private static final String GROUP_NAMES = "group-names";
    private static final String NORMALIZATION = "perform-normalization";
    private static final String NORM_REFERENCE = "normalization-reference";
    private static final String NORM_SUBSETS = "normalization-subsets";
    private static final String NORM_PARAMETERS = "normalization-parameters";

    private final FlowExperiment _experiment;
    private final File _workspaceFile;
    private final String _workspaceName;
    private final File _originalImportedFile;
    private final File _runFilePathRoot;
    private final List<File> _keywordDirs;
    // Map workspace sample id -> FlowFCSFile (or null if we aren't resolving previously imported FCS files)
    private final Map<String, FlowFCSFile> _selectedFCSFiles;
    private final List<String> _importGroupNames;
    private final boolean _performNormalization;
    private final String _normalizationReference;
    private final List<String> _normalizationSubsets;
    private final List<String> _normalizationParameters;

    private final Container _targetStudy;

    private final boolean _failOnError;

    public RScriptJob(ViewBackgroundInfo info,
                      PipeRoot root,
                      FlowExperiment experiment,
                      WorkspaceData workspaceData,
                      File originalImportedFile,
                      File runFilePathRoot,
                      List<File> keywordDirs,
                      Map<String, FlowFCSFile> selectedFCSFiles,
                      // UNDONE: Remove importGroupNames and use selectedFCSFiles instead.
                      List<String> importGroupNames,
                      boolean performNormalization,
                      String normalizationReference,
                      List<String> normalizationSubsets,
                      List<String> normalizationParameters,
                      Container targetStudy,
                      boolean failOnError) throws Exception
    {
        super(info, root, experiment.getLSID(), FlowProtocol.ensureForContainer(info.getUser(), info.getContainer()), experiment.getName(), FlowProtocolStep.analysis);
        _experiment = experiment;
        _originalImportedFile = originalImportedFile;
        _runFilePathRoot = runFilePathRoot;
        _keywordDirs = keywordDirs;
        _selectedFCSFiles = selectedFCSFiles;
        _importGroupNames = importGroupNames;
        _performNormalization = performNormalization;
        _normalizationReference = normalizationReference;
        _targetStudy = targetStudy;
        _failOnError = failOnError;

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
        _normalizationSubsets = normalizationSubsets;
        _normalizationParameters = normalizationParameters;
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

        try (InputStream is = r.getInputStream())
        {
            return PageFlowUtil.getStreamContentsAsString(is);
        }
    }

    private void runScript(File rAnalysisDir, File normalizedDir) throws IOException
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
            bindings.put(ExternalScriptEngine.PARAM_REPLACEMENT_MAP, replacements = new HashMap<>());

        replacements.put(WORKSPACE_PATH, _workspaceFile.getAbsolutePath().replaceAll("\\\\", "/"));
        replacements.put(FCSFILE_DIRECTORY, _runFilePathRoot.getAbsolutePath().replaceAll("\\\\", "/"));
        // UNDONE: Add _keywordDirs replacement parameter, remove _runFilePathRoot replacement ?
        // UNDONE: Add _selectedFCSFiles replacement parameter
        replacements.put(R_ANALYSIS_DIRECTORY, rAnalysisDir.getAbsolutePath().replaceAll("\\\\", "/"));
        replacements.put(NORMALIZED_DIRECTORY, normalizedDir.getAbsolutePath().replaceAll("\\\\", "/"));
        replacements.put(RUN_NAME, _workspaceName);
        replacements.put(GROUP_NAMES, _importGroupNames != null && _importGroupNames.size() > 0 ? _importGroupNames.get(0) : "");
        replacements.put(NORMALIZATION, _performNormalization ? "TRUE" : "FALSE");
        replacements.put(NORM_REFERENCE, _normalizationReference == null ? "" : _normalizationReference);

        String sep = "";
        StringBuilder normSubsets = new StringBuilder("c(");
        for (String subset : _normalizationSubsets)
        {
            normSubsets.append(sep);
            normSubsets.append("\"").append(subset).append("\"");
            sep = ", ";
        }
        normSubsets.append(")");
        replacements.put(NORM_SUBSETS, normSubsets.toString());

        sep = "";
        StringBuilder normParams = new StringBuilder("c(");
        for (String param : _normalizationParameters)
        {
            normParams.append(sep);
            normParams.append("\"").append(param).append("\"");
            sep = ", ";
        }
        normParams.append(")");
        replacements.put(NORM_PARAMETERS, normParams.toString());

        // UNDONE: add protocol filter to script
        SimpleFilter filter = _protocol.getFCSAnalysisFilter();

        String script = getScript();
        ScriptContext context = engine.getContext();

        try (Writer writer = new FileWriter(getLogFile()))
        {
            context.setWriter(writer);
            String output = (String) engine.eval(script, context);
            info(output);
        }
        catch (ScriptException e)
        {
            error("Error running R script", e);
        }
    }

    private void writeCompensation(File workingDir) throws Exception
    {
        info("Writing compensation matrices...");
        Workspace workspace = Workspace.readWorkspace(new FileInputStream(_workspaceFile));
        SampleIdMap<CompensationMatrix> matrices = new SampleIdMap<>();
        for (Workspace.SampleInfo sampleInfo : workspace.getSamples())
        {
            CompensationMatrix matrix = null;
            if (!sampleInfo.isPrecompensated())
                matrix = sampleInfo.getCompensationMatrix();
            if (matrix == null)
                continue;

            matrices.put(sampleInfo.getSampleId(), sampleInfo.getLabel(), matrix);
        }

        FileSystemFile rootDir = new FileSystemFile(workingDir);
        AnalysisSerializer writer = new AnalysisSerializer(_logger, rootDir);
        writer.writeAnalysis(null, null, matrices);
        if (!hasErrors())
            info("Wrote compensation matrices.");
    }

    private void importResults(File dir, String analysisRunName) throws Throwable
    {
        if (hasErrors())
            return;

        ImportResultsJob importJob = new ImportResultsJob(
                getInfo(),
                getPipeRoot(),
                getExperiment(),
                AnalysisEngine.R,
                dir,
                _originalImportedFile,
                _runFilePathRoot,
                _keywordDirs,
                _selectedFCSFiles,
                analysisRunName,
                _targetStudy,
                _failOnError);
        importJob.setLogFile(getLogFile());
        importJob.setLogLevel(getLogLevel());
        importJob.setSubmitted();

        try
        {
            info("Importing results from '" + dir + "'");
            importJob.doRun();
            if (importJob.hasErrors())
            {
                getLogger().error("Failed to import results from R analysis '" + dir + "'.");
                setStatus(TaskStatus.error);
            }
            else
            {
                info("Finished importing results from '" + dir + "'");
            }
        }
        catch (Exception e)
        {
            error("Import failed to complete", e);
        }
    }

    @Override
    protected void doRun() throws Throwable
    {
        File workingDir = createAnalysisDirectory(getExperiment().getName(), FlowProtocolStep.analysis);
        File rAnalysisDir = new File(workingDir, "rAnalysis");
        if (!rAnalysisDir.mkdirs())
            throw new IOException("Could not create analysis directory: " + rAnalysisDir.getAbsolutePath());

        File normalizedDir = new File(workingDir, "normalized");
        if (_performNormalization && !normalizedDir.mkdirs())
            throw new IOException("Could not create normalization directory: " + normalizedDir.getAbsolutePath());

        runScript(rAnalysisDir, normalizedDir);

        if (!hasErrors())
            writeCompensation(workingDir);

        if (!hasErrors())
            importResults(rAnalysisDir, _workspaceName);

        if (_performNormalization && !hasErrors())
            importResults(normalizedDir, "Normalized " + _workspaceName);

        deleteAnalysisDirectory(workingDir.getParentFile());
    }

    @Override
    public String getDescription()
    {
        return "R Analysis " + getExperiment().getName();
    }
}
