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

import java.util.Map;

/**
 * User: kevink
 * Date: 6/6/11
 */
public class RemapExpressionTransform implements SubsetExpression.Transform<SubsetExpression>
{
    Map<SubsetSpec, SubsetSpec> _mapping;

    RemapExpressionTransform(Map<SubsetSpec, SubsetSpec> mapping)
    {
        _mapping = mapping;
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
        if (!_mapping.containsKey(spec))
            throw new FlowException("Failed to replace '" + spec + "' in boolean expression using mapping: " + _mapping);
        return new SubsetExpression.SubsetTerm(_mapping.get(spec), term.isGrouped());
    }
}
