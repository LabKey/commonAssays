package org.labkey.flow.analysis.model;

import org.labkey.flow.analysis.web.SubsetExpression;
import org.labkey.flow.analysis.web.SubsetSpec;

/**
 * User: kevink
 * Date: 9/6/11
 */
public class CleanNameExpressionTransform implements SubsetExpression.Transform<SubsetExpression>
{
    private boolean _tailOnly = false;

    public CleanNameExpressionTransform(boolean tailOnly)
    {
        _tailOnly = tailOnly;
    }

    @Override
    public SubsetExpression and(SubsetExpression.AndTerm term, SubsetExpression leftResult, SubsetExpression rightResult)
    {
        return new SubsetExpression.AndTerm(leftResult, rightResult, term.isGrouped());
    }

    @Override
    public SubsetExpression or(SubsetExpression.OrTerm term, SubsetExpression leftResult, SubsetExpression rightResult)
    {
        return new SubsetExpression.OrTerm(leftResult, rightResult, term.isGrouped());
    }

    @Override
    public SubsetExpression not(SubsetExpression.NotTerm term, SubsetExpression notResult)
    {
        return new SubsetExpression.NotTerm(notResult, term.isGrouped());
    }

    @Override
    public SubsetExpression subset(SubsetExpression.SubsetTerm term)
    {
        SubsetSpec spec = term.getSpec();
        SubsetPart[] parts = spec.getSubsets();

        SubsetSpec cleaned = null;
        if (_tailOnly && parts.length > 0)
        {
            SubsetPart part = parts[parts.length - 1];
            PopulationName cleanedName = PopulationName.fromString(FlowJoWorkspace.___cleanName(part.toString(true, true)));
            cleaned = new SubsetSpec(null, cleanedName);
        }
        else
        {
            for (SubsetPart part : parts)
            {
                PopulationName cleanedName = PopulationName.fromString(FlowJoWorkspace.___cleanName(part.toString(true, true)));
                if (cleaned == null)
                    cleaned = new SubsetSpec(null, cleanedName);
                else
                    cleaned = cleaned.createChild(cleanedName);
            }
        }

        return new SubsetExpression.SubsetTerm(cleaned, term.isGrouped());
    }
}
