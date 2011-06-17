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

/**
 * User: kevink
 * Date: 6/6/11
 */
public class GateExpressionTransform implements SubsetExpression.Transform<Gate>
{
    @Override
    public Gate and(SubsetExpression.AndTerm term, Gate leftResult, Gate rightResult)
    {
        return new AndGate(leftResult, rightResult);
    }

    @Override
    public Gate or(SubsetExpression.OrTerm term, Gate leftResult, Gate rightResult)
    {
        return new OrGate(leftResult, rightResult);
    }

    @Override
    public Gate not(SubsetExpression.NotTerm term, Gate notResult)
    {
        return new NotGate(notResult);
    }

    @Override
    public Gate subset(SubsetExpression.SubsetTerm term)
    {
        return new SubsetRef(term.getSpec());
    }
}
