/*
 * Copyright (c) 2005-2010 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.labkey.api.search.SearchService;
import org.labkey.api.util.NetworkDrive;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 */
public class FCSHeader
{
    private Map<String, String> keywords = new TreeMap<String,  String>();
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

    public FCSHeader()
    {
    }


    protected void load(File file) throws IOException
    {
        _file = file;
        NetworkDrive.ensureDrive(file.getPath());
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
            byte[] textBuf = new byte[(textLast - textOffset + 1)];
            long read = is.read(textBuf, 0, textBuf.length);
            assert read == textBuf.length;
            cbRead += read;
            String fullText = new String(textBuf);
//            assert fullText.charAt(0) == fullText.charAt(fullText.length() - 1);
            chDelimiter = fullText.charAt(0);
            int ichStart = 0;
            while (true)
            {
                int ichMid = fullText.indexOf(chDelimiter, ichStart + 1);
                if (ichMid < 0)
                    break;
                int ichEnd = fullText.indexOf(chDelimiter, ichMid + 1);
                if (ichEnd < 0)
                {
//                    assert false;
                    ichEnd = fullText.length();
                }
                String strKey = fullText.substring(ichStart + 1, ichMid);
                String strValue = fullText.substring(ichMid + 1, ichEnd);
                keywords.put(strKey, strValue.trim());
                ichStart = ichEnd;
            }
        }

        if (dataOffset == 0)
            try {dataOffset = Integer.parseInt(keywords.get("$BEGINDATA"));}catch(Exception x){}
        if (dataLast == 0)
            try {dataLast = Integer.parseInt(keywords.get("$ENDDATA"));}catch(Exception x){}
        if (dataOffset != 0)
            is.skip(dataOffset - cbRead);
        _parameterCount = Integer.parseInt(getKeyword("$PAR"));
    }


    int getParameterCount()
    {
        return _parameterCount;
    }


    protected DataFrame createDataFrame(float[][] data)
    {
        // by default we use either linear, or a modified log
        // to be compatible with flowjo we use simple log for non-compensated integer data
        // we're just going to assume the fcs data is non-compensated
        boolean datatypeI = "I".equals(getKeyword("$DATATYPE"));
        boolean facsCalibur = "FACSCalibur".equals(getKeyword("$CYT"));

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
            f.setScalingFunction(ScalingFunction.makeFunction(decade, scale, range));
            if (datatypeI && facsCalibur && 0 != decade)
                f.setSimpleLogAxis(true);
            if (datatypeI)
                f.setDither(true);
            fields[i] = f;
        }
        return new DataFrame(fields, data);
    }


    public DataFrame createEmptyDataFrame()
    {
        return createDataFrame(new float[getParameterCount()][0]);
    }




    //
    // DocumentParser , tika like methods (w/o the imports)
    //
    public static SearchService.DocumentParser documentParser = new SearchService.DocumentParser()
    {
        public String getMediaType()
        {
            return "application/fcs";
        }

        public boolean detect(byte[] buf) throws IOException
        {
            if (buf.length < 58)
                return false;
            String header = new String(buf, 0, 58);

            if (!header.startsWith("FCS2.0") && !header.startsWith("FCS3.0"))
                return false;

            try
            {
                //String version = header.substring(0, 6).trim();
                int textOffset = Integer.parseInt(header.substring(10, 18).trim());
                int textLast = Integer.parseInt(header.substring(18, 26).trim());
                int dataOffset = Integer.parseInt(header.substring(26, 34).trim());
                int dataLast = Integer.parseInt(header.substring(34, 42).trim());
                return true;
            }
            catch (NumberFormatException x)
            {
                return false;
            }
        }

        public void parse(InputStream stream, ContentHandler h) throws IOException, SAXException
        {
            StringBuilder sb = new StringBuilder(1000);
            char[] buf = new char[1000];

            FCSHeader loader = new FCSHeader();
            loader.load(stream);
            Map<String,String> keywords = loader.keywords;

            _start(h,"html");
            _start(h,"body");
            _start(h,"pre");

            // TODO: Metadata (TITLE, DATE)

            String expName = keywords.get("EXPERIMENT NAME");
            if (!StringUtils.isEmpty(expName))
            {
                sb.append(expName).append("\n");
                _write(h, sb, buf);
            }

            for (Map.Entry<String,String> e : keywords.entrySet())
            {
                String k = e.getKey();
                String v = e.getValue();
                if (k.startsWith("$"))
                    continue;
                if (k.startsWith("P") && k.endsWith("DISPLAY"))
                    continue;
                if (k.equals("SPILL"))
                    continue;
                sb.setLength(0);
                sb.append(k).append(" ").append(v).append("\n");
                _write(h,sb,buf);
            }

            _end(h,"pre");
            _end(h,"body");
            _end(h,"html");
        }


        private void _start(ContentHandler h, String tag) throws SAXException
        {
            h.startElement("http://www.w3.org/1999/xhtml", tag, tag, emptyAttributes);
        }

        private void _end(ContentHandler h, String tag) throws SAXException
        {
            h.endElement("http://www.w3.org/1999/xhtml", tag, tag);
        }

        private void _write(ContentHandler h, StringBuilder sb, char[] buf) throws SAXException
        {
            int len = Math.min(sb.length(),buf.length);
            sb.getChars(0, len, buf, 0);
            h.characters(buf,0,len);
        }
    };


    static Attributes emptyAttributes = new Attributes()
    {
        public int getLength()
        {
            return 0;
        }

        public String getURI(int i)
        {
            return null;
        }

        public String getLocalName(int i)
        {
            return null;
        }

        public String getQName(int i)
        {
            return null;
        }

        public String getType(int i)
        {
            return null;
        }

        public String getValue(int i)
        {
            return null;
        }

        public int getIndex(String s, String s1)
        {
            return 0;
        }

        public int getIndex(String s)
        {
            return 0;
        }

        public String getType(String s, String s1)
        {
            return null;
        }

        public String getType(String s)
        {
            return null;
        }

        public String getValue(String s, String s1)
        {
            return null;
        }

        public String getValue(String s)
        {
            return null;
        }
    };
}
