/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
package org.labkey.microarray.matrix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.qc.TransformResult;
import org.labkey.api.query.ValidationException;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.matrix.AbstractMatrixRunCreator;
import org.labkey.microarray.MicroarrayManager;

import java.io.File;

/**
 * User: kevink
 * Date: 1/18/14
 */
public class ExpressionMatrixRunCreator extends AbstractMatrixRunCreator<ExpressionMatrixAssayProvider>
{
    public static final String SAMPLES_ROLE_NAME = "Samples";

    public ExpressionMatrixRunCreator(ExpressionMatrixAssayProvider provider)
    {
        super(provider);
    }

    @Override
    public String getIdColumnName()
    {
        return ExpressionMatrixDataHandler.FEATURE_ID_COLUMN_NAME;
    }

    @Override
    public String getSetPropertyName()
    {
        return ExpressionMatrixAssayProvider.FEATURE_SET_PROPERTY_NAME;
    }

    @Override
    public String getRoleName()
    {
        return SAMPLES_ROLE_NAME;
    }

    @Override
    public String getSetName()
    {
        return "Feature Annotation";
    }

    @Override
    public TransformResult transform(AssayRunUploadContext<ExpressionMatrixAssayProvider> context, ExpRun run) throws ValidationException
    {
        return super.transform(context, run);
    }

    /**
     * Ensure the run property 'featureSet' actually exists.
     *
     * @param context AssayRunUploadContext
     * @param runPath Path under the pipeline root to look for the featureSet, when featureSet is a path.
     * @param featureSet The feature set id, name, or file path.
     * @return The feature annotation set id only if it needs to be saved back to the 'featureSet' property; otherwise null.
     * @throws ValidationException
     */
    @Override
    public Integer ensureSet(@NotNull AssayRunUploadContext<ExpressionMatrixAssayProvider> context, @Nullable File runPath, @NotNull String featureSet) throws ValidationException, ExperimentException
    {
        return ensureFeatureAnnotationSet(context, runPath, featureSet);
    }

    protected Integer ensureFeatureAnnotationSet(@NotNull AssayRunUploadContext<ExpressionMatrixAssayProvider> context, @Nullable File runPath, @NotNull String featureSet) throws ValidationException, ExperimentException
    {
        Integer featureSetId = MicroarrayManager.get().ensureFeatureAnnotationSet(context.getLogger(), context.getContainer(), context.getUser(), runPath, featureSet);
        if (featureSetId == null)
            throw new ValidationException(getSetName()+ " set not found '" + featureSet + "'");

        // return featureSet id only if it needs to be updated
        if (!featureSet.equals(String.valueOf(featureSetId)))
            return featureSetId;

        return null;
    }
}
