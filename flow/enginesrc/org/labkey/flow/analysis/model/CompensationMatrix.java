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

import Jama.Matrix;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.util.JunitUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.flow.analysis.data.NumberArray;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static org.labkey.flow.analysis.model.WorkspaceParser.COMPENSATION_1_5_NS;
import static org.labkey.flow.analysis.model.WorkspaceParser.COMPENSATION_2_0_NS;
import static org.labkey.flow.analysis.model.WorkspaceParser.DATATYPES_1_5_NS;
import static org.labkey.flow.analysis.model.WorkspaceParser.DATATYPES_2_0_NS;
import static org.labkey.flow.analysis.model.WorkspaceParser.TRANSFORMATIONS_1_5_NS;
import static org.labkey.flow.analysis.model.WorkspaceParser.TRANSFORMATIONS_2_0_NS;

/**
 * Compensation matrix as read from a FlowJo workspace file.
 * The compensation matrix contains the list of the amount which each fluorescene contributes to a channel value.
 * It is necessary to invert this matrix to obtain the compensated data.
 */
public class CompensationMatrix implements Serializable
{
    public static String PREFIX = "<";
    public static String SUFFIX = ">";
    public static String DITHERED_PREFIX = "dithered-";
    String _name;
    String[] _channelNames;
    double[][] _rows;
    String _prefix;
    String _suffix;

    public CompensationMatrix(String name)
    {
        this(name, PREFIX, SUFFIX);
    }

    public void setName(String name)
    {
        _name = name;
    }

    public void setPrefix(String prefix)
    {
        _prefix = prefix;
    }

    public String getPrefix()
    {
        return _prefix;
    }

    public void setSuffix(String suffix)
    {
        _suffix = suffix;
    }

    public String getSuffix()
    {
        return _suffix;
    }

    public CompensationMatrix(Element elMatrix)
    {
        init(elMatrix);
    }

    public CompensationMatrix(File file) throws Exception
    {
        this(new FileInputStream(file));
    }

    public CompensationMatrix(InputStream is) throws Exception
    {
        String strContents = PageFlowUtil.getStreamContentsAsString(is);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        try
        {
            Document doc = db.parse(strContents);
            init(doc.getDocumentElement());
        }
        catch (Exception e)
        {
            init(strContents);
        }
    }

    private void init(Element elMatrix)
    {
        String tagName = elMatrix.getLocalName();
        String nsURI = elMatrix.getNamespaceURI();

        if ("CompensationMatrix".equals(tagName))
        {
            initCompensationMatrix(elMatrix);
        }
        else if (COMPENSATION_1_5_NS.equals(nsURI) && "spilloverMatrix".equals(tagName))
        {
            initGatingML_1_5(elMatrix);
        }
        else if (TRANSFORMATIONS_2_0_NS.equals(nsURI) && "spilloverMatrix".equals(tagName))
        {
            // FlowJo v10.0.6 serializes a GatingML 1.5 matrix using the 2.0 namespace. Why for the love of god?
            initGatingML_1_5(elMatrix);
        }
        else if (TRANSFORMATIONS_2_0_NS.equals(nsURI) && "spectrumMatrix".equals(tagName))
        {
            initGatingML_2_0(elMatrix);
        }
    }

