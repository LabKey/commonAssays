/*
 * Copyright (c) 2006-2014 LabKey Corporation
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
package org.labkey.ms2.reader;

import net.systemsbiology.regisWeb.pepXML.PeptideprophetSummaryDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.values.XmlValueOutOfRangeException;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.reader.SimpleXMLStreamReader;
import org.labkey.api.util.MatrixUtil;

import javax.xml.stream.XMLStreamException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: arauch
 * Date: Mar 6, 2006
 * Time: 1:04:29 PM
 */
public class PeptideProphetSummary extends SensitivitySummary
{
    private static final Pattern VERSION_PATTERN = Pattern.compile(".*TPP v(\\d+(\\.\\d+)?).*");
    public static final String PEPTIDEPROPHET_SUMMARY_TAG = "peptideprophet_summary";
    public static final String ANALYSIS_SUMMARY_TAG = "analysis_summary";

    private float[] _fval;
    private float[][] _modelPos;
    private float[][] _modelNeg;
    private float[][] _obs;

    public static PeptideProphetSummary load(SimpleXMLStreamReader parser) throws XMLStreamException
    {
        // We're currently at a start <analysis_summary> tag for the PeptideProphet section
        // Look for a starting <peptideprophet_summary> tag
        while (!parser.isStartElement() || !PEPTIDEPROPHET_SUMMARY_TAG.equals(parser.getLocalName()))
        {
            // Bail out if we exit the current <analysis_summary> section
            if (!parser.hasNext() || (parser.isEndElement() && ANALYSIS_SUMMARY_TAG.equals(parser.getLocalName())))
            {
                return null;
            }
            parser.next();
        }

        try
        {
            Double version = parseTPPVersion(parser.getAttributeValue(null, "version"));
            // Starting with version 4.5, the TPP changed the XML schema for where the PeptideProphet summary data
            // is stored
            if (version != null && version.doubleValue() >= 4.50)
            {
                // We've hacked the namespace for the newer pepXML XSD file that we have checked in so that
                // XMLBeans doesn't try to generate duplicate classes. Tell the parser to treat the alternative
                // namespace like the real namespace so that it recognizes the document
                XmlOptions options = new XmlOptions();
                Map<String,String> namespaceMap = new HashMap<>();
                namespaceMap.put("http://regis-web.systemsbiology.net/pepXML", "http://regis-web.systemsbiology.net/pepXML117");
                options.setLoadSubstituteNamespaces(namespaceMap);
                net.systemsbiology.regisWeb.pepXML117.PeptideprophetSummaryDocument.PeptideprophetSummary summary = net.systemsbiology.regisWeb.pepXML117.PeptideprophetSummaryDocument.Factory.parse(parser, options).getPeptideprophetSummary();
                return new PeptideProphetSummary(summary);
            }
            else
            {
                // Fall back on our old parsing code
                PeptideprophetSummaryDocument.PeptideprophetSummary summary = PeptideprophetSummaryDocument.Factory.parse(parser).getPeptideprophetSummary();
                return new PeptideProphetSummary(summary);
            }
        }
        catch(XmlException | XmlValueOutOfRangeException e)
        {
            throw new XMLStreamException("Failed to parse PeptideProphet summary data, this appears to be an invalid XML file", e);
        }
    }


