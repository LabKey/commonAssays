/*
 * Copyright (c) 2005-2017 LabKey Corporation
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
import java.util.Comparator;
import java.util.List;
import java.io.Serializable;

/**
 * User: mbellew
 * Date: Apr 26, 2005
 * Time: 8:02:11 PM
 */
public abstract class Gate implements Serializable
{
    public static Comparator<Gate> NAME_COMPARATOR = Comparator.comparing(Gate::getName);

    String _id;
    PopulationName _name;

    public Gate()
    {
    }

    public abstract BitSet apply(PopulationSet populations, DataFrame data);

    public String getId()
    {
        return _id;
    }

    public void setId(String id)
    {
        _id = id;
    }

    public PopulationName getName()
    {
        return _name;
    }

    public void setName(PopulationName name)
    {
        _name = name;
    }

    static public List<Gate> readGateList(Element element)
    {
        List<Gate> ret = new ArrayList<>();
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
            else if ("subset".equals(elChild.getTagName()))
            {
                ret.add(SubsetRef.readRef(elChild));
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
            PopulationName name = PopulationName.fromString(element.getAttribute("name"));
            gates.get(0).setName(name);
            return gates.get(0);
        }
        return null;
    }

    @Override
    public int hashCode()
    {
        int result = 0;//_id != null ? _id.hashCode() : 0;
        result = 31 * result + (_name != null ? _name.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Gate gate = (Gate) o;

        //if (_id != null ? !_id.equals(gate._id) : gate._id != null) return false;
        if (_name != null ? !_name.equals(gate._name) : gate._name != null) return false;

        return true;
    }

    abstract public boolean isSimilar(Gate other);

    abstract public void getPolygons(List<Polygon> list, String xAxis, String yAxis);
    abstract public boolean requiresCompensationMatrix();
}