    // Read the Mac FlowJo xml format
    private void initCompensationMatrix(Element elMatrix)
    {
        // FlowJo v9.7 uses 'matrixName' attribute on <CompensationMatrix> element
        _name = FlowJoWorkspace.getAttribute(elMatrix, "name", "matrixName");
        _prefix = elMatrix.getAttribute("prefix");
        _prefix = _prefix == null ? "<" : _prefix;
        _suffix = elMatrix.getAttribute("suffix");
        _suffix = _suffix == null ? ">" : _suffix;

        NodeList nlChannels = elMatrix.getChildNodes();
        for (int iChannel = 0; iChannel < nlChannels.getLength(); iChannel ++)
        {
            if (!(nlChannels.item(iChannel) instanceof Element))
                continue;
            Element elChannel = (Element) nlChannels.item(iChannel);
            HashMap<String, Double> mapValues = new HashMap<>();
            NodeList nlChannelValues = elChannel.getChildNodes();
            for (int iValue = 0; iValue < nlChannelValues.getLength(); iValue ++)
            {
                if (!(nlChannelValues.item(iValue) instanceof Element))
                    continue;
                Element elChannelValue = (Element) nlChannelValues.item(iValue);

                // FlowJo v9.7 uses 'fluorName' attribute name
                String fluorName = FlowJoWorkspace.getAttribute(elChannelValue, "name", "fluorName");

                // FlowJo v9.7 uses 'spillValue' attribute name
                String spillValue = FlowJoWorkspace.getAttribute(elChannelValue, "value", "spillValue");

                mapValues.put(fluorName, new Double(spillValue));
            }
            setChannel(elChannel.getAttribute("name"), mapValues);
        }
    }

    // Read the GatingML 1.5 xml format
    // NOTE: FlowJo 10.0.6 incorrectly uses GatingML 2.0 namespaces on GatingML 1.5 elements.
    private void initGatingML_1_5(Element elMatrix)
    {
        _name = elMatrix.getAttribute("name");
        if (_name == null)
            _name = FlowJoWorkspace.getAttribute(elMatrix, new QName(COMPENSATION_1_5_NS, "id"), new QName(COMPENSATION_2_0_NS, "id"));
        _prefix = elMatrix.getAttribute("prefix");
        _prefix = _prefix == null ? "<" : _prefix;
        _suffix = elMatrix.getAttribute("suffix");
        _suffix = _suffix == null ? ">" : _suffix;

        NodeList nlSpillover = elMatrix.getElementsByTagNameNS(TRANSFORMATIONS_1_5_NS, "spillover");
        if (nlSpillover.getLength() == 0)
            nlSpillover = elMatrix.getElementsByTagNameNS(TRANSFORMATIONS_2_0_NS, "spillover");

        for (int i = 0; i < nlSpillover.getLength(); i++)
        {
            if (!(nlSpillover.item(i) instanceof Element))
                continue;
            Element elSpillover = (Element)nlSpillover.item(i);

            String channelName = elSpillover.getAttributeNS(DATATYPES_1_5_NS, "parameter");
            if (channelName == null || channelName.length() == 0)
                channelName = elSpillover.getAttributeNS(DATATYPES_2_0_NS, "parameter");
            if (channelName == null || channelName.length() == 0)
                throw new FlowException("Compensation matrix spillover name required");

            Map<String, Double> mapValues = new HashMap<>();
            NodeList nlCoefficient = elSpillover.getElementsByTagNameNS(TRANSFORMATIONS_1_5_NS, "coefficient");
            if (nlCoefficient.getLength() == 0)
                nlCoefficient = elSpillover.getElementsByTagNameNS(TRANSFORMATIONS_2_0_NS, "coefficient");
            for (int j = 0; j < nlCoefficient.getLength(); j++)
            {
                if (!(nlCoefficient.item(j) instanceof Element))
                    continue;
                Element elCoefficient = (Element)nlCoefficient.item(j);

                String parameterName = elCoefficient.getAttributeNS(DATATYPES_1_5_NS, "parameter");
                if (parameterName == null || parameterName.length() == 0)
                    parameterName = elCoefficient.getAttributeNS(DATATYPES_2_0_NS, "parameter");
                if (parameterName == null || parameterName.length() == 0)
                    throw new FlowException("Compensation matrix coefficient name required");

                String value = elCoefficient.getAttributeNS(TRANSFORMATIONS_1_5_NS, "value");
                if (value == null || value.length() == 0)
                    value = elCoefficient.getAttributeNS(TRANSFORMATIONS_2_0_NS, "value");
                if (value == null || value.length() == 0)
                    throw new FlowException("Compensation matrix coefficient value required");

                Double d = Double.valueOf(value);
                mapValues.put(parameterName, d);
            }

            setChannel(channelName, mapValues);
        }
    }

