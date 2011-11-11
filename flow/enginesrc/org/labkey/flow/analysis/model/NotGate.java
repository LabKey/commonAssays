/*
 * Copyright (c) 2005-2011 LabKey Corporation
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
import org.w3c.dom.Element;

import java.util.BitSet;
import java.util.List;


public class NotGate extends Gate implements SubsetExpressionGate
{
    Gate _gate;
    SubsetExpression _originalExpression;

    public NotGate() { }

    public NotGate(Gate gate)
    {
        setGate(gate);
    }

    public boolean equals(Object other)
    {
        if (!super.equals(other))
            return false;
        NotGate otherGate = (NotGate) other;
        return _gate.equals(otherGate._gate);
    }

    public void setGate(Gate gate)
    {
        _gate = gate;
    }
    public Gate getGate()
    {
        return _gate;
    }

    public int hashCode()
    {
        return super.hashCode() ^ _gate.hashCode();
    }

    @Override
    public boolean isSimilar(Gate other)
    {
        if (equals(other))
            return true;

        if (!(other instanceof NotGate))
            return false;

        NotGate notGate = (NotGate) other;
        return _gate.isSimilar(notGate._gate);
    }

    public void getPolygons(List<Polygon> list, String xAxis, String yAxis)
    {
        _gate.getPolygons(list, xAxis, yAxis);
    }

    public BitSet apply(PopulationSet populations, DataFrame data)
    {
        BitSet bits = _gate.apply(populations, data);
        bits.flip(0, data.getRowCount());
        return bits;
    }

    public boolean requiresCompensationMatrix()
    {
        return _gate.requiresCompensationMatrix();
    }

    static public NotGate readNot(Element el)
    {
        NotGate ret = new NotGate();
        ret._gate = Gate.readGate(el);
        return ret;
    }

    public SubsetExpression.NotTerm createTerm()
    {
        if (!(_gate instanceof SubsetExpressionGate))
            throw new FlowException("can't create term from gate type: " + _gate);

        boolean grouped = false;
        if (_gate instanceof GateList && ((GateList)_gate).getGates().size() > 1)
            grouped = true;

        SubsetExpression.NotTerm term =
            new SubsetExpression.NotTerm(
                ((SubsetExpressionGate)_gate).createTerm());
        term.setGrouped(grouped);

        return term;
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
