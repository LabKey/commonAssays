package org.labkey.microarray.pipeline;

import java.net.URI;
import java.io.File;

import org.apache.beehive.netui.pageflow.FormData;
import org.apache.log4j.Logger;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.*;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.study.assay.AssayService;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;


public class PipelineController extends SpringActionController
{
    private static Logger _log = Logger.getLogger(PipelineController.class);

    public static class ExtractionForm extends FormData
    {
        private String _path;
        private int _protocolId;
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
    }

  /*
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

            URI uriRoot = pr.getUri();
            URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
            if (uriData == null)
                HttpView.throwNotFound();

            //PipelineProtocolFactory extractionProtocol = ArrayPipelineManager.getExtractionProtocolInstance(form.getExtractionEngine());
            String error = null;
            ExpProtocol protocol = null;
            String protocolName = form.getProtocol();

            try {
                AssayService.get().getP;
                if("agilent".equalsIgnoreCase(form.getExtractionEngine())) {
                    AgilentExtractionProtocol specificProtocol
                            = new AgilentExtractionProtocol(
                            form.getProtocolName(),
                            form.getProtocolDescription());
                    specificProtocol.setEmail(getUser().getEmail());
                    protocol = specificProtocol;
                } else {
                    // we take Agilent as the default case
                    AgilentExtractionProtocol specificProtocol
                            = new AgilentExtractionProtocol(
                            form.getProtocolName(),
                            form.getProtocolDescription());
                    specificProtocol.setEmail(getUser().getEmail());
                    protocol = specificProtocol;
                }
                protocol.validate(uriRoot);
                if (form.isSaveProtocol()) {
                    protocol.saveDefinition(uriRoot);
                }

                ViewBackgroundInfo info =
                        service.getJobBackgroundInfo(getViewBackgroundInfo(), new File(uriData));
                ArrayPipelineManager.runFeatureExtraction(info,
                        uriRoot,
                        uriData,
                        protocol,
                        form.getExtractionEngine());

                // Forward to the job's container.
                c = info.getContainer();
            } catch (IllegalArgumentException ea) {
                error = ea.getMessage();
            } catch (PipelineValidationException ea) {
                error = ea.getMessage();
            } catch (IOException eio) {
                error = "Failure attempting to write input parameters.  Please try again.";
            }

            if (error == null || error.length() == 0) {
                HttpView.throwRedirect(ViewURLHelper.toPathString("Project", "begin", c.getPath()));
            }

            _log.error("Import image process failed...\n" + error);
            //XXX need to buid an error page to display the pipeline errors
            ViewURLHelper url = cloneViewURLHelper();
            url.setPageFlow("Project");
            url.setAction("begin");
            return new ViewForward(url);
        }
    }

    @RequiresPermission(ACL.PERM_INSERT)
    public class ImportMageFilesAction extends SimpleViewAction<ExtractionForm>
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

            URI uriRoot = pr.getUri();
            URI uriData = URIUtil.resolve(pr.getUri(c), form.getPath());
            if (uriData == null)
                HttpView.throwNotFound();

            AssayService

            //PipelineProtocolFactory extractionProtocol = ArrayPipelineManager.getExtractionProtocolInstance(form.getExtractionEngine());
            String error = null;
            MicroarrayAssayProvider provider = (MicroarrayAssayProvider)AssayService.get().getProvider(MicroarrayAssayProvider.NAME);
            provider.getP
            String protocolName = form.getProtocol();

            ViewBackgroundInfo info =
                    service.getJobBackgroundInfo(getViewBackgroundInfo(), new File(uriData));
            ArrayPipelineManager.runMageLoader(info,
                    uriRoot,
                    uriData,
                    protocol);

            // Forward to the job's container.
            c = info.getContainer();

            if (error == null || error.length() == 0) {
                HttpView.throwRedirect(ViewURLHelper.toPathString("Project", "begin", c.getPath()));
            }

            _log.error("Import image process failed...\n" + error);
            //XXX need to buid an error page to display the pipeline errors
            ViewURLHelper url = cloneViewURLHelper();
            url.setPageFlow("Project");
            url.setAction("begin");
            return new ViewForward(url);
        }
    }
    */
}