    // Read the GatingML 2.0 xml format
    private void initGatingML_2_0(Element elMatrix)
    {
        throw new UnsupportedOperationException("NYI");
        /*
        try
        {
            XmlOptions options = new XmlOptions();

            SpectrumMatrixType matrix = SpectrumMatrixType.Factory.parse(elMatrix, options);
            matrix.getDetectors();
        }
        catch (XmlException e)
        {
            e.printStackTrace();
        }
        */
    }

    private void init(String strFile)
    {
        String[] lines = StringUtils.split(strFile, "\r\n");
        if (lines.length < 5)
            throw new IllegalArgumentException("Compensation matrix file should be at least 5 lines long");
        _name = lines[0];
        String[] prefixSuffix = StringUtils.split(lines[1], "\t");

        if (prefixSuffix.length == 2)
        {
            _prefix = prefixSuffix[0];
            _suffix = prefixSuffix[1];
        }
        _channelNames = lines[2].split("\t");
        if (_channelNames.length <= 1)
        {
            throw new IllegalArgumentException("Third line of file should contain tab separated channel names");
        }
        _rows = new double[_channelNames.length][];
        for (int iChannel = 0; iChannel < _channelNames.length; iChannel ++)
        {
            int iLine = iChannel + 3;
            String[] values = lines[iLine].split("\t");
            if (values.length != _channelNames.length)
            {
                throw new IllegalArgumentException("Incorrect number of values on line " + iLine + ".  Expected: " + _channelNames.length + " Found:" + values.length);
            }
            _rows[iChannel] = new double[_channelNames.length];
            for (int i = 0; i < values.length; i ++)
            {
                _rows[iChannel][i] = Double.valueOf(values[i]);
            }
        }
    }

    public CompensationMatrix(String name, String prefix, String suffix)
    {
        _name = name;
        _prefix = prefix;
        _suffix = suffix;
    }

    public void setChannel(String channelName, Map<String, Double> channel)
    {
        if (_channelNames == null)
        {
            _channelNames = new String[channel.size()];
            int icol = 0;
            for (Iterator it = channel.keySet().iterator(); it.hasNext(); icol++)
            {
                _channelNames[icol] = (String) it.next();
            }
            _rows = new double[_channelNames.length][];
        }
        int iRow = Arrays.asList(_channelNames).indexOf(channelName);
        double[] row = new double[_channelNames.length];
        for (int i = 0; i < _channelNames.length; i ++)
        {
            row[i] = channel.get(_channelNames[i]);
        }

        _rows[iRow] = row;
    }

    public String getName()
    {
        return _name;
    }

    /**
     * @throws FlowException if a channel required by the compensation matrix is not found in the DataFrame.
     */
    public Matrix getMatrix(DataFrame data) throws FlowException
    {
        Matrix ret = new Matrix(data.getColCount(), data.getColCount());
        for (int i = 0; i < data.getColCount(); i ++)
        {
            ret.set(i, i, 1);
        }
        for (int i = 0; i < _channelNames.length; i ++)
        {
            DataFrame.Field ifield = data.getField(_channelNames[i]);
            if (ifield == null)
                throw new FlowException("Channel '" + _channelNames[i] + "' required for compensation matrix.");
            int irow = ifield.getIndex();
            double[] row = _rows[i];
            for (int j = 0; j < _channelNames.length; j ++)
            {
                DataFrame.Field jfield = data.getField(_channelNames[j]);
                if (jfield == null)
                    throw new FlowException("Channel '" + _channelNames[j] + "' required for compensation matrix.");
                int icol = jfield.getIndex();
                ret.set(icol, irow, row[j]);
            }
        }
        return ret;
    }

    public String[] getChannelNames()
    {
        return _channelNames;
    }

    public boolean hasChannel(String channel)
    {
        for (String channelName : _channelNames)
            if (channelName.equalsIgnoreCase(channel))
                return true;

        return false;
    }

