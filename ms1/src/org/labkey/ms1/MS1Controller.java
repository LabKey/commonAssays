package org.labkey.ms1;

import org.apache.log4j.Logger;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.ms1.pipeline.MSInspectImportPipelineJob;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

/**
 * This controller is the entry point for all web pages specific to the MS1
 * module. Each action is represented by a nested class named as such:
 * [action]Action
 */
public class MS1Controller extends SpringActionController
{
    static Logger _log = Logger.getLogger(MS1Controller.class);
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(MS1Controller.class);

    public MS1Controller() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    /**
     * Returns a url helper for the specified action within this controller and current contianer
     * @param action Name of action
     * @return URL helper for the action in this controller and current container
     */
    protected ViewURLHelper getViewURLHelper(String action)
    {
        return new ViewURLHelper(MS1Module.CONTROLLER_NAME, action, getContainer());
    }

    /**
     * Adds the begin step to a NavTree
     * @param root The root of the NavTree
     * @return Modified NavTree
     */
    protected NavTree addBeginChild(NavTree root)
    {
        return root.addChild("MS1 Runs", getViewURLHelper("begin.view"));
    }

    /**
     * Adds the Features view step to a NavTree
     * @param root The root of the NavTree
     * @return Modified NavTree
     */
    protected NavTree addFeaturesChild(NavTree root)
    {
        return root.addChild("Features from Run", getViewURLHelper("showFeatures.view"));
    }

    /**
     * Begin action for the MS1 Module. Displays a list of msInspect feature finding runs
     */
    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return ExperimentService.get().createExperimentRunWebPart(getViewContext(),MS1Module.EXP_RUN_FILTER, true);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return addBeginChild(root);
        }
    } //class BeginAction

    /**
     * Action to show the features for a given experiment run
     */
    @RequiresPermission(ACL.PERM_READ)
    public class ShowFeaturesAction extends SimpleViewAction<RunIdForm>
    {

        public ModelAndView getView(RunIdForm form, BindException errors) throws Exception
        {
            if(-1 == form.getRunId())
                return HttpView.redirect(MS1Controller.this.getViewURLHelper("begin"));
            else
                return new FeaturesView(getViewContext(), form.getRunId());
        }

        public NavTree appendNavTrail(NavTree root)
        {
            addBeginChild(root);
            return addFeaturesChild(root);
        }
    } //class ShowFeaturesAction

    /**
     * Action to import an msInspect TSV features file
     */
    @RequiresPermission(ACL.PERM_INSERT)
    public class ImportMsInspectAction extends SimpleViewAction<ImportForm>
    {
        public ModelAndView getView(ImportForm form, BindException errors) throws Exception
        {
            Container c = getContainer();
            PipelineService service = PipelineService.get();

            File f = null;
            String path = form.getPath();
            if (path != null)
            {
                // Figure out the pipeline root for this container, which might
                // be set directly on this container, or on a parent container
                PipeRoot pr = service.findPipelineRoot(c);
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
                ViewBackgroundInfo info = new ViewBackgroundInfo(getContainer(), getUser(), getViewContext().getViewURLHelper());
                info = service.getJobBackgroundInfo(info, f);
                MSInspectImportPipelineJob job = new MSInspectImportPipelineJob(info, f);
                service.queueJob(job);

                // Be sure to use the job's container for forwarding.
                // Depending on the pipeline setup for this container, the job
                // may actually load the run into a child container
                c = info.getContainer();
            }
            else
            {
                throw new FileNotFoundException("Unable to open the msInspect feature file '" + form.getPath() + "'.");
            }

            ViewURLHelper url = new ViewURLHelper(getViewContext().getRequest(), "Project", "begin", c.getPath());
            return HttpView.redirect(url);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root; //since the action always redirects, just return what was passed in
        }
    } //class ImportMsInspectAction

    /**
     * Form used by the ImportMsInspectAction
     */
    public static class ImportForm
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
    } //class ImportForm

    /**
     * Form used by the ShowFeaturesAction
     */
    public static class RunIdForm
    {
        private int _runId = -1;

        public int getRunId()
        {
            return _runId;
        }

        public void setRunId(int runId)
        {
            _runId = runId;
        }
    } //class RunIDForm
} //class MS1Controller