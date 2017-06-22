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

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.util.StringUtilsLabKey;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: mbellew
 * Date: Apr 26, 2005
 * Time: 3:19:22 PM
 */
public class FCS extends FCSHeader
{
    static private final Logger _log = Logger.getLogger(FCS.class);
    boolean bigEndian;
    DataFrame rawData;
    public static List<String> supportedVersions = new ArrayList<>(Arrays.asList("FCS2.0","FCS3.0","FCS3.1"));

    protected FCS() { }

    public FCS(File f) throws IOException
    {
        load(f);
    }

    protected void load(InputStream is) throws IOException
    {
        super.load(is);
        //
        // METADATA
        //
        String byteOrder = StringUtils.trimToEmpty(getKeyword("$BYTEORD"));
        if ("4,3,2,1".equals(byteOrder) || "2,1".equals(byteOrder))
            bigEndian = true;
        else if ("1,2,3,4".equals(byteOrder) || "1,2".equals(byteOrder))
            bigEndian = false;
        else
        {
            // App.setMessage("$BYTEORD not specified assuming big endian.");
        }

        int eventCount = Integer.parseInt(getKeyword("$TOT"));
        int count = getParameterCount();

        //
        // PARAMETERS
        //
        int[] bitCounts = new int[count];
        boolean packed = false;
        String datatype = getKeyword("$DATATYPE");
        for (int i = 0; i < count; i++)
        {
            String key = "$P" + (i + 1);
            if (null != getKeyword(key + "B"))
            {
                int b = Integer.parseInt(getKeyword(key + "B"));
                if (datatype.equals("A"))
                {
                    bitCounts[i] = b * 8;
                }
                else
                {
                    bitCounts[i] = b;
                    if (b % 8 != 0)
                    {
                        packed = true;
                    }
                }
            }
        }

        // if metadata indicates packed data, verify that the data length is consistent
        // if it is not consistent, fall back to unpacked and fixup bitcounts
        if (packed)
        {
            int expectedLength = expectedDataLengthIntegerPacked(bitCounts, eventCount);
            int dataLength = (dataLast - dataOffset + 1);
            if (expectedLength != dataLength)
            {
                packed = false;
                for (int i=0 ; i<bitCounts.length ; i++)
                    bitCounts[i] = ((bitCounts[i]+7)/8)*8;
            }
        }

        float[][] data = new float[count][eventCount];
        DataFrame frame = createDataFrame(data, bitCounts);

        //
        // DATA
        //
        {
            if ("L".equals(getKeyword("$MODE")))
            {
                switch (datatype.charAt(0))
                {
                    case'I':
                    {
                        if (packed)
                        {
                            readListDataIntegerPacked(is, bitCounts, data);
                        }
                        else
                        {
                            readListDataInteger(is, bitCounts, data);
                        }
                        break;
                    }
                    case'F':
                    {
                        readListDataFloat(is, bitCounts, data);
                        break;
                    }
                    case'D':
                    {
                        throw new java.lang.UnsupportedOperationException("Double data not supported");
                    }
                    case'A':
                    {
                        throw new java.lang.UnsupportedOperationException("ASCII data not supported");
                    }
                }
            }
            else
            {
                throw new java.lang.UnsupportedOperationException("only supports ListMode");
            }


            this.rawData = frame;
        }
    }


    void readListDataInteger(InputStream is, int[] bitCounts, float[][] data) throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(is, 1024*1024);
        int rowBitLength = 0;
        for (int p = 0; p < bitCounts.length; p++)
            rowBitLength += bitCounts[p];
        int rowLength = rowBitLength/8;
        byte[] dataBuf = new byte[rowLength];

