/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.search.AbstractDocumentParser;
import org.labkey.api.search.SearchService;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.Pair;
import org.labkey.api.util.VersionNumber;
import org.labkey.api.webdav.WebdavResource;
import org.labkey.flow.util.KeywordUtil;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 */
public class FCSHeader
{
    public static final String CONTENT_TYPE = "application/vnd.isac.fcs";

    private static final Logger LOG = LogManager.getLogger(FCSHeader.class);

    private Map<String, String> keywords = new CaseInsensitiveHashMap<>();
    int dataLast;
    int dataOffset;
    int textOffset;
    int textLast;
    int _parameterCount;
    char chDelimiter;
    String version;
    VersionNumber _versionNumber;
    File _file;
    CompensationMatrix spillMatrix;


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
        NetworkDrive.ensureDrive(file.getPath());
        try (InputStream is = new FileInputStream(file))
        {
            load(is);
        }
    }

    public File getFile()
    {
        return _file;
    }

    public String getVersion()
    {
        return version;
    }

    @Nullable
    public VersionNumber getVersionNumber()
    {
        return _versionNumber;
    }

    public String getKeyword(String key)
    {
        return keywords.get(key);
    }

    public String getCytometer()
    {
        return getKeyword("$CYT");
    }

    /** Get the i-th parameter name where i is 0-based. */
    public String getParameterName(int i)
    {
        return getParameterName(keywords, i);
    }

    /** Get the i-th parameter description where i is 0-based. */
    public String getParameterDescription(int i)
    {
        return getParameterStain(keywords, i);
    }

    @Nullable
    public Date getDate()
    {
        String dateStr = getKeyword("$DATE");
        if (dateStr != null)
        {
            long date = parseDateTime(dateStr);
            return new Date(date);
        }

        String exportTime = getKeyword("EXPORT TIME");
        if (exportTime != null)
        {
            long date = parseDateTime(exportTime);
            return new Date(date);
        }

        return null;
    }

    @Nullable
    public Date getDateTime()
    {
        String dateStr = getKeyword("$DATE");
        if (dateStr != null)
        {
            long date = DateUtil.parseDate(dateStr);
            long btim = getBeginTime();
            return new Date(date + btim);
        }

        String exportTime = getKeyword("EXPORT TIME");
        if (exportTime != null)
        {
            long date = parseDateTime(exportTime);
            return new Date(date);
        }

        return null;
    }

    public long getBeginTime()
    {
        String btim = getKeyword("$BTIM");
        if (btim == null)
            return 0;

        return parseTime(btim);
    }

    public long getEndTime()
    {
        String etim = getKeyword("$ETIM");
        if (etim == null)
            return 0;

        return parseTime(etim);
    }

    // Issue 38649: flow: parse fractional seconds used in $BTIM keyword
    // turns off strict to allow overflow of jiffy seconds
    private long parseDateTime(String dateStr)
    {
        return DateUtil.parseDateTime(dateStr, DateUtil.MonthDayOption.MONTH_DAY, false, null);
    }

    // Issue 38649: flow: parse fractional seconds used in $BTIM keyword
    // turns off strict to allow overflow of jiffy seconds
    private long parseTime(String dateStr)
    {
        return DateUtil.parseTime(dateStr, false);
    }

    /** Get the duration in seconds. */
    public long getDuration()
    {
        long begin = getBeginTime();
        long end = getEndTime();
        if (begin == 0 || end == 0)
            return 0;

        return (end - begin) / 1000L;
    }


    // Remove "Lin" or "Log" suffix
    public static String cleanParameterName(String name)
    {
        if (name == null)
            return null;

        if (name.endsWith(" Lin") || name.endsWith(" Log"))
            name = name.substring(0, name.length()-4);
        return name;
    }

    /** Get the i-th parameter name where i is 0-based. */
    public static String getParameterName(Map<String, String> keywords, int index)
    {
        return cleanParameterName(StringUtils.trimToNull(keywords.get("$P" + (index+1) + "N")));
    }

    /** Get the i-th parameter stain where i is 0-based. */
    public static String getParameterStain(Map<String, String> keywords, int index)
    {
        return StringUtils.trimToNull(keywords.get("$P" + (index+1) + "S"));
    }

    public int getParameterBitCount(int i)
    {
        return getParameterBitCount(keywords, i);
    }

    public static int getParameterBitCount(Map<String, String> keywords, int i)
    {
        String B = StringUtils.trimToNull(keywords.get("$P" + (i+1) + "B"));
        if (B == null)
            throw new IllegalStateException("$P" + (i+i) + "B keyword is required");

        return Integer.parseInt(B);
    }

    public double getParameterRange(int i)
    {
        return getParameterRange(keywords, i);
    }

    public static double getParameterRange(Map<String, String> keywords, int i)
    {
        String R = StringUtils.trimToNull(keywords.get("$P" + (i+1) + "R"));
        if (R == null)
            throw new IllegalStateException("$P" + (i+i) + "R keyword is required");

        return Double.parseDouble(R);
    }

    public Pair<Double, Double> getParameterLogAmplification(int i)
    {
        String E = getKeyword("$P" + (i+1) + "E");
        if (E == null)
            throw new IllegalStateException("$P" + (i+i) + "E keyword is required");

        double decade = Double.parseDouble(E.substring(0, E.indexOf(',')));
        double scale = Double.parseDouble(E.substring(E.indexOf(',') + 1));
        return Pair.of(decade, scale);
    }

    public double getParameterGain(int i)
    {
        return getParameterGain(keywords, i);
    }

    public static double getParameterGain(Map<String, String> keywords, int i)
    {
        double gain = 1.0;
        String gainStr = StringUtils.trimToNull(keywords.get("$P" + (i+1) + "G"));
        if (gainStr != null)
            gain = Double.parseDouble(gainStr);
        if (gain <= 0)
            gain = 1.0;
        return gain;
    }

    public enum ParameterDisplayType
    {
        Linear {
            @Override
            public ParameterDisplay create(float lower, float upper)
            {
                return new LinearParameterDisplay(lower, upper);
            }
        },
        Logarithmic {
            @Override
            public ParameterDisplay create(float decades, float offset)
            {
                return new LogrithmicParameterDisplay(decades, offset);
            }
        };

        public abstract ParameterDisplay create(float f1, float f2);

        @Nullable
        public static ParameterDisplayType fromDisplayParameter(@Nullable String display)
        {
            if (display == null)
                return null;

            if (display.equalsIgnoreCase("lin") || display.equalsIgnoreCase("linear"))
                return ParameterDisplayType.Linear;

            if (display.equalsIgnoreCase("log") || display.equalsIgnoreCase("logarithmic"))
                return ParameterDisplayType.Logarithmic;

            return null;
        }
    }

    public abstract static class ParameterDisplay
    {
        protected final float _f1;
        protected final float _f2;

        public abstract ParameterDisplayType getType();

        protected ParameterDisplay(float f1, float f2)
        {
            _f1 = f1;
            _f2 = f2;
        }
    }

    public static class LinearParameterDisplay extends ParameterDisplay
    {
        private LinearParameterDisplay(float lower, float upper)
        {
            super(lower, upper);
        }

        @Override
        public ParameterDisplayType getType() { return ParameterDisplayType.Linear; }

        public float getLowerBound() { return _f1; }
        public float getUpperBound() { return _f2; }
    }

    public static class LogrithmicParameterDisplay extends ParameterDisplay
    {
        private LogrithmicParameterDisplay(float decades, float offset)
        {
            super(decades, offset);
        }

        @Override
        public ParameterDisplayType getType() { return ParameterDisplayType.Logarithmic; }

        public float getDecades() { return _f1; }
        public float getOffset() { return _f2; }
    }

    /**
     * Determine the parameter display from the "$PnD" or "PnDISPLAY" keywords.
     */
    @Nullable
    public ParameterDisplay getParameterDisplay(int index)
    {
        String pStr = StringUtils.trimToNull(keywords.get("$P" + (index+1) + "D"));
        if (pStr == null)
            pStr = getParameterLegacyDisplay(keywords, index);
        if (pStr == null)
            return null;

        String[] parts = pStr.split(",");
        if (parts.length == 0)
            return null;

        ParameterDisplayType type = ParameterDisplayType.fromDisplayParameter(parts[0]);
        if (type == ParameterDisplayType.Linear)
        {
            float lower = 0.0f;
            float upper = (float) getParameterRange(index);
            if (parts.length > 2)
            {
                lower = Float.parseFloat(parts[1]);
                upper = Float.parseFloat(parts[2]);
            }
            return new LinearParameterDisplay(lower, upper);
        }
        else if (type == ParameterDisplayType.Logarithmic)
        {
            if (parts.length > 2)
            {
                float decades = Float.parseFloat(parts[1]);
                float offset = Float.parseFloat(parts[2]);
                return new LogrithmicParameterDisplay(decades, offset);
            }
            else
            {
                // unsure about what to use for the log display
                return new LogrithmicParameterDisplay(5, 1);
            }
        }

        return null;
    }


    /** Get the i-th parameter "PnDISPLAY" keyword, where i is 0-based. */
    public String getParameterLegacyDisplay(int index)
    {
        return getParameterLegacyDisplay(keywords, index);
    }

    /** Get the i-th parameter "PnDISPLAY" keyword, where i is 0-based. */
    public static String getParameterLegacyDisplay(Map<String, String> keywords, int index)
    {
        String s = StringUtils.trimToNull(keywords.get("P" + (index+1) + "DISPLAY"));
        if (s != null)
            return s;
        return StringUtils.trimToNull(keywords.get((index+1) + "DISPLAY"));
    }

    // UNDONE: $SPILL, SPILLOVER, COMP, $COMP
    public static boolean hasSpillKeyword(Map<String, String> keywords)
    {
        return KeywordUtil.hasSpillKeyword(keywords);
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
            if (version.startsWith("FCS"))
                _versionNumber = new VersionNumber(version.substring(3));
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
                // FCS format encodes empty keyword values as a single space character -- convert it to null.
                strValue = strValue.trim();
                if (strValue.length() == 0)
                    strValue = null;
                if (keywords.containsKey(strKey))
                    LOG.warn("Duplicate key '" + strKey + "'");
                keywords.put(strKey, strValue);
                ichStart = ichEnd;
            }
        }

        if (dataOffset == 0)
            try {dataOffset = Integer.parseInt(keywords.get("$BEGINDATA"));}catch(Exception x){}
        if (dataLast == 0)
            try {dataLast = Integer.parseInt(keywords.get("$ENDDATA"));}catch(Exception x){}
        if (dataOffset != 0)
            is.skip(dataOffset - cbRead);
        try {_parameterCount = Integer.parseInt(keywords.get("$PAR"));}catch(Exception x){}
    }


    int getParameterCount()
    {
        return _parameterCount;
    }


    protected DataFrame createDataFrame(float[][] data, int[] bitCounts)
    {
        boolean datatypeI = "I".equals(getKeyword("$DATATYPE"));
        boolean facsCalibur = "FACSCalibur".equals(getCytometer());

        CompensationMatrix comp = getSpill();
        int count = getParameterCount();
        DataFrame.Field[] fields = new DataFrame.Field[count];
        for (int i = 0; i < count; i++)
        {
            String name = getParameterName(i);
            int bits = bitCounts != null ? bitCounts[i] : getParameterBitCount(i);
            double range = getParameterRange(i);

            var E = getParameterLogAmplification(i);
            double decade = E.first;
            double scale = E.second;
            if (scale <= 0)
                scale = 1;

            // Gain linear amplifier
            double gain = getParameterGain(i);

            boolean simpleLog = false;
            
            // By default we use either linear, or a modified log but in some cases we use simple log for FlowJo compatibility.
            if (0 != decade)
            {
                // Use simple log if the range is <4096 and bits is <=32.
                // This is a legacy behavior of FlowJo due to an internal representation of bins
                if (range < 4096 && bits <= 32)
                    simpleLog = true;

                // Use simple log for non-compensated integer data.
                // We're just going to assume the fcs data is non-compensated.
                if (datatypeI && facsCalibur)
                    simpleLog = true;
            }
            else if (KeywordUtil.isTimeChannel(name) && scale == 1.0 && gain != 0.0)
            {
                scale = gain;
            }

            ParameterDisplay display = getParameterDisplay(i);

            DataFrame.Field f = new DataFrame.Field(i, name, (int) range);
            f.setDescription(getParameterDescription(i));
            f.setScalingFunction(ScalingFunction.makeFunction(decade, scale, range));
            f.setParameterDisplay(display);
            f.setSimpleLogAxis(simpleLog);

            if (datatypeI)
                f.setDither(true);

            // Create aliases for the field name including compensated aliases if needed.
            boolean precompensated = comp != null && comp.hasChannel(name);
            f.initAliases(precompensated);

            fields[i] = f;
        }
        return new DataFrame(fields, data);
    }

    public DataFrame createEmptyDataFrame()
    {
        return createDataFrame(new float[getParameterCount()][0], null);
    }

    /** Returns true if the sample has already been compensated by the flow cytometer. */
    // Is it possible to have uncompensated data while still having a $SPILL matrix?
    public boolean isPrecompensated()
    {
        return getSpill() != null;
    }

    /** Get the spill matrix from the $SPILL keyword or $DFCnTOm keywords if it is non-identity. */
    public CompensationMatrix getSpill()
    {
        if (spillMatrix == null)
            spillMatrix = CompensationMatrix.fromSpillKeyword(getKeywords());

        return spillMatrix;
    }

    //
    // DocumentParser , tika like methods (w/o the imports)
    //
    public static SearchService.DocumentParser documentParser = new AbstractDocumentParser()
    {
        @Override
        public String getMediaType()
        {
            return "application/fcs";
        }

        @Override
        public boolean detect(WebdavResource resource, String contentType, byte[] buf)
        {
            if (contentType != null)
            {
                contentType = contentType.toLowerCase().trim();
                if (contentType.equals(CONTENT_TYPE))
                    return true;
            }

            if (buf.length < 58)
                return false;

            if (!FCS.isSupportedVersion(buf))
                return false;

            String header = new String(buf, 0, 58);

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

        @Override
        public void parseContent(InputStream stream, ContentHandler h) throws IOException, SAXException
        {
            StringBuilder sb = new StringBuilder(1000);
            char[] buf = new char[1000];

            FCSHeader loader = new FCSHeader();
            loader.load(stream);
            Map<String,String> keywords = loader.keywords;

            // TODO: Metadata (TITLE, DATE)

            String expName = keywords.get("EXPERIMENT NAME");
            if (!StringUtils.isEmpty(expName))
            {
                sb.append(expName).append("\n");
                write(h, sb, buf);
            }

            for (Map.Entry<String,String> e : keywords.entrySet())
            {
                String k = e.getKey();
                String v = e.getValue();
                if (KeywordUtil.isHidden(k))
                    continue;
                sb.setLength(0);
                sb.append(k).append(" ").append(v).append("\n");
                write(h,sb,buf);
            }
        }
    };


}
