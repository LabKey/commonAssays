/*
 * Copyright (C) 2005 LabKey LLC. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
