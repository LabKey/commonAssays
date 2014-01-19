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
