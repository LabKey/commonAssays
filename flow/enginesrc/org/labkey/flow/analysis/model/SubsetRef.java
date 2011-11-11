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
import org.w3c.dom.Element;

import java.util.BitSet;
import java.util.List;

/**
 * User: kevink
 * Date: May 10, 2011
 *
 * A subset reference used in boolean gates.
 */
public class SubsetRef extends Gate implements SubsetExpressionGate
{
    private SubsetSpec _ref;
    private SubsetExpression _originalExpression;

    public SubsetRef()
    {
    }

    public SubsetRef(SubsetSpec ref)
    {
        _ref = ref;
    }

    public void setRef(SubsetSpec ref)
    {
        assert !ref.isExpression();
        _ref = ref;
    }

    public SubsetSpec getRef()
    {
        return _ref;
    }

    @Override
    public BitSet apply(PopulationSet populations, DataFrame data)
    {
        // NOTE: we only support populations starting from the root
        SubsetPart[] terms = _ref.getSubsets();
        BitSet ret = new BitSet();
        ret.flip(0, data.getRowCount());
        PopulationSet curr = populations;
        for (int i = 0; i < terms.length; i ++)
        {
            Object term = terms[i];
            if (term instanceof SubsetExpression)
            {
                throw new FlowException("subset expressions not allowed in population references.");
            }
            else if (term instanceof PopulationName)
            {
                Population pop = curr.getPopulation((PopulationName)term);

                if (pop == null)
                {
                    throw new FlowException("Could not find subset '" + _ref + "'");
                }
                for (Gate gate : pop.getGates())
                {
                    BitSet bits = gate.apply(populations, data);
                    ret.and(bits);
                }
                curr = pop;
            }
            else
            {
                throw new FlowException("Unexpected subset term: " + term);
            }
        }
        return ret;
    }

    @Override
    public int hashCode()
    {
        return super.hashCode() ^ this._ref.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (!super.equals(other))
            return false;

        SubsetRef otherSubetRef = (SubsetRef)other;
        return _ref.equals(otherSubetRef._ref);
    }

    @Override
    public boolean isSimilar(Gate other)
    {
        return super.equals(other);
    }

    @Override
    public void getPolygons(List<Polygon> list, String xAxis, String yAxis)
    {
    }

    @Override
    public boolean requiresCompensationMatrix()
    {
        // XXX: Not sure if this is correct
        return false;
    }

    public static SubsetRef readRef(Element el)
    {
        SubsetRef ret = new SubsetRef();
        String ref = el.getAttribute("subset");
        SubsetSpec spec = SubsetSpec.fromEscapedString(ref);
        ret.setRef(spec);
        return ret;
    }

    @Override
    public SubsetExpression createTerm()
    {
        return new SubsetExpression.SubsetTerm(_ref);
    }

    @Override
    public SubsetExpression getOriginalExpression()
    {
        return _originalExpression;
    }

    @Override
    public void setOriginalExpression(SubsetExpression expr)
    {
        _originalExpression = expr;
    }

}
