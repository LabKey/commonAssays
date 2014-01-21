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
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.api.study.assay.ParticipantVisitResolverType;

import java.util.Map;

/**
 * User: kevink
 * Date: 1/18/14
 */
public class ExpressionMatrixRunCreator extends DefaultAssayRunCreator<ExpressionMatrixAssayProvider>
{
    public ExpressionMatrixRunCreator(ExpressionMatrixAssayProvider provider)
    {
        super(provider);
    }

    @Override
    protected void addInputMaterials(AssayRunUploadContext<ExpressionMatrixAssayProvider> context, Map<ExpMaterial, String> inputMaterials, ParticipantVisitResolverType resolverType) throws ExperimentException
    {
        // UNDONE: Add samples found in matrix column header as inputs
    }
}