    public double[] getRow(int irow)
    {
        return _rows[irow];
    }

    public DataFrame getCompensatedData(DataFrame data, boolean dither) throws FlowException
    {
        Matrix matrix = getMatrix(data);
        matrix = matrix.inverse();
        if (dither)
            data = data.dither();
        data = data.multiply(matrix);
        return data;
    }

    public DataFrame getCompensatedData(DataFrame data) throws FlowException
    {
        DataFrame comp = getCompensatedData(data, false);
        DataFrame compDithered = comp;
        for (int i = 0; i < data.getColCount(); i ++)
        {
            if (data.getField(i).shouldDither())
            {
                compDithered = getCompensatedData(data, true);
                break;
            }
        }
        int newFieldCount = data.getColCount() + _channelNames.length * 2;
        DataFrame.Field[] fields = new DataFrame.Field[newFieldCount];
        NumberArray[] cols = new NumberArray[newFieldCount];
        for (int i = 0; i < data.getColCount(); i ++)
        {
            fields[i] = data.getField(i);
            cols[i] = data.getColumn(i);
        }
        for (int i = 0; i < _channelNames.length; i ++)
        {
            DataFrame.Field origField = data.getField(_channelNames[i]);
            if (origField == null)
                throw new FlowException("Channel '" + _channelNames[i] + "' required for compensation matrix.");
            DataFrame.Field compField = new DataFrame.Field(data.getColCount() + i, origField, _channelNames[i], _prefix, _suffix);
            DataFrame.Field ditheredField = new DataFrame.Field(data.getColCount() + _channelNames.length + i, origField, _channelNames[i], DITHERED_PREFIX + _prefix, _suffix);
            fields[compField.getIndex()] = compField;
            fields[ditheredField.getIndex()] = ditheredField;
            cols[compField.getIndex()] = comp.getColumn(_channelNames[i]);
            cols[ditheredField.getIndex()] = compDithered.getColumn(_channelNames[i]);
        }
        return new DataFrame(fields, cols);
    }

