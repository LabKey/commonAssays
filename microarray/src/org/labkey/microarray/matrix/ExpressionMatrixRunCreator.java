/*
 * Copyright (c) 2014 LabKey Corporation
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
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.qc.DefaultTransformResult;
import org.labkey.api.qc.TransformResult;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.api.study.assay.ParticipantVisitResolverType;
import org.labkey.microarray.MicroarrayManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: 1/18/14
 */
public class ExpressionMatrixRunCreator extends DefaultAssayRunCreator<ExpressionMatrixAssayProvider>
{
    public static final String SAMPLES_ROLE_NAME = "Samples";

    public ExpressionMatrixRunCreator(ExpressionMatrixAssayProvider provider)
    {
        super(provider);
    }

    @Override
    public TransformResult transform(AssayRunUploadContext<ExpressionMatrixAssayProvider> context, ExpRun run) throws ValidationException
    {
        TransformResult result = super.transform(context, run);

        try
        {
            result = transformFeatureSetId(context, run, result);
        }
        catch (ExperimentException e)
        {
            throw new ValidationException(e.getMessage());
        }

        return result;
    }

    protected TransformResult transformFeatureSetId(AssayRunUploadContext<ExpressionMatrixAssayProvider> context, ExpRun run, TransformResult result) throws ValidationException, ExperimentException
    {
        Map<DomainProperty, String> runProps = result.getRunProperties() != null && !result.getRunProperties().isEmpty() ? result.getRunProperties() : context.getRunProperties();
        Map.Entry<DomainProperty, String> featureSetEntry = findFeatureSetProperty(runProps);
        if (featureSetEntry == null || featureSetEntry.getValue() == null)
            throw new ValidationException("Feature annotation set required");

        Integer updateFeatureSetId = ensureFeatureAnnotationSet(context, run.getFilePathRoot(), featureSetEntry.getValue());
        if (updateFeatureSetId != null)
        {
            DefaultTransformResult ret = new DefaultTransformResult(result);

            Map<DomainProperty, String> updatedRunProps = new HashMap<>(runProps);

            // Set the update featureSet id string value
            updatedRunProps.put(featureSetEntry.getKey(), String.valueOf(updateFeatureSetId));

            ret.setRunProperties(updatedRunProps);

            context.setTransformResult(ret);
            result = ret;
        }

        return result;
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
    protected Integer ensureFeatureAnnotationSet(@NotNull AssayRunUploadContext<ExpressionMatrixAssayProvider> context, @Nullable File runPath, @NotNull String featureSet) throws ValidationException, ExperimentException
    {
        Integer featureSetId = MicroarrayManager.get().ensureFeatureAnnotationSet(context.getLogger(), context.getContainer(), context.getUser(), runPath, featureSet);
        if (featureSetId == null)
            throw new ValidationException("Feature annotation set not found '" + featureSet + "'");

        // return featureSet id only if it needs to be updated
        if (!featureSet.equals(String.valueOf(featureSetId)))
            return featureSetId;

        return null;
    }

    private Map.Entry<DomainProperty, String> findFeatureSetProperty(Map<DomainProperty, String> runProps)
    {
        for (Map.Entry<DomainProperty, String> entry : runProps.entrySet())
        {
            DomainProperty dp = entry.getKey();
            if (ExpressionMatrixAssayProvider.FEATURE_SET_PROPERTY_NAME.equalsIgnoreCase(dp.getName()))
                return entry;
        }

        return null;
    }

    @Override
    protected void addInputMaterials(AssayRunUploadContext<ExpressionMatrixAssayProvider> context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        // Attach the materials found in the matrix file to the run
        try
        {
            File dataFile = getPrimaryFile(context);
            try (TabLoader loader = ExpressionMatrixDataHandler.createTabLoader(dataFile))
            {
                ColumnDescriptor[] cols = loader.getColumns();

                List<String> columnNames = new ArrayList<>(cols.length);
                for (ColumnDescriptor col : cols)
                    columnNames.add(col.getColumnName());

                Map<String, Integer> samplesMap = ExpressionMatrixDataHandler.ensureSamples(context.getContainer(), context.getUser(), columnNames);
                List<? extends ExpMaterial> materials = ExperimentService.get().getExpMaterials(samplesMap.values());
                for (ExpMaterial material : materials)
                {
                    // TODO: Check if there is some other role that might be useful (well id)
                    inputMaterials.put(material, SAMPLES_ROLE_NAME);
                }
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException("Failed to read from data file", e);
        }
    }

    private File getPrimaryFile(AssayRunUploadContext context) throws IOException, ExperimentException
    {
        Map<String, File> files = context.getUploadedData();
        assert files.containsKey(AssayDataCollector.PRIMARY_FILE);
        return files.get(AssayDataCollector.PRIMARY_FILE);
    }

}