    private PeptideProphetSummary(net.systemsbiology.regisWeb.pepXML117.PeptideprophetSummaryDocument.PeptideprophetSummary summary)
    {
        this();
        net.systemsbiology.regisWeb.pepXML117.PeptideprophetSummaryDocument.PeptideprophetSummary.RocErrorData[] datas = summary.getRocErrorDataArray();

        for (net.systemsbiology.regisWeb.pepXML117.PeptideprophetSummaryDocument.PeptideprophetSummary.RocErrorData data : datas)
        {
            // Only import the rollup data, not the individual charge states
            if ("all".equalsIgnoreCase(data.getCharge()))
            {
                net.systemsbiology.regisWeb.pepXML117.PeptideprophetSummaryDocument.PeptideprophetSummary.DistributionPoint[] distribution = summary.getDistributionPointArray();
                _fval = new float[distribution.length];
                _obs = new float[3][distribution.length];
                _modelPos = new float[3][distribution.length];
                _modelNeg = new float[3][distribution.length];

                for (int i=0; i<distribution.length; i++)
                {
                    net.systemsbiology.regisWeb.pepXML117.PeptideprophetSummaryDocument.PeptideprophetSummary.DistributionPoint point = distribution[i];

                    _fval[i] = point.getFvalue();
                    _modelPos[0][i] = point.getModel1PosDistr();
                    _modelNeg[0][i] = point.getModel1NegDistr();
                    _obs[0][i] = point.getObs1Distr().floatValue();
                    _modelPos[1][i] = point.getModel2PosDistr();
                    _modelNeg[1][i] = point.getModel2NegDistr();
                    _obs[1][i] = point.getObs2Distr().floatValue();
                    _modelPos[2][i] = point.getModel3PosDistr();
                    _modelNeg[2][i] = point.getModel3NegDistr();
                    _obs[2][i] = point.getObs3Distr().floatValue();
                }

                net.systemsbiology.regisWeb.pepXML117.PeptideprophetSummaryDocument.PeptideprophetSummary.RocErrorData.RocDataPoint[] roc = data.getRocDataPointArray();

                _minProb = new float[roc.length];
                _sensitivity = new float[roc.length];
                _error = new float[roc.length];

                for (int i = 0; i < roc.length; i++)
                {
                    net.systemsbiology.regisWeb.pepXML117.PeptideprophetSummaryDocument.PeptideprophetSummary.RocErrorData.RocDataPoint rocDataPoint = roc[i];

                    _minProb[i] = rocDataPoint.getMinProb();
                    _sensitivity[i] = rocDataPoint.getSensitivity();
                    _error[i] = rocDataPoint.getError();
                }
            }
        }

    }

    private PeptideProphetSummary(PeptideprophetSummaryDocument.PeptideprophetSummary summary)
    {
        PeptideprophetSummaryDocument.PeptideprophetSummary.DistributionPoint[] distribution = summary.getDistributionPointArray();

        _fval = new float[distribution.length];
        _obs = new float[3][distribution.length];
        _modelPos = new float[3][distribution.length];
        _modelNeg = new float[3][distribution.length];

        for (int i=0; i<distribution.length; i++)
        {
            PeptideprophetSummaryDocument.PeptideprophetSummary.DistributionPoint point = distribution[i];

            _fval[i] = point.getFvalue();
            _modelPos[0][i] = point.getModel1PosDistr();
            _modelNeg[0][i] = point.getModel1NegDistr();
            _obs[0][i] = point.getObs1Distr().floatValue();
            _modelPos[1][i] = point.getModel2PosDistr();
            _modelNeg[1][i] = point.getModel2NegDistr();
            _obs[1][i] = point.getObs2Distr().floatValue();
            _modelPos[2][i] = point.getModel3PosDistr();
            _modelNeg[2][i] = point.getModel3NegDistr();
            _obs[2][i] = point.getObs3Distr().floatValue();
        }

        PeptideprophetSummaryDocument.PeptideprophetSummary.RocDataPoint[] roc = summary.getRocDataPointArray();

        _minProb = new float[roc.length];
        _sensitivity = new float[roc.length];
        _error = new float[roc.length];

        for (int i = 0; i < roc.length; i++)
        {
            PeptideprophetSummaryDocument.PeptideprophetSummary.RocDataPoint rocDataPoint = roc[i];

            _minProb[i] = rocDataPoint.getMinProb();
            _sensitivity[i] = rocDataPoint.getSensitivity();
            _error[i] = rocDataPoint.getError();
        }
    }

