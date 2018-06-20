/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

package org.labkey.microarray;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.FolderTypeManager;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.SpringModule;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryView;
import org.labkey.api.search.SearchService;
import org.labkey.api.study.assay.AssayDataType;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.FileType;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.microarray.affy.AffymetrixAssayProvider;
import org.labkey.microarray.affy.AffymetrixDataHandler;
import org.labkey.microarray.assay.MageMLDataHandler;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.controllers.FeatureAnnotationSetController;
import org.labkey.microarray.controllers.MicroarrayController;
import org.labkey.microarray.matrix.ExpressionMatrixAssayProvider;
import org.labkey.microarray.matrix.ExpressionMatrixDataHandler;
import org.labkey.microarray.matrix.ExpressionMatrixExperimentListener;
import org.labkey.microarray.pipeline.GeneDataPipelineProvider;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MicroarrayModule extends SpringModule
{
    public static final String NAME = "Microarray";
    public static final String WEBPART_MICROARRAY_RUNS = "Microarray Runs";
    public static final String WEBPART_MICROARRAY_STATISTICS = "Microarray Summary";
    public static final String WEBPART_PENDING_FILES = "Pending MAGE-ML Files";
    private static final String WEBPART_FEATURE_ANNOTATION_SET = "Feature Annotation Sets";

    private static final String CONTROLLER_NAME = "microarray";
    private static final String FEATURE_ANNOTATION_SET_CONTROLLER_NAME = "feature-annotationset";

    public static final String DB_SCHEMA_NAME = "microarray";

    public static final AssayDataType MAGE_ML_INPUT_TYPE =
            new AssayDataType("MicroarrayAssayData", new FileType(Arrays.asList("_MAGEML.xml", "MAGE-ML.xml", ".mage"), "_MAGEML.xml"), "MageML");
    public static final AssayDataType TIFF_INPUT_TYPE =
            new AssayDataType("MicroarrayTIFF", new FileType(Arrays.asList(".tiff", ".tif"), ".tiff"), "Image");
    public static final AssayDataType QC_REPORT_INPUT_TYPE =
            new AssayDataType("MicroarrayQCData", new FileType(".pdf"), "QCReport");
    public static final AssayDataType THUMBNAIL_INPUT_TYPE =
            new AssayDataType("MicroarrayImageData", new FileType(".jpg"), "ThumbnailImage");
    public static final AssayDataType FEATURES_INPUT_TYPE =
            new AssayDataType("MicroarrayFeaturesData", new FileType("_feat.csv"), "Features");
    public static final AssayDataType GRID_INPUT_TYPE =
            new AssayDataType("MicroarrayGridData", new FileType("_grid.csv"), "Grid");

    /** Collection of all of the non-MageML input types that are handled specially in the code */
    public static final List<AssayDataType> RELATED_INPUT_TYPES =
            Arrays.asList(QC_REPORT_INPUT_TYPE, THUMBNAIL_INPUT_TYPE, FEATURES_INPUT_TYPE, GRID_INPUT_TYPE);

    public String getName()
    {
        return "Microarray";
    }

    public double getVersion()
    {
        return 18.10;
    }

    protected void init()
    {
        addController(CONTROLLER_NAME, MicroarrayController.class);
        addController(FEATURE_ANNOTATION_SET_CONTROLLER_NAME, FeatureAnnotationSetController.class);
        MicroarrayUserSchema.register(this);
    }

    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return new ArrayList<>(Arrays.asList(
            new BaseWebPartFactory(WEBPART_MICROARRAY_RUNS)
            {
                public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    QueryView view = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), MicroarrayRunType.INSTANCE);
                    view.setShowExportButtons(true);
                    view.setTitle(WEBPART_MICROARRAY_RUNS);
                    view.setTitleHref(MicroarrayController.getRunsURL(portalCtx.getContainer()));
                    return view;
                }
            },
            new BaseWebPartFactory(WEBPART_PENDING_FILES)
            {
                public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    QueryView view = new PendingMageMLFilesView(portalCtx);
                    view.setTitle("Pending MageML Files");
                    view.setTitleHref(MicroarrayController.getPendingMageMLFilesURL(portalCtx.getContainer()));
                    return view;
                }
            },
            new BaseWebPartFactory(WEBPART_MICROARRAY_STATISTICS, WebPartFactory.LOCATION_RIGHT)
            {
                public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    WebPartView view = new MicroarrayStatisticsView(portalCtx);
                    view.setTitle(WEBPART_MICROARRAY_STATISTICS);
                    view.setTitleHref(MicroarrayController.getRunsURL(portalCtx.getContainer()));
                    return view;
                }
            },
            new BaseWebPartFactory(WEBPART_FEATURE_ANNOTATION_SET)
            {
                public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    String dataRegionName = MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION_SET + webPart.getIndex();
                    MicroarrayUserSchema schema = new MicroarrayUserSchema(portalCtx.getUser(), portalCtx.getContainer());
                    return schema.createView(portalCtx, dataRegionName, MicroarrayUserSchema.TABLE_FEATURE_ANNOTATION_SET, null);
                }
            }
        ));
    }

    public boolean hasScripts()
    {
        return true;
    }

    @NotNull
    public Collection<String> getSummary(Container c)
    {
        long count = MicroarrayManager.get().featureAnnotationSetCount(c);
        if (count > 0)
            return Arrays.asList(count + " " + (count > 1 ? "Feature annotation sets" : "Feature annotation set"));

        return Collections.emptyList();
    }

    @Override
    protected void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        final ModuleContext finalModuleContext = moduleContext;
        FolderTypeManager.get().registerFolderType(this, new MicroarrayFolderType(this));
        AssayService.get().registerAssayProvider(new MicroarrayAssayProvider());
        AssayService.get().registerAssayProvider(new AffymetrixAssayProvider());
        AssayService.get().registerAssayProvider(new ExpressionMatrixAssayProvider());
        PipelineService.get().registerPipelineProvider(new GeneDataPipelineProvider(this));

        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(new MicroarrayContainerListener());

        ExperimentService.get().addExperimentListener(new ExpressionMatrixExperimentListener());
        ExperimentService.get().registerExperimentDataHandler(new MageMLDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new AffymetrixDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new ExpressionMatrixDataHandler());
        ExperimentService.get().registerExperimentRunTypeSource(container ->
        {
            if (container == null || container.getActiveModules(finalModuleContext.getUpgradeUser()).contains(MicroarrayModule.this))
            {
                return Collections.singleton(MicroarrayRunType.INSTANCE);
            }
            return Collections.emptySet();
        });

        SearchService ss = SearchService.get();
        if (null != ss)
            ss.addDocumentParser(new MageMLDocumentParser());

        // TODO: Are these module properties still needed?
        /*
        ModuleProperty reportProperty = new ModuleProperty(this, "ComparisonReportId");
        reportProperty.setCanSetPerContainer(true);
        addModuleProperty(reportProperty);

        ModuleProperty assayDesignName = new ModuleProperty(this, "AssayDesignName");
        assayDesignName.setCanSetPerContainer(true);
        addModuleProperty(assayDesignName);
        */
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.singleton(DB_SCHEMA_NAME);
    }
}
