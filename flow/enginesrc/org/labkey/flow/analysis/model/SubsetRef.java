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
public class SubsetRef extends Gate
{
    private SubsetSpec _ref;

    public SubsetRef()
    {
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
        // UNDONE: we only support populations starting from the root
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
                    BitSet bits = gate.apply(null, data);
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
    public void getPolygons(List<Polygon> list, String xAxis, String yAxis)
    {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public boolean requiresCompensationMatrix()
    {
        throw new UnsupportedOperationException("NYI");
    }

    public static SubsetRef readRef(Element el)
    {
        SubsetRef ret = new SubsetRef();
        String ref = el.getAttribute("subset");
        SubsetSpec spec = SubsetSpec.fromEscapedString(ref);
        ret.setRef(spec);
        return ret;
    }
}