    public PeptideProphetSummary()
    {
        _obs = new float[3][];
        _modelPos = new float[3][];
        _modelNeg = new float[3][];
    }

    public byte[] getFValSeries()
    {
        return toByteArray(_fval);
    }

    public void setFValSeries(byte[] pepProphetFValSeries)
    {
        _fval = toFloatArray(pepProphetFValSeries);
    }

    public byte[] getObsSeries1()
    {
        return toByteArray(_obs[0]);
    }

    public void setObsSeries1(byte[] pepProphetObsSeries1)
    {
        _obs[0] = toFloatArray(pepProphetObsSeries1);
    }

    public byte[] getObsSeries2()
    {
        return toByteArray(_obs[1]);
    }

    public void setObsSeries2(byte[] pepProphetObsSeries2)
    {
        _obs[1] = toFloatArray(pepProphetObsSeries2);
    }

    public byte[] getObsSeries3()
    {
        return toByteArray(_obs[2]);
    }

    public void setObsSeries3(byte[] pepProphetObsSeries3)
    {
        _obs[2] = toFloatArray(pepProphetObsSeries3);
    }

    public byte[] getModelPosSeries1()
    {
        return toByteArray(_modelPos[0]);
    }

    public void setModelPosSeries1(byte[] pepProphetModelPosSeries1)
    {
        _modelPos[0] = toFloatArray(pepProphetModelPosSeries1);
    }

    public byte[] getModelPosSeries2()
    {
        return toByteArray(_modelPos[1]);
    }

    public void setModelPosSeries2(byte[] pepProphetModelPosSeries2)
    {
        _modelPos[1] = toFloatArray(pepProphetModelPosSeries2);
    }

    public byte[] getModelPosSeries3()
    {
        return toByteArray(_modelPos[2]);
    }

    public void setModelPosSeries3(byte[] pepProphetModelPosSeries3)
    {
        _modelPos[2] = toFloatArray(pepProphetModelPosSeries3);
    }

    public byte[] getModelNegSeries1()
    {
        return toByteArray(_modelNeg[0]);
    }

    public void setModelNegSeries1(byte[] pepProphetModelNegSeries1)
    {
        _modelNeg[0] = toFloatArray(pepProphetModelNegSeries1);
    }

    public byte[] getModelNegSeries2()
    {
        return toByteArray(_modelNeg[1]);
    }

    public void setModelNegSeries2(byte[] pepProphetModelNegSeries2)
    {
        _modelNeg[1] = toFloatArray(pepProphetModelNegSeries2);
    }

    public byte[] getModelNegSeries3()
    {
        return toByteArray(_modelNeg[2]);
    }

    public void setModelNegSeries3(byte[] pepProphetModelNegSeries3)
    {
        _modelNeg[2] = toFloatArray(pepProphetModelNegSeries3);
    }

    public float[] getFval() {
        return _fval;
    }

    public float[] getObs(int charge) {
        return _obs[charge - 1];
    }

    public float[] getModelPos(int charge) {
        return _modelPos[charge - 1];
    }

    public float[] getModelNeg(int charge) {
        return _modelNeg[charge - 1];
    }

    public float[] getModelTotal(int charge)
    {
        float[] pos = getModelPos(charge);
        float[] neg = getModelNeg(charge);
        float[] total = new float[pos.length];

        for (int i=0; i<total.length; i++)
                total[i] = pos[i] + neg[i];

        return total;
    }

    public double distributionR2(int charge)
    {
        double r2 = -1.0;

        try
        {
            r2 = MatrixUtil.r2(getModelTotal(charge), getObs(charge));
        }
        catch(RuntimeException e)
        {
            // Ignore matrix errors... probably means blank PeptideProphet distribution
        }

        return r2;
    }


