package org.labkey.flow.analysis.model;

import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.*;

/**
 */
public class FCSHeader
{
    private Map<String, String> keywords = new TreeMap();
    int dataLast;
    int dataOffset;
    int textOffset;
    int textLast;
    int _parameterCount;
    char chDelimiter;
    String version;
    File _file;


    public FCSHeader(File file) throws IOException
    {
        load(file);
    }

    protected FCSHeader()
    {
    }


    protected void load(File file) throws IOException
    {
        _file = file;
        InputStream is = new FileInputStream(file);
        try
        {
            load(is);
        }
        finally
        {
            is.close();
        }
    }

    public File getFile()
    {
        return _file;
    }

    public String getKeyword(String key)
    {
        return keywords.get(key);
    }

    public Map<String, String> getKeywords()
    {
        return Collections.unmodifiableMap(keywords);
    }

    public class TranslationFunction implements ScalingFunction
    {
        double _decade;
        double _scale;
        double _range;
        double _exp;

        public TranslationFunction(double decade, double scale, double range)
        {
            _decade = decade;
            _scale = scale;
            _range = range;
            if (_decade != 0)
            {
                if (_scale == 0)
                    _scale = 1;
                _exp = Math.log(Math.pow(10, _decade)) / _range;
            }
        }

        public boolean isLogarithmic()
        {
            return _decade != 0;
        }

        public double translate(double value)
        {
            if (_decade == 0)
            {
                if (_scale == 0)
                    return value;
                return value * _scale;
            }
            else
            {
                return _scale * Math.exp(value * _exp);
            }
        }

        public double untranslate(double value)
        {
            if (value <= 0)
                return -1;
            if (_decade == 0)
            {
                if (_scale == 0)
                    return value;
                return value / _scale;
            }
            else
            {
                return Math.log(value / _scale) / _exp;
            }
        }

        public double dither(double value)
        {
            if (_decade == 0)
                return -1;
            double rand = Math.random() - .5;
            return value * Math.exp(rand * _exp) * _scale;
        }
    }

    protected void load(InputStream is) throws IOException
    {
        textOffset = 0;
        textLast = 0;
        long cbRead = 0;

        //
        // HEADER
        //
        {
            byte[] headerBuf = new byte[58];
            long read = is.read(headerBuf, 0, headerBuf.length);
            assert read == 58;
            cbRead += read;
            String header = new String(headerBuf);

            version = header.substring(0, 6).trim();
            textOffset = Integer.parseInt(header.substring(10, 18).trim());
            textLast = Integer.parseInt(header.substring(18, 26).trim());
            dataOffset = Integer.parseInt(header.substring(26, 34).trim());
            dataLast = Integer.parseInt(header.substring(34, 42).trim());
//		analysisOffset = Integer.parseInt(header.substring(42,50).trim());
//		analysisLast   = Integer.parseInt(header.substring(50,58).trim());
        }

        //
        // TEXT
        //

        {
            assert cbRead <= textOffset;
            cbRead += is.skip(textOffset - cbRead);
            byte[] textBuf = new byte[(int) (textLast - textOffset + 1)];
            long read = is.read(textBuf, 0, textBuf.length);
            assert read == textBuf.length;
            cbRead += read;
            String fullText = new String(textBuf);
            textBuf = null;
            assert fullText.charAt(0) == fullText.charAt(fullText.length() - 1);
            chDelimiter = fullText.charAt(0);
            int ichStart = 0;
            while (true)
            {
                int ichMid = fullText.indexOf(chDelimiter, ichStart + 1);
                if (ichMid < 0)
                    break;
                int ichEnd = fullText.indexOf(chDelimiter, ichMid + 1);
                if (ichEnd < 0)
                    assert false;
                String strKey = fullText.substring(ichStart + 1, ichMid);
                String strValue = fullText.substring(ichMid + 1, ichEnd);
                keywords.put(strKey, strValue.trim());
                ichStart = ichEnd;
            }
        }
        is.skip(dataOffset - cbRead);
        _parameterCount = Integer.parseInt(getKeyword("$PAR"));
    }

    int getParameterCount()
    {
        return _parameterCount;
    }

    protected DataFrame createDataFrame(float[][] data)
    {
        int count = getParameterCount();
        DataFrame.Field[] fields = new DataFrame.Field[count];
        for (int i = 0; i < count; i++)
        {
            String key = "$P" + (i + 1);
            String name = getKeyword(key + "N");
            double range = Double.parseDouble(getKeyword(key + "R"));
            String E = getKeyword(key + "E");
            double decade = Double.parseDouble(E.substring(0, E.indexOf(',')));
            final double scale = Double.parseDouble(E.substring(E.indexOf(',') + 1));
            DataFrame.Field f = new DataFrame.Field(i, name, (int) range);
            f.setDescription(getKeyword(key + "S"));
            f.setScalingFunction(new TranslationFunction(decade, scale, range));
            fields[i] = f;
        }
        return new DataFrame(fields, data);
    }

    public DataFrame createEmptyDataFrame()
    {
        return createDataFrame(new float[getParameterCount()][0]);
    }
}
