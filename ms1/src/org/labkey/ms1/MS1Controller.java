package org.labkey.ms1;

import org.apache.beehive.netui.pageflow.Forward;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.log4j.Logger;
import org.labkey.api.view.*;
import org.labkey.api.data.Container;
import org.labkey.api.security.ACL;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.ExperimentException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import org.labkey.ms1.pipeline.MSInspectImportPipelineJob;

import javax.servlet.ServletException;

/**
 * This controller is the entry point for all web pages specific to the MS1
 * module.
 */

@Jpf.Controller(messageBundles = {@Jpf.MessageBundle(bundlePath = "messages.Validation")})
public class MS1Controller extends ViewController
{
    static Logger _log = Logger.getLogger(MS1Controller.class);

    /**
     * begin() is the default page the user will be shown when they click on the
     * MS1 tab
     */
    @Jpf.Action
    protected Forward begin() throws Exception
    {
        HtmlView v = new HtmlView("MS1 module");
        return renderInTemplate(v);
    }

    /**
     * Invoked when the user clicks to import a .tsv file 
     */
    @Jpf.Action
    protected Forward importMsInspect(ImportForm form) throws Exception
    {
        Container c = getContainer(ACL.PERM_INSERT);
        PipelineService service = PipelineService.get();

        File f = null;
        String path = form.getPath();
        if (path != null)
        {
            // Figure out the pipeline root for this container, which might
            // be set directly on this container, or on a parent container
            PipelineService.PipeRoot pr = service.findPipelineRoot(c);
            if (pr != null)
            {
                // The path to the file to load will be relative to the pipeline root
                URI uriData = URIUtil.resolve(pr.getUri(c), path);
                f = new File(uriData);
            }
        }


        if (null != f && f.exists() && f.isFile())
        {
            // Assuming we can find the job, put a job in the queue to load this run
            ViewBackgroundInfo info = service.getJobBackgroundInfo(getViewBackgroundInfo(), f);
            MSInspectImportPipelineJob job = new MSInspectImportPipelineJob(info, f);
            service.queueJob(job);

            // Be sure to use the job's container for forwarding.
            // Depending on the pipeline setup for this container, the job
            // may actually load the run into a child container
            c = info.getContainer();
        }
        else
        {
            throw new FileNotFoundException("Unable to open the file '" + form.getPath() + "' to load as a msInspect feature file");
        }

        ViewURLHelper url = new ViewURLHelper(getRequest(), "Project", "begin", c.getPath());
        return new ViewForward(url);
    }

    @Jpf.Action
    protected Forward showFeaturesFile(FeatureFileForm form) throws ServletException, ExperimentException, IOException
    {
        // Currently this method just reads the file off of disk and returns
        // it to the browser. To add a richer UI, this is the place to start
        // on the rendering code.
        Container c = getContainer(ACL.PERM_READ);
        ExpData data = ExperimentService.get().getData(form.getDataRowId());
        if (data == null)
        {
            return HttpView.throwNotFound("Could not find data object with rowId " + form.getDataRowId());
        }

        if (!data.getContainer().hasPermission(getUser(), ACL.PERM_READ))
        {
            HttpView.throwUnauthorized();
        }

        File f = data.getDataFile();
        if (f == null || !f.isFile())
        {
            return HttpView.throwNotFound("Could not find data file on disk");
        }

        PageFlowUtil.streamFile(getResponse(), f, false);
        return null;
    }

    /** Helper to use the specified view to render a response */
    private Forward renderInTemplate(HttpView view) throws Exception
    {
        HttpView template = new HomeTemplate(getViewContext(), getContainer(), view);
        return includeView(template);
    }

    /**
     * Beehive/Struts form for importing data 
     */
    public static class ImportForm extends ViewForm
    {
        private String _path;

        public String getPath()
        {
            return _path;
        }

        public void setPath(String path)
        {
            _path = path;
        }
    }

    /**
     * Beehive/Struts form for showing a particular file in the UI
     */
    public static class FeatureFileForm extends ViewForm
    {
        private int _dataRowId;

        public int getDataRowId()
        {
            return _dataRowId;
        }

        public void setDataRowId(int dataRowId)
        {
            _dataRowId = dataRowId;
        }
    }
}