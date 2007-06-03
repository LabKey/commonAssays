package org.labkey.flow.analysis.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ObjectUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.util.*;
import java.text.DecimalFormat;
import java.io.*;

import Jama.Matrix;

import org.labkey.flow.analysis.data.NumberArray;

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
        this (new FileInputStream(file));
    }

    public CompensationMatrix(InputStream is) throws Exception
    {
        String strContents = IOUtils.toString(is);
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
        _name = elMatrix.getAttribute("name");
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
            HashMap mapValues = new HashMap();
            NodeList nlChannelValues = elChannel.getChildNodes();
            for (int iValue = 0; iValue < nlChannelValues.getLength(); iValue ++)
            {
                if (!(nlChannelValues.item(iValue) instanceof Element))
                    continue;
                Element elChannelValue = (Element) nlChannelValues.item(iValue);
                mapValues.put(elChannelValue.getAttribute("name"), new Double(elChannelValue.getAttribute("value")));
            }
            setChannel(elChannel.getAttribute("name"), mapValues);
        }
    }

    private void init(String strFile)
    {
        String[] lines = StringUtils.split(strFile, "\r\n");
        if (lines.length < 5)
            throw new IllegalArgumentException("Compensation matrix file should be at least 5 lines long");
        String name = lines[0];
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

    public Matrix getMatrix(DataFrame data)
    {
        Matrix ret = new Matrix(data.getColCount(), data.getColCount());
        for (int i = 0; i < data.getColCount(); i ++)
        {
            ret.set(i, i, 1);
        }
        for (int i = 0; i < _channelNames.length; i ++)
        {
            int irow = data.getField(_channelNames[i]).getIndex();
            double[] row = _rows[i];
            for (int j = 0; j < _channelNames.length; j ++)
            {
                int icol = data.getField(_channelNames[j]).getIndex();
                ret.set(icol, irow, row[j]);
            }
        }
        return ret;
    }

    public String[] getChannelNames()
    {
        return _channelNames;
    }

    public double[] getRow(int irow)
    {
        return _rows[irow];
    }

    public DataFrame getCompensatedData(DataFrame data, boolean dither)
    {
        Matrix matrix = getMatrix(data);
        matrix = matrix.inverse();
        if (dither)
            data = data.dither();
        data = data.multiply(matrix);
        return data;
    }

    public DataFrame getCompensatedData(DataFrame data)
    {
        DataFrame comp = getCompensatedData(data, false);
        DataFrame compDithered = getCompensatedData(data, true);
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
            DataFrame.Field compField = new DataFrame.Field(data.getColCount() + i, origField, _prefix + _channelNames[i] + _suffix);
            DataFrame.Field ditheredField = new DataFrame.Field(data.getColCount() + _channelNames.length + i, origField, DITHERED_PREFIX + _prefix + _channelNames[i] + _suffix);
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
        if (!ObjectUtils.equals(_name, comp._name))
            return false;
        if (!ObjectUtils.equals(_prefix, comp._prefix))
            return false;
        return true;
    }

    public boolean isSingular()
    {
        Matrix m = new Matrix(_rows);
        return m.det() == 0;
    }

    static public boolean isParamCompensated(String param)
    {
        return param.startsWith(PREFIX);
    }
}