        for (int row = 0; row < data[0].length; row++)
        {
            int r = bis.read(dataBuf);
            if (r < dataBuf.length)
                throw new IOException("unexpected EOF");

            int ib = 0;
            for (int p = 0; p < bitCounts.length; p++)
            {
                int value = 0;
                switch (bitCounts[p])
                {
                    case 8:
                        value = toInt(dataBuf[ib++]);
                        break;
                    case 16:
                        value = toInt(dataBuf[ib++], dataBuf[ib++]);
                        break;
                    case 32:
                        value = toInt(dataBuf[ib++], dataBuf[ib++], dataBuf[ib++], dataBuf[ib++]);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported bit count: " + bitCounts[p]);
                }
                data[p][row] = (float) value;
            }
        }
    }


    int expectedDataLengthIntegerPacked(int[] bitCounts, int events)
    {
        int bitsPerRow = 0;
        for (int i = 0; i < bitCounts.length; i++)
            bitsPerRow += bitCounts[i];
        int expectedBytes = (events * bitsPerRow + 7) / 8;
        return expectedBytes;
    }


    void readListDataIntegerPacked(InputStream is, int[] bitCounts, float[][] data) throws IOException
    {
        byte[] dataBuf = new byte[(dataLast - dataOffset + 1)];
        long read = is.read(dataBuf, 0, dataBuf.length);
        assert read == dataBuf.length;
        int expectedBytes = expectedDataLengthIntegerPacked(bitCounts, data[0].length);
        if (expectedBytes != read)
        {
            throw new IllegalArgumentException("dataBuf is of length " + dataBuf.length + " expected " + expectedBytes);
        }

        int bitOffset = 0;
        for (int row = 0; row < data[0].length; row++)
        {
            for (int p = 0; p < bitCounts.length; p++)
            {
                int value = toIntPacked(dataBuf, bitOffset, bitCounts[p]);
                bitOffset += bitCounts[p];
                data[p][row] = (float) value;
            }
        }
    }

    void readListDataFloat(InputStream is, int[] bitCounts, float[][] data) throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(is, 1024*1024);
        int rowBitLength = 0;
        for (int p = 0; p < bitCounts.length; p++)
            rowBitLength += bitCounts[p];
        int rowLength = rowBitLength/8;
        byte[] dataBuf = new byte[rowLength];

        for (int row = 0; row < data[0].length; row++)
        {
            int r = bis.read(dataBuf);
            if (r < dataBuf.length)
                throw new IOException("unexpected EOF");

            int ib = 0;
            for (int p = 0; p < bitCounts.length; p++)
            {
                float value = 0;
                switch (bitCounts[p])
                {
                    case 32:
                        int intValue = toInt(dataBuf[ib], dataBuf[ib + 1], dataBuf[ib + 2], dataBuf[ib + 3]);
                        ib += 4;
                        value = Float.intBitsToFloat(intValue);
                        break;
                    default:
                        throw new IllegalArgumentException("Only 32 bit floating point numbers are supported: " + bitCounts[p]);
                }
                data[p][row] = value;
            }
        }
    }

    protected final int toIntPacked(byte[] bytes, int bitOffset, int bitCount)
    {
        int ret = 0;
        int bitsRemain = bitCount;
        while (bitsRemain > 0)
        {
            int curByte = bytes[bitOffset / 8];
            int bitsConsumed = Math.min(bitsRemain, (((7 - bitOffset) % 8 + 8) % 8) + 1);
            int mask = ((1 << bitsConsumed) - 1) << (bitOffset % 8);
            int curValue;
            curValue = (curByte & mask) >> (bitOffset % 8);
            curValue = curValue << (bitCount - bitsRemain);
            bitOffset += bitsConsumed;
            bitsRemain -= bitsConsumed;
            ret += curValue;
        }
        return ret;
    }

    protected final int toInt(byte a)
    {
        return unsigned(a);
    }

    protected final int toInt(byte a, byte b)
    {
        if (bigEndian)
            return unsigned(a) * 256 + unsigned(b);
        else
            return unsigned(b) * 256 + unsigned(a);
    }

    protected final int toInt(byte a, byte b, byte c, byte d)
    {
        int value;
        if (bigEndian)
        {
            value = unsigned(a);
            value = value * 256 + unsigned(b);
            value = value * 256 + unsigned(c);
            value = value * 256 + unsigned(d);
        }
        else
        {
            value = unsigned(d);
            value = value * 256 + unsigned(c);
            value = value * 256 + unsigned(b);
            value = value * 256 + unsigned(a);
        }
        return value;
    }

    protected final int unsigned(byte b)
    {
        return ((int) b) & 0x000000ff;
    }

    public DataFrame getScaledData(ScriptSettings settings)
    {
        return rawData.translate(settings);
    }


    static public FcsFileFilter FCSFILTER = new FcsFileFilter();

    /**
     * assume that any .FCS .FACS or .LMD file is an FCS file
     * assume any other extension is NOT an FCS file
     * if no extension (or numeric extension) inspect the file
     */
    static class FcsFileFilter implements IOFileFilter
    {
        private FcsFileFilter() {}

        public boolean accept(File file)
        {
            int i;
            if (-1 != (i= file.getName().lastIndexOf(".")))
            {
                String ext = file.getName().substring(i).toLowerCase();
                if (ext.equals(".fcs") || ext.equals(".facs") || ext.equals(".lmd"))
					return true;

				// fall through if this look like a bogus numeric extension e.g. .001
				try
				{
					Integer.parseInt(ext.substring(1));
				}
				catch (NumberFormatException x)
				{
					return false;
				}
	        }
			return isFCSFile(file);
        }

        public boolean accept(File dir, String name)
        {
            int i;
            if (-1 != (i= name.indexOf(".")))
            {
                String ext = name.substring(i).toLowerCase();
                return ext.equals(".fcs") || ext.equals(".facs") || ext.equals(".lmd");
            }
            else
                return isFCSFile(new File(dir,name));
        }
    }

    
    static public boolean isFCSFile(File file)
    {
        try
        {
            if (!file.isFile())
            {
                // Don't read from the File if it is "/dev/ttyp0"
                return false;
            }
            byte[] compare1 = new byte[]{'F', 'C', 'S'};

            if (!Arrays.equals(readFirstBytes(file, 3), compare1))
                return false;
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static boolean isSupportedVersion(File file)
    {
        try
        {
            return isSupportedVersion(readFirstBytes(file, 6));
        }
        catch (IOException e)
        {
            return false;
        }
    }

    private static byte[] readFirstBytes(File file , int len) throws IOException
    {
        InputStream stream = null;
        try
        {
            stream = new FileInputStream(file);

            byte[] bytes = new byte[len];
            stream.read(bytes);
            return bytes;
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException e)
                {
                }
            }
        }

    }

    public static String getFcsVersion(File file) throws IOException
    {
        InputStream stream = null;
        String errorMessage = "Unable to read version.";
        try
        {
            stream = new FileInputStream(file);

            byte[] buffer = new byte[6];

            if (stream.read(buffer) != 6)
            {
                return errorMessage;
            }

            return new String(buffer, 0, 6);
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException e)
                {
                }
            }
        }

    }

    public static boolean isSupportedVersion(byte[] buffer)
    {
        String version = new String(buffer, 0, 6);
        return supportedVersions.contains(version);
    }


    private int indexOf(byte[] buf, byte[] key, int min, int max)
    {
        outer:
        for (int i = min; i < max - key.length; i++)
        {
            for (int j = 0; j < key.length; j++)
            {
                if (buf[i + j] != key[j])
                {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private int indexOf(byte[] buf, byte key, int start, int end)
    {
        return indexOf(buf, new byte[]{key}, start, end);
    }

    public byte[] getFCSBytes(File file, int maxEventCount) throws IOException
    {
        int oldEventCount = rawData.getRowCount();
        int newEventCount = Math.min(maxEventCount, oldEventCount);
        int oldByteCount = (dataLast - dataOffset + 1);
        int newSize = (int) (dataOffset + (((long) oldByteCount * (long) newEventCount) + oldEventCount - 1) / oldEventCount);
        byte[] bytes = new byte[newSize];
        InputStream is = new FileInputStream(file);
        is.read(bytes);
        int newDataLast = newSize - 1;
        String strDataLast = Integer.toString(newDataLast);
        byte[] rgbDataLast = strDataLast.getBytes(StringUtilsLabKey.DEFAULT_CHARSET);
        // fill the dataLast with spaces
        Arrays.fill(bytes, 34, 42, (byte) 32);
        System.arraycopy(rgbDataLast, 0, bytes, 34 + 8 - rgbDataLast.length, rgbDataLast.length);

        // set $ENDDATA keyword value to new data last offset
        byte[] rgbEndDataKey = new byte[]{(byte) chDelimiter, '$', 'E', 'N', 'D', 'D', 'A', 'T', 'A', (byte) chDelimiter};
        int ibEndData = indexOf(bytes, rgbEndDataKey, textOffset, textLast);
        if (ibEndData >= 0)
        {
            int ibNumberStart = ibEndData + rgbEndDataKey.length;
            int ibNumberEnd = indexOf(bytes, new byte[] {(byte) chDelimiter}, ibNumberStart, textLast + 1);
            if (ibNumberEnd > 0)
            {
                Arrays.fill(bytes, ibNumberStart, ibNumberEnd, (byte) 32);
                System.arraycopy(rgbDataLast, 0, bytes, ibNumberStart, rgbDataLast.length);
            }
        }

        // now, look for $TOT in the file
        byte[] rgbTotKey = new byte[]{(byte) chDelimiter, '$', 'T', 'O', 'T', (byte) chDelimiter};
        int ibTot = indexOf(bytes, rgbTotKey, textOffset, textLast);
        if (ibTot >= 0)
        {
            int ibNumberStart = ibTot + rgbTotKey.length;
            int ibNumberEnd = indexOf(bytes, new byte[]{(byte) chDelimiter}, ibNumberStart, textLast + 1);
            if (ibNumberEnd > 0)
            {
                Arrays.fill(bytes, ibNumberStart, ibNumberEnd, (byte) 32);
                String strNewTot = Integer.toString(newEventCount);
                byte[] rgbNewTot = strNewTot.getBytes("UTF-8");
                System.arraycopy(rgbNewTot, 0, bytes, ibNumberStart, rgbNewTot.length);
            }
        }

        // replace $NEXTDATA with "0"
        byte[] rgbNextDataKey = new byte[]{(byte) chDelimiter, '$', 'N', 'E', 'X', 'T', 'D', 'A', 'T', 'A', (byte) chDelimiter};
        int ibNextData = indexOf(bytes, rgbNextDataKey, textOffset, textLast);
        if (ibNextData >= 0)
        {
            int ibNumberStart = ibNextData + rgbNextDataKey.length;
            int ibNumberEnd = indexOf(bytes, new byte[]{(byte) chDelimiter}, ibNumberStart, textLast + 1);
            if (ibNumberEnd > 0)
            {
                Arrays.fill(bytes, ibNumberStart, ibNumberEnd, (byte) 32);
                byte[] rgbNewNextData = "0".getBytes(StringUtilsLabKey.DEFAULT_CHARSET);
                System.arraycopy(rgbNewNextData, 0, bytes, ibNumberStart, rgbNewNextData.length);
            }
        }

        return bytes;
    }
}