    public static float[] toFloatArray(byte[] source)
    {
        if (null == source)
            source = new byte[0];

        ByteBuffer bb = ByteBuffer.wrap(source);
        // Intel native is LITTLE_ENDIAN -- UNDONE: Make this an app-wide constant?

        bb.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer fb = bb.asFloatBuffer();
        float[] result = new float[fb.capacity()];
        fb.get(result);
        return result;
    }

    public static byte[] toByteArray(float[] x)
    {
        if (x == null)
        {
            return null;
        }
        if (x.length == 0)
        {
            return new byte[0];
        }
        int floatCount = x.length;
        ByteBuffer bb = ByteBuffer.allocate(floatCount * 4);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < floatCount; i++)
            bb.putFloat(x[i]);

        return bb.array();
    }

    public static byte[] toByteArray(int[] x)
    {
        if (x == null)
        {
            return null;
        }
        if (x.length == 0)
        {
            return new byte[0];
        }
        int floatCount = x.length;
        ByteBuffer bb = ByteBuffer.allocate(floatCount * 4);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < floatCount; i++)
            bb.putInt(x[i]);

        return bb.array();
    }

    public static int[] toIntArray(byte[] source)
    {
        if (null == source)
            source = new byte[0];

        ByteBuffer bb = ByteBuffer.wrap(source);
        // Intel native is LITTLE_ENDIAN -- UNDONE: Make this an app-wide constant?

        bb.order(ByteOrder.LITTLE_ENDIAN);
        IntBuffer ib = bb.asIntBuffer();
        int[] result = new int[ib.capacity()];
        ib.get(result);
        return result;
    }

    @Nullable
    private static Double parseTPPVersion(@Nullable String tppVersion)
    {
        if (tppVersion == null)
        {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(tppVersion);
        if (matcher.matches())
        {
            String versionString = matcher.group(1);
            try
            {
                return new Double(versionString);
            }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public static class TestCase extends Assert
    {
        private static final double DELTA = 1E-8;
    
        @Test
        public void testVersionParse()
        {
            // Test some real version strings
            assertEquals(4.7, parseTPPVersion("PeptideProphet  (TPP v4.7 POLAR VORTEX rev 1, Build 201405141849 (MinGW))"), DELTA);
            assertEquals(4.3, parseTPPVersion("PeptideProphet  (TPP v4.3 JETSTREAM rev 1, Build 200909211148 (MSVC))"), DELTA);
            assertEquals(4.5, parseTPPVersion("PeptideProphet  (TPP v4.5 RAPTURE rev 1, Build 201112210851 (linux))"), DELTA);
            assertEquals(2.9, parseTPPVersion("PeptideProphet v3.0 April 1, 2004 (TPP v2.9 GALE rev.3, Build 200611090444(Win32))"), DELTA);
            assertEquals(3.2, parseTPPVersion("PeptideProphet v3.0 April 1, 2004 (TPP v3.2 SQUALL rev.0, Build 200706151416)"), DELTA);
            assertEquals(4.3, parseTPPVersion("PeptideProphet  (TPP v4.3 JETSTREAM rev 1, Build 201011301302 (linux))"), DELTA);
            assertEquals(4.4, parseTPPVersion("PeptideProphet  (TPP v4.4 VUVUZELA rev 1, Build 201012011012 (linux))"), DELTA);

            // Test some hypothetical version strings
            assertEquals(45.33, parseTPPVersion("PeptideProphet  (TPP v45.33 JETSTREAM rev 1, Build 201011301302 (linux))"), DELTA);
            assertEquals(4.5, parseTPPVersion("PeptideProphet  (TPP v4.5.1 JETSTREAM rev 1, Build 201011301302 (linux))"), DELTA);

            // Test some garbage input
            assertEquals(null, parseTPPVersion("PeptideProphet  (TPP vBAD JETSTREAM rev 1, Build 201011301302 (linux))"));
            assertEquals(null, parseTPPVersion(null));
            assertEquals(null, parseTPPVersion("PeptideProphet"));
        }
    }
}
