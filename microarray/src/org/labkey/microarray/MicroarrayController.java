package org.labkey.microarray;

import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.*;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.URIUtil;
import org.labkey.microarray.pipeline.ArrayPipelineManager;
import org.labkey.microarray.pipeline.FeatureExtractionPipelineJob;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.apache.beehive.netui.pageflow.FormData;

import java.net.URI;
import java.io.FileNotFoundException;

public class MicroarrayController extends SpringActionController
{
    static DefaultActionResolver _actionResolver = new DefaultActionResolver(MicroarrayController.class);

    public MicroarrayController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }

    public static ViewURLHelper getRunsURL(Container c)
    {
        return new ViewURLHelper(ShowRunsAction.class, c);
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowRunsAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return ExperimentService.get().createExperimentRunWebPart(getViewContext(), MicroarrayModule.EXP_RUN_FILTER, true);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new HtmlView("Test");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    public static class ExtractionForm extends FormData
    {
        private String _path;
        private int _protocolId;
        private String _protocolName;
        private String _extractionEngine = "Agilent";

        public String getPath()
        {
            return _path;
        }

        public void setPath(String path)
        {
            _path = path;
        }

        public int getProtocol()
        {
            return _protocolId;
        }

        public void setProtocol(int protocol)
        {
            _protocolId = protocol;
        }

        public ExpProtocol lookupProtocol()
        {
            return ExperimentService.get().getExpProtocol(_protocolId);
        }

        public String getExtractionEngine()
        {
            return _extractionEngine;
        }

        public void setExtractionhEngine(String extractionEngine)
        {
            _extractionEngine = extractionEngine;
        }

        public String getProtocolName()
        {
            return _protocolName;
        }

        public void setProtocolName(String protocolName)
        {
            _protocolName = protocolName;
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class ImportImageFilesAction extends SimpleViewAction<ExtractionForm>
    {
        public NavTree appendNavTrail(NavTree root)
        {
            throw new UnsupportedOperationException();
        }

        public ModelAndView getView(ExtractionForm form, BindException errors) throws Exception
        {
            Container c = getContainer();
            PipelineService service = PipelineService.get();

            PipeRoot pr = service.findPipelineRoot(c);
            if (pr == null || !URIUtil.exists(pr.getUri()))
                HttpView.throwNotFound();

            URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
            if (uriData == null)
            {
                HttpView.throwNotFound();
            }

            try
            {
                PipelineJob job = new FeatureExtractionPipelineJob(getViewBackgroundInfo(), form.getProtocolName(), uriData, form.getExtractionEngine());
                PipelineService.get().queueJob(job);

                HttpView.throwRedirect(ViewURLHelper.toPathString("Project", "begin", c.getPath()));
            }
            catch (FileNotFoundException e)
            {
                //TODO - need to buid an error page to display the pipeline errors
                throw new ExperimentException("Import image process failed", e);
            }
            return null;
        }
    }

}