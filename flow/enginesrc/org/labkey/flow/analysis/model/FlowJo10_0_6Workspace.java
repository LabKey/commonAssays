/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.isacNet.std.gatingML.v20.datatypes.ValueAttributeType;
import org.isacNet.std.gatingML.v20.gating.AbstractGateType;
import org.isacNet.std.gatingML.v20.gating.BooleanGateType;
import org.isacNet.std.gatingML.v20.gating.DimensionType;
import org.isacNet.std.gatingML.v20.gating.EllipsoidGateType;
import org.isacNet.std.gatingML.v20.gating.GatingMLDocument;
import org.isacNet.std.gatingML.v20.gating.Point2DType;
import org.isacNet.std.gatingML.v20.gating.PolygonGateType;
import org.isacNet.std.gatingML.v20.gating.RectangleGateDimensionType;
import org.isacNet.std.gatingML.v20.gating.RectangleGateType;
import org.labkey.api.util.UnexpectedException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

import static org.labkey.flow.analysis.model.WorkspaceParser.GATINGML_2_0_PREFIX_MAP;
import static org.labkey.flow.analysis.model.WorkspaceParser.GATING_2_0_NS;
import static org.labkey.flow.analysis.model.WorkspaceParser.TRANSFORMATIONS_2_0_NS;

/**
 * User: kevink
 * Date: 2/26/13
 *
 * FlowJo version 10.0.6 is the first version that supports Gating-ML v2.0.
 */
public class FlowJo10_0_6Workspace extends PC75Workspace
{
    public FlowJo10_0_6Workspace(String name, String path, Element elDoc)
    {
        super(name, path, elDoc);
    }

    @Override
    protected void readSampleCompensation(SampleInfo sampleInfo, Element elSample)
    {
        // FlowJo 10.0.6 uses the GatingML 2.0 namespace with GatingML 1.5 elements
        NodeList nodeList = elSample.getElementsByTagNameNS(TRANSFORMATIONS_2_0_NS, "spilloverMatrix");
        if (nodeList != null && nodeList.getLength() > 0)
        {
            Element elSpilloverMatrix = (Element)nodeList.item(0);
            CompensationMatrix matrix = new CompensationMatrix(elSpilloverMatrix);
            int index = _compensationMatrices.indexOf(matrix);
            if (index == -1)
            {
                _compensationMatrices.add(matrix);
                index = _compensationMatrices.size()-1;
            }
            // CONSIDER: Add CompensationMatrix field to SampleInfo instead of using index
            // Issue 19843: **flow: FlowJo 10.0.6 ArrayIndexOutOfBoundsException during import -- compensationId is a 1-based index into the list
            sampleInfo._compensationId = String.valueOf(index+1);
        }
    }

    private PolygonGate readPolygonGate(PolygonGateType xPolygonGate)
    {
        DimensionType[] dims = xPolygonGate.getDimensionArray();
        if (dims == null || dims.length != 2)
        {
            warning("PolygonGate must have two dimensions");
            return null;
        }

        Point2DType[] vertices = xPolygonGate.getVertexArray();
        if (vertices == null || vertices.length < 3)
        {
            warning("PolygonGate must have at least three vertices");
            return null;
        }

        String xAxis = ___cleanName(dims[0].getFcsDimension().getName());
        String yAxis = ___cleanName(dims[1].getFcsDimension().getName());
        List<Double> lstX = new ArrayList<>();
        List<Double> lstY = new ArrayList<>();

        for (Point2DType vertex : vertices)
        {
            ValueAttributeType[] coord = vertex.getCoordinateArray();
            lstX.add(coord[0].getValue());
            lstY.add(coord[1].getValue());
        }
        scaleValues(xAxis, lstX);
        scaleValues(yAxis, lstY);
        double[] X = toDoubleArray(lstX);
        double[] Y = toDoubleArray(lstY);
        Polygon poly = new Polygon(X, Y);
        return new PolygonGate(xAxis, yAxis, poly);
    }

    private Gate readRectangleGate(RectangleGateType xRectangleGate)
    {
        List<String> axes = new ArrayList<>();
        List<Double> lstMin = new ArrayList<>();
        List<Double> lstMax = new ArrayList<>();
        for (RectangleGateDimensionType dim : xRectangleGate.getDimensionArray())
        {
            axes.add(dim.getFcsDimension().getName());
            lstMin.add(dim.isSetMin() ? dim.getMin() : Double.MIN_VALUE); // XXX: Comment in IntervalGate implies we use Float.MIN/MAX_VALUE instead
            lstMax.add(dim.isSetMax() ? dim.getMax() : Double.MAX_VALUE);
        }

        if (axes.size() == 1)
        {
            return new IntervalGate(axes.get(0), lstMin.get(0), lstMax.get(0));
        }
        else if (axes.size() == 2)
        {
            double[] X = new double[] { lstMin.get(0).doubleValue(), lstMin.get(0).doubleValue(), lstMax.get(0).doubleValue(), lstMax.get(0).doubleValue() };
            double[] Y = new double[] { lstMin.get(1).doubleValue(), lstMax.get(1).doubleValue(), lstMax.get(1).doubleValue(), lstMin.get(1).doubleValue() };
            return new PolygonGate(axes.get(0), axes.get(1), new Polygon(X, Y));
        }
        else
        {
            warning("Multi-dimensional RectangleGate not yet supported");
            return null;
        }
    }

    private Gate readEllipsoidGate(EllipsoidGateType xEllipsoidGate)
    {
        return null;
    }

    private Gate readBooleanGate(BooleanGateType xBooleanGate)
    {
        return null;
    }

    @Override
    protected Gate readGate(Element elGate)
    {
        Gate gate = null;
        try
        {
            XmlOptions options = new XmlOptions();
            options.setLoadReplaceDocumentElement(new QName(GATING_2_0_NS, "Gating-ML"));
            options.setLoadAdditionalNamespaces(GATINGML_2_0_PREFIX_MAP);

            GatingMLDocument xGatingML = GatingMLDocument.Factory.parse(elGate, options);
            XmlObject[] xGates = xGatingML.getGatingML().selectPath("./*");
            if (xGates.length > 1)
                warnOnce("Ignoring other elements under <Gate>");

            XmlObject xGate = xGates[0];

            if (xGate instanceof PolygonGateType)
                gate = readPolygonGate((PolygonGateType) xGate);
            else if (xGate instanceof RectangleGateType)
                gate = readRectangleGate((RectangleGateType) xGate);
            //else if (xGate instanceof EllipsoidGateType)
            //    gate = readEllipsoidGate((EllipsoidGateType) xGate);
            //else if (xGate instanceof BooleanGateType)
            //    gate = readBooleanGate((BooleanGateType) xGate);
            //else
            //    warnOnce(sampleId, analysis.getName(), subset, "Unsupported gate '" + elChild.getTagName() + "'");

            if (gate != null && xGate instanceof AbstractGateType)
                gate.setId(((AbstractGateType) xGate).getId());
        }
        catch (XmlException e)
        {
            throw new UnexpectedException(e);
        }
        return gate;
    }

    @Override
    protected void readCompensationMatrices(Element elDoc)
    {
        // Compensation matrices moved to Group and Sample
    }
}