    public String toString()
    {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < _channelNames.length; i ++)
        {
            ret.append(_channelNames[i] + ":");
            for (int j = 0; j < _channelNames.length; j ++)
            {
                if (j != 0)
                    ret.append(",");
                ret.append(_rows[i][j]);
            }
            ret.append("\n");
        }
        return ret.toString();
    }

    public String formatMatrix(int precision)
    {
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(precision);
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < _channelNames.length; i ++)
        {
            ret.append(_channelNames[i] + ":");
            for (int j = 0; j < _channelNames.length; j ++)
            {
                if (j != 0)
                    ret.append(",");
                ret.append(format.format(_rows[i][j]));
            }
            ret.append("\n");
        }
        return ret.toString();
    }

    public String toExportFormat()
    {
        final String eol = "\r\n";

        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(4);

        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(eol);
        sb.append(_prefix).append("\t").append(_suffix).append(eol);

        String sep = "";
        for (String channelName : _channelNames)
        {
            sb.append(sep).append(channelName);
            sep = "\t";
        }
        sb.append(eol);

        for (int i = 0; i < _channelNames.length; i++)
        {
            sep = "";
            for (int j = 0; j < _channelNames.length; j++)
            {
                sb.append(sep).append(format.format(_rows[i][j]));
                sep = "\t";
            }
            sb.append(eol);
        }

        return sb.toString();
    }

    public Document toXML()
    {
        Document doc;
        try
        {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        }
        catch (ParserConfigurationException e)
        {
            return null;
        }
        doc.appendChild(doc.createElement("CompensationMatrix"));
        Element elRoot = doc.getDocumentElement();
        elRoot.setAttribute("name", _name);
        elRoot.setAttribute("prefix", _prefix);
        elRoot.setAttribute("suffix", _suffix);
        for (int i = 0; i < _channelNames.length; i ++)
        {
            Element elChannel = doc.createElement("Channel");
            elChannel.setAttribute("name", _channelNames[i]);
            for (int j = 0; j < _channelNames.length; j ++)
            {
                Element elChannelValue = doc.createElement("ChannelValue");
                elChannelValue.setAttribute("name", _channelNames[j]);
                elChannelValue.setAttribute("value", Double.toString(_rows[i][j]));
                elChannel.appendChild(doc.createTextNode("\n\t\t"));
                elChannel.appendChild(elChannelValue);
            }
            elChannel.appendChild(doc.createTextNode("\n\t"));
            elRoot.appendChild(doc.createTextNode("\n\t"));
            elRoot.appendChild(elChannel);
            elRoot.appendChild(doc.createTextNode("\n"));
        }
        return doc;
    }

    public void save(OutputStream os) throws Exception
    {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(toXML()), new StreamResult(os));
    }

    public int hashCode()
    {
        int ret = 0;
        for (int i = 0; i < _channelNames.length; i ++)
            ret ^= _channelNames[i].hashCode();
        for (int i = 0; i < _rows.length; i ++)
        {
            for (int j = 0; j < _rows[i].length; j ++)
            {
                ret ^= Double.valueOf(_rows[i][j]).hashCode();
            }
        }
        return ret;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof CompensationMatrix))
            return false;
        CompensationMatrix comp = (CompensationMatrix) other;
        if (!Arrays.equals(_channelNames, comp._channelNames))
            return false;
        for (int i = 0; i < _rows.length; i ++)
        {
            if (!Arrays.equals(_rows[i], comp._rows[i]))
                return false;
        }
        if (!Objects.equals(_name, comp._name))
            return false;
        if (!Objects.equals(_prefix, comp._prefix))
            return false;
        return true;
    }

    /** Returns true if this matrix is singular and therefore not invertible. */
    public boolean isSingular()
    {
        Matrix m = new Matrix(_rows);
        return m.det() == 0;
    }

    /** Returns true if this matrix is the identity matrix. */
    public boolean isIdentity()
    {
        for (int i = 0; i < _rows.length; i++)
        {
            double[] row = _rows[i];
            if (_rows.length != row.length)
                return false;
            for (int j = 0; j < row.length; j++)
            {
                if (_rows[i][j] != (i == j ? 1 : 0))
                    return false;
            }
        }

        return true;
    }

    static public boolean isParamCompensated(String param)
    {
        return param.startsWith(PREFIX);
    }

    // 10,FITC-A,PE-A,ECD-A,PERCP-CY55-A,PE-CY7-A,PACIFIC BLUE-A,AMCYAN-A,APC-A,ALEXA700-A,APC-CY7-A,1,0.3327766322031372,0.08699297487832836,0.008525446763715205,0,0.030025363800367917,0.02979915857838515,0.0036565757271039686,0,0,0,1,0.29986355748731647,0.06143627585576419,0.008331089558338778,0.016684502190259746,0.006805323448541102,0.0021320747409013492,0,0,0,0.16546991509185638,1,0.35973620776132276,0.07313148489024157,0.008247277486239378,0.0033201743601924123,0.0025782639404550063,0,0,0,0,0,1,0.2942568888150039,0.018925486197387757,0.00723083182564879,0.022137246939944292,0.2518111683708703,0.03815142394918861,0.00010326738939971734,0.2697030598543567,0.08293057539597117,0.0205204333759048,1,0.004424313601991271,0.001865910274198225,0.0007811486298667462,0.001664167194771763,0.009672547981150574,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0.009579141973090935,0.0026444048266224965,0.0033388532993441056,0.0004257990187642113,1,0.4972571116470553,0.05013809029413019,0.0008089280838177729,0,0.0004449097690497699,0.024914975224123448,0.013549541639880062,0,0,0.002648227370237401,1,0.09927582503338037,0,0,0,0,0,0,0,0,0,1
    // UNDONE: $SPILL, SPILLOVER, COMP, $COMP
    static public CompensationMatrix fromSpillKeyword(Map<String,String> keywords)
    {
        String spill = StringUtils.trimToNull(keywords.get("SPILL"));

        if (spill == null)
        {
            spill = StringUtils.trimToNull(keywords.get("$SPILLOVER"));
        }

        if (null != spill)
        {
            String[] strings = StringUtils.split(spill, ",");
            if (strings.length == 0)
            {
                // Attempt to parse as space separated list of values
                String[] s = StringUtils.split(spill, " ");
                if (s.length > 0)
                    strings = s;
            }

            int index = 0;
            int channelCount = Integer.parseInt(strings[index ++]);
            String[] channelNames = new String[channelCount];

            for (int i = 0; i < channelCount; i ++)
            {
                channelNames[i] = FCSHeader.cleanParameterName(strings[index++]);
            }
            double[][] rows = new double[channelCount][channelCount];
            for (int i = 0; i < channelCount; i ++)
            {
                for (int j = 0; j < channelCount; j ++)
                {
                    rows[i][j] = Double.parseDouble(strings[index ++]);
                }
            }
            CompensationMatrix ret = new CompensationMatrix("spill");
            ret._channelNames = channelNames;
            ret._rows = rows;
            if (ret.isIdentity())
                return null;
            return ret;
        }

        if (null != StringUtils.trimToNull(keywords.get("$DFC1TO2")))
        {
            // Not sure how I'm supposed to know which parameters to compensate
            // Skip FS, SS, TIME, and take the rest in order
            ArrayList<String> channelNames = new ArrayList<>();
            for (int p=1 ; ; p++)
            {
                String name;
                name = FCSHeader.getParameterName(keywords, p);
                if (null == name)
                    break;
                if (name.equals("FS") || name.startsWith("FS ") || name.equals("SS") || name.startsWith("SS ") || name.startsWith("TIME"))
                    continue;
                channelNames.add(name);
            }
            int channelCount = channelNames.size();
            double[][] rows = new double[channelCount][channelCount];

            // assert that the matrix isn't bigger than we expect
            assert null == StringUtils.trimToNull(keywords.get("$DFC" + (channelCount) + "TO" + (channelCount+1)));
            assert null == StringUtils.trimToNull(keywords.get("$DFC" + (channelCount+1) + "TO" + (channelCount)));

            for (int i=0 ; i<channelCount ; i++)
            {
                for (int j=0 ; j<channelCount; j++)
                {
                    String key = keywords.get("$DFC" + (i+1) + "TO" + (j+1));
                    String dfc = StringUtils.trimToNull(key);
                    if (null == dfc && i==j)
                        dfc = "1";
                    assert dfc != null : "Expected compensation keyword: " + key;
                    rows[i][j] = Double.parseDouble(dfc);
                }
            }

            CompensationMatrix ret = new CompensationMatrix("spill");
            ret._channelNames = channelNames.toArray(new String[channelCount]);
            ret._rows = rows;
            if (ret.isIdentity())
                return null;
            return ret;
        }

        return null;
    }

    public static class TestFCS extends Assert
    {
        @Test
        public void testFcsLoad() throws IOException
        {
            File fcsFile = JunitUtil.getSampleData(null, "flow/FCS/version3_1_spillover.fcs");
            FCS fcs = new FCS(fcsFile);
            CompensationMatrix compensationMatrix = fromSpillKeyword(fcs.getKeywords());
            Assert.assertEquals(compensationMatrix,compensationMatrix);
            String[] expectedNames = {"FITC-A","PE-A","Alexa Fluor 700-A","PerCP-Cy5-5-A","APC-Cy7-A","BV 421-A","BV 510-A"};
            assert compensationMatrix != null;
            Assert.assertArrayEquals(expectedNames,compensationMatrix.getChannelNames());
            Assert.assertEquals(1.0,compensationMatrix.getRow(0)[0],0);
            Assert.assertEquals(0.03153153525207893,compensationMatrix.getRow(0)[1],0);
        }
    }
}
