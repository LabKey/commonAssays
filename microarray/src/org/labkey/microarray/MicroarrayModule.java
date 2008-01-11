package org.labkey.microarray;

import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.security.User;
import org.labkey.api.view.*;
import org.labkey.api.query.QueryView;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.ExperimentRunFilter;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.assay.MageMLDataHandler;
import org.labkey.microarray.pipeline.MicroarrayPipelineProvider;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class MicroarrayModule extends DefaultModule implements ContainerManager.ContainerListener
{
    public static final String NAME = "Microarray";
    public static final String WEBPART_MICROARRAY_RUNS = "Microarray Runs";
    public static final String WEBPART_PENDING_FILES = "Pending MAGE-ML Files";
    private static final String CONTROLLER_NAME = "microarray";

    public static final DataType MAGE_ML_DATA_TYPE = new DataType("MicroarrayAssayData");
    public static final DataType QC_REPORT_DATA_TYPE = new DataType("MicroarrayQCData");
    public static final DataType IMAGE_DATA_TYPE = new DataType("MicroarrayImageData");

    public static final ExperimentRunFilter EXP_RUN_FILTER = new ExperimentRunFilter("Microarray", MicroarraySchema.SCHEMA_NAME, MicroarraySchema.TABLE_RUNS);

    public MicroarrayModule()
    {
        super(NAME, 0.01, null, true,
                new WebPartFactory(WEBPART_MICROARRAY_RUNS)
                {
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        QueryView view = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), MicroarrayModule.EXP_RUN_FILTER, true);
                        view.setTitle(WEBPART_MICROARRAY_RUNS);
                        view.setTitleHref(MicroarrayController.getRunsURL(portalCtx.getContainer()));
                        return view;
                    }
                },
                new WebPartFactory(WEBPART_PENDING_FILES)
                {
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        QueryView view = new PendingMageMLFilesView(portalCtx);
                        view.setTitle(WEBPART_PENDING_FILES);
                        view.setTitleHref(MicroarrayController.getRunsURL(portalCtx.getContainer()));
                        return view;
                    }
                }
        );
        addController(CONTROLLER_NAME, MicroarrayController.class);

        MicroarraySchema.register();
    }

    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c, User user)
    {
    }

    public Set<String> getModuleDependencies()
    {
        return PageFlowUtil.set("Experiment");
    }

    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }

    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(this);
        ModuleLoader.getInstance().registerFolderType(new MicroarrayFolderType(this));
        AssayService.get().registerAssayProvider(new MicroarrayAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new MageMLDataHandler());

        PipelineService.get().registerPipelineProvider(new MicroarrayPipelineProvider());
    }

    public Set<String> getSchemaNames()
    {
        return Collections.singleton(CONTROLLER_NAME);
    }

    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(MicroarraySchema.getSchema());
    }
}