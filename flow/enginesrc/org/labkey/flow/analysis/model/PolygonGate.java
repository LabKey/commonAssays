/*
 * Copyright (c) 2005-2012 LabKey Corporation
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

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.labkey.flow.analysis.data.NumberArray;

/**
 */
public class PolygonGate extends RegionGate
{
    String _strX;
    String _strY;
    Polygon _poly;

    public PolygonGate(String xFieldName, String yFieldName, Polygon poly)
    {
        _strX = xFieldName;
        _strY = yFieldName;
        _poly = poly;
    }

    public BitSet apply(PopulationSet populations, DataFrame data)
    {
        BitSet ret = new BitSet(data.getRowCount());
        NumberArray xValues = data.getColumn(_strX);
        NumberArray yValues = data.getColumn(_strY);
        int count = 0;
        for (int i = 0; i < data.getRowCount(); i ++)
        {
            if (_poly.contains(xValues.getDouble(i), yValues.getDouble(i)))
            {
                ret.set(i);
                count ++;
            }
        }
        // nicksh: Add this check in here because I have been seeing strange behavior of statistics being wrong.
        if (count != ret.cardinality())
        {
            throw new IllegalStateException("Internal error computing polygon gate.  BitSet cardinality " + ret.cardinality() + " does not equal expected value " + count);
        }
        return ret;
    }

    public String getXAxis()
    {
        return _strX;
    }

    public String getYAxis()
    {
        return _strY;
    }

    @Override
    public List<String> getAxes()
    {
        return Arrays.asList(_strX, _strY);
    }

    public Polygon getPolygon()
    {
        return _poly;
    }


    static public PolygonGate readPolygon(Element elPolygon)
    {
        NodeList nlPoints = elPolygon.getElementsByTagName("point");
        double [] arrX = new double[nlPoints.getLength()], arrY = new double[nlPoints.getLength()];
        for (int i = 0; i < nlPoints.getLength(); i ++)
        {
            Element elPoint = (Element) nlPoints.item(i);
            arrX[i] = Double.parseDouble(elPoint.getAttribute("x"));
            arrY[i] = Double.parseDouble(elPoint.getAttribute("y"));
        }
        Polygon poly = new Polygon(arrX, arrY);
        PolygonGate ret = new PolygonGate(elPolygon.getAttribute("xAxis"), elPolygon.getAttribute("yAxis"), poly);
        return ret;
    }


    public int hashCode()
    {
        int ret = super.hashCode();
        ret ^= _strX.hashCode();
        ret ^= _strY.hashCode();
        ret ^= _poly.hashCode();
        return ret;
    }


    public boolean equals(Object other)
    {
        if (!super.equals(other))
            return false;
        PolygonGate gate = (PolygonGate) other;
        if (!_strX.equals(gate._strX))
            return false;
        if (!_strY.equals(gate._strY))
            return false;
        if (!_poly.equals(gate._poly))
            return false;
        return true;
    }


    public void getPolygons(List<Polygon> list, String xAxis, String yAxis)
    {
        double[] X = null;
        double[] Y = null;
        if (getXAxis().equals(xAxis))
            X = getPolygon().X;
        else if (getYAxis().equals(xAxis))
            X = getPolygon().Y;

        if (getXAxis().equals(yAxis))
            Y = getPolygon().X;
        else if (getYAxis().equals(yAxis))
            Y = getPolygon().Y;

        if (X == null || Y == null)
            return;
        list.add(new Polygon(X, Y));
    }


    public boolean requiresCompensationMatrix()
    {
        return CompensationMatrix.isParamCompensated(_strX) || CompensationMatrix.isParamCompensated(_strY);
    }
}
