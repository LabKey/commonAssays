/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.SpringModule;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.assay.AssayDataType;
import org.labkey.api.assay.AssayService;
import org.labkey.api.util.FileType;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
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
import java.util.Set;

public class MicroarrayModule extends SpringModule
{
    private static final String WEBPART_FEATURE_ANNOTATION_SET = "Feature Annotation Sets";

    private static final String CONTROLLER_NAME = "microarray";
    private static final String FEATURE_ANNOTATION_SET_CONTROLLER_NAME = "feature-annotationset";

    public static final String DB_SCHEMA_NAME = "microarray";

    public static final AssayDataType QC_REPORT_INPUT_TYPE =
            new AssayDataType("MicroarrayQCData", new FileType(".pdf"), "QCReport");
    public static final AssayDataType THUMBNAIL_INPUT_TYPE =
            new AssayDataType("MicroarrayImageData", new FileType(".jpg"), "ThumbnailImage");

    public String getName()
    {
        return "Microarray";
    }

    public double getVersion()
    {
        return 19.20;
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
        AssayService.get().registerAssayProvider(new ExpressionMatrixAssayProvider());
        PipelineService.get().registerPipelineProvider(new GeneDataPipelineProvider(this));

        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(new MicroarrayContainerListener());

        ExperimentService.get().addExperimentListener(new ExpressionMatrixExperimentListener());
        ExperimentService.get().registerExperimentDataHandler(new ExpressionMatrixDataHandler());
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.singleton(DB_SCHEMA_NAME);
    }
}
