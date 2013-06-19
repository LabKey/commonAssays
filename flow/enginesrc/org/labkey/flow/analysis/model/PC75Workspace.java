/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.isacNet.std.gatingML.v15.datatypes.ValueAttributeType;
import org.isacNet.std.gatingML.v15.gating.AbstractGateType;
import org.isacNet.std.gatingML.v15.gating.BooleanGateType;
import org.isacNet.std.gatingML.v15.gating.DimensionType;
import org.isacNet.std.gatingML.v15.gating.EllipsoidGateType;
import org.isacNet.std.gatingML.v15.gating.GatingMLDocument;
import org.isacNet.std.gatingML.v15.gating.Point2DType;
import org.isacNet.std.gatingML.v15.gating.PolygonGateType;
import org.isacNet.std.gatingML.v15.gating.RectangleGateDimensionType;
import org.isacNet.std.gatingML.v15.gating.RectangleGateType;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static org.labkey.flow.analysis.model.WorkspaceParser.GATINGML_1_5_PREFIX_MAP;
import static org.labkey.flow.analysis.model.WorkspaceParser.GATING_1_5_NS;
import static org.labkey.flow.analysis.model.WorkspaceParser.GATING_2_0_NS;

/**
 * User: kevink
 * Date: 2/13/12
 *
 * First version of FlowJo that supports Gating-ML v1.5.
 */
public class PC75Workspace extends PCWorkspace
{
    public PC75Workspace(String name, String path, Element elDoc)
    {
        super(name, path, elDoc);
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

        String xAxis = ___cleanName(dims[0].getParameter().getName());
        String yAxis = ___cleanName(dims[1].getParameter().getName());
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
            axes.add(dim.getParameter().getName());
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

    protected Gate readGate(Element elGate, SubsetSpec subset, Analysis analysis, String sampleId)
    {
        List<Element> elChildren = getElements(elGate);
        if (elChildren.size() == 0)
        {
            warnOnce(sampleId, analysis.getName(), subset, "No gate found");
            return null;
        }

        Element elChild = elChildren.get(0);
        boolean invert = inverted(elChild);

        Gate gate = readGate(elGate);

        if (gate != null)
        {
            String id = gate.getId();
            if (id == null || "".equals(id))
                id = elGate.getAttributeNS(GATING_1_5_NS, "id");
            if (id == null || "".equals(id))
                id = elGate.getAttributeNS(GATING_2_0_NS, "id");

            if (invert)
            {
                gate.setId(null);
                gate = new NotGate(gate);
            }

            if (id != null)
                gate.setId(id);
        }

        return gate;
    }

    protected Gate readGate(Element elGate)
    {
        Gate gate = null;
        try
        {
            XmlOptions options = new XmlOptions();
            options.setLoadReplaceDocumentElement(new QName(GATING_1_5_NS, "Gating-ML"));
            options.setLoadAdditionalNamespaces(GATINGML_1_5_PREFIX_MAP);
            //options.setLoadSubstituteNamespaces(FJ_GATINGML_NAMEPSACE_FIXUP);

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
    protected void readGates(Element elPopulation, SubsetSpec subset, SubsetSpec parentSubset, Population ret, Analysis analysis, String sampleId)
    {
        for (Element elGate : getElementsByTagName(elPopulation, "Gate"))
        {
            Gate gate = readGate(elGate, subset, analysis, sampleId);
            if (gate != null)
            {
                ret.addGate(gate);
                if (gate instanceof RegionGate)
                    analysis.addGraph(new GraphSpec(parentSubset, ((RegionGate)gate).getAxes()));
            }
        }
    }
}
