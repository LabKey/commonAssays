/*
 * Copyright (c) 2011 LabKey Corporation
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
