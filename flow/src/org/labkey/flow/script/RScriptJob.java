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
import org.labkey.flow.data.FlowExperiment;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.data.FlowProtocolStep;
import org.labkey.flow.data.FlowRun;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * User: kevink
 */
public class RScriptJob extends FlowExperimentJob
{
    private static final String SCRIPT_TEMPLATE =
            "library(labkeyDemo)\n" +
            "runAnalysis(\"${output-directory}\", \"${run-name}\")\n";
    private static final String OUTPUT_DIRECTORY_REPLACEMENT = "output-directory";
    private static final String RUN_NAME_REPLACEMENT = "run-name";

    private FlowExperiment _experiment;
    private FlowRun _keywordsRun;
    //private ScriptEngineReport _report;
    private String _analysisRunName;

    public RScriptJob(ViewBackgroundInfo info, PipeRoot root, FlowExperiment experiment, FlowProtocol protocol, FlowRun keywordsRun, String analysisRunName)
            throws Exception
    {
        super(info, root, experiment.getLSID(), protocol, experiment.getName(), FlowProtocolStep.analysis);
        _experiment = experiment;
        _keywordsRun = keywordsRun;
        //_report = report;
        _analysisRunName = analysisRunName;
    }

    @Override
    public FlowExperiment getExperiment()
    {
        return _experiment;
    }

    // Use Report based scripts so they can exist in the file-system like other reports?
    /*
    private ScriptEngineReport getReport()
    {
        //return _report;

        // UNDONE: Use a saved report rather than creating a new dummy report.
        Module flowModule = ModuleLoader.getInstance().getModule(FlowModule.NAME);
        String reportKey = "~~FlowDemo~~";
        String reportPath = "META-INF";
        FileResource sourceResource = (FileResource)flowModule.getModuleResource(new Path(reportPath, "ranalysis-demo.r"));
        File sourceFile = sourceResource.getFile();

        ModuleRReportDescriptor descriptor = new ModuleRReportDescriptor(flowModule, reportKey, sourceFile, reportPath);
        ScriptEngineReport report = new RReport();
        report.setDescriptor(descriptor);
        return report;
    }

    private void runReportScript(File workingDir) throws ScriptException
    {
        ScriptEngineReport report = getReport();
        ScriptEngine engine = report.getScriptEngine();

        List<ParamReplacement> outputReplacements = new ArrayList<ParamReplacement>();
        ViewContext context = new ViewContext(getInfo());
        String output = report.runScript(context, outputReplacements, null);
    }
    */

    private String getScript() throws IOException
    {
        Module flowModule = ModuleLoader.getInstance().getModule(FlowModule.NAME);
        String reportPath = "META-INF";
        Resource r = flowModule.getModuleResource(new Path(reportPath, "ranalysis-demo.r"));
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
        replacements.put(OUTPUT_DIRECTORY_REPLACEMENT, workingDir.getAbsolutePath()); // ? escape
        replacements.put(RUN_NAME_REPLACEMENT, _analysisRunName); // ? escape

        String script = getScript();
        String output = (String)engine.eval(script);
        info(output);
    }

    @Override
    protected void doRun() throws Throwable
    {
        File workingDir = createAnalysisDirectory(getExperiment().getName(), FlowProtocolStep.analysis);
        runScript(workingDir);

        File summaryStats = new File(workingDir, RImportJob.SUMMARY_STATS_FILENAME);
        RImportJob importJob = new RImportJob(getInfo(), getPipeRoot(), getExperiment(), getProtocol(), _keywordsRun, summaryStats, _analysisRunName);
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
            addError(null, null, e.toString());
        }

        deleteAnalysisDirectory(workingDir);
    }

    @Override
    public String getDescription()
    {
        return "R Analysis " + getExperiment().getName();
    }
}
