/*
 * Copyright (c) 2005-2016 LabKey Corporation
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

public class AndGate extends GateList implements SubsetExpressionGate
{
    public AndGate() { }

    public AndGate(Gate... gates)
    {
        super(gates);
    }

    public AndGate(List<Gate> gates)
    {
        super(gates);
    }

    public BitSet apply(PopulationSet populations, DataFrame data)
    {
        BitSet bits = null;
        for (Gate gate : _gates)
        {
            BitSet nextBits = gate.apply(populations, data);
            if (bits == null)
                bits = nextBits;
            else
                bits.and(nextBits);
        }
        return bits;
    }

    static public AndGate readAnd(Element el)
    {
        AndGate ret = new AndGate();
        ret._gates = Gate.readGateList(el);
        return ret;
    }

    public SubsetExpression.AndTerm createTerm()
    {
        if (_gates.size() < 2)
            throw new IllegalStateException("AndGate expects at least two gates");

        Gate leftGate = _gates.get(0);
        if (!(leftGate instanceof SubsetExpressionGate))
            throw new FlowException("can't create term from gate type: " + leftGate);

        Gate rightGate = _gates.get(1);
        if (!(rightGate instanceof SubsetExpressionGate))
            throw new FlowException("can't create term from gate type: " + rightGate);

        SubsetExpression.AndTerm and = new SubsetExpression.AndTerm(
                ((SubsetExpressionGate)leftGate).createTerm(),
                ((SubsetExpressionGate)rightGate).createTerm());

        if (_gates.size() > 2)
        {
            for (Gate g : _gates.subList(2, _gates.size()))
            {
                if (!(g instanceof SubsetExpressionGate))
                    throw new FlowException("can't create term from gate type: " + leftGate);

                and = new SubsetExpression.AndTerm(and, ((SubsetExpressionGate) g).createTerm());
            }
        }

        return and;
    }
}
