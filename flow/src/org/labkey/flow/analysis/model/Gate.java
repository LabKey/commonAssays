/*
 * Copyright (c) 2005-2007 LabKey Corporation
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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.BitSet;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

/**
 * User: mbellew
 * Date: Apr 26, 2005
 * Time: 8:02:11 PM
 */
public abstract class Gate implements Serializable
{
    String _name;

    public Gate()
    {
    }

    public abstract BitSet apply(DataFrame data);

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    static public List<Gate> readGateList(Element element)
    {
        List<Gate> ret = new ArrayList();
        NodeList nlChildren = element.getChildNodes();
        for (int i = 0; i < nlChildren.getLength(); i ++)
        {
            Node nodeChild = nlChildren.item(i);
            if (!(nodeChild instanceof Element))
                continue;
            Element elChild = (Element) nodeChild;
            if ("interval".equals(elChild.getTagName()))
            {
                ret.add(IntervalGate.readInterval(elChild));
            }
            else if ("polygon".equals(elChild.getTagName()))
            {
                ret.add(PolygonGate.readPolygon(elChild));
            }
            else if ("not".equals(elChild.getTagName()))
            {
                ret.add(NotGate.readNot(elChild));
            }
            else if ("and".equals(elChild.getTagName()))
            {
                ret.add(AndGate.readAnd(elChild));
            }
            else if ("or".equals(elChild.getTagName()))
            {
                ret.add(OrGate.readOr(elChild));
            }
            else if ("ellipse".equals(elChild.getTagName()))
            {
                ret.add(EllipseGate.readEllipse(elChild));
            }
        }
        return ret;
    }

    static public Gate readGate(Element element)
    {
        List<Gate> gates = readGateList(element);
        if (gates.size() == 1)
        {
            gates.get(0).setName(element.getAttribute("name"));
            return gates.get(0);
        }
        return null;
    }

    public int hashCode()
    {
        if (_name == null)
            return 0;
        return _name.hashCode();
    }

    public boolean equals(Object other)
    {
        if (other == null)
            return false;
        if (other.getClass() != this.getClass())
            return false;
        Gate otherGate = (Gate) other;
        if (otherGate._name == null && _name == null)
            return true;
        if (otherGate._name == null || _name == null)
            return false;
        return otherGate._name.equals(_name);
    }

    abstract public void getPolygons(List<Polygon> list, String xAxis, String yAxis);
    abstract public boolean requiresCompensationMatrix();
}
