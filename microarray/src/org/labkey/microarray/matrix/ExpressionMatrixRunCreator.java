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

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.study.assay.AssayDataCollector;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.api.study.assay.ParticipantVisitResolverType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
