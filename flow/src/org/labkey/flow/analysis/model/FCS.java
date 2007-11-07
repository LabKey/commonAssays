/*
 * Copyright (C) 2005 LabKey LLC. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.labkey.flow.analysis.model;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: mbellew
 * Date: Apr 26, 2005
 * Time: 3:19:22 PM
 */
public class FCS extends FCSHeader
{
    static private final Logger _log = Logger.getLogger(FCS.class);
    boolean bigEndian;
    DataFrame rawData;

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
        String byteOrder = getKeyword("$BYTEORD");
        if ("4,3,2,1".equals(byteOrder))
            bigEndian = true;
        else if ("1,2,3,4".equals(byteOrder))
            bigEndian = false;
        else
        {
            // App.setMessage("$BYTEORD not specified assuming big endian.");
        }

        int eventCount = Integer.parseInt(getKeyword("$TOT"));
        int count = getParameterCount();
        float[][] data = new float[count][eventCount];
        DataFrame frame = createDataFrame(data);
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

        //
        // DATA
        //
        {
            byte[] dataBuf = new byte[(dataLast - dataOffset + 1)];
            long read = is.read(dataBuf, 0, dataBuf.length);
            assert read == dataBuf.length;
            int expectedCount = eventCount;
            int bitsPerRow = 0;
            for (int i = 0; i < bitCounts.length; i++)
                bitsPerRow += bitCounts[i];
            int expectedBytes = (expectedCount * bitsPerRow + 7) / 8;
            if (expectedBytes != dataBuf.length)
            {
                throw new IllegalArgumentException("dataBuf is of length " + dataBuf.length + " expected " + expectedBytes);
            }
            if ("L".equals(getKeyword("$MODE")))
            {
                switch (datatype.charAt(0))
                {
                    case'I':
                        if (packed)
                        {
                            readListDataIntegerPacked(dataBuf, bitCounts, data);
                        }
                        else
                        {
                            readListDataInteger(dataBuf, bitCounts, data);
                        }
                        break;
                    case'F':
                        readListDataFloat(dataBuf, bitCounts, data);
                        break;
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

    void readListDataInteger(byte[] dataBuf, int[] bitCounts, float[][] data)
    {
        int ib = 0;
        for (int row = 0; row < data[0].length; row++)
        {
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

    void readListDataIntegerPacked(byte[] dataBuf, int[] bitCounts, float[][] data)
    {
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

    void readListDataFloat(byte[] dataBuf, int[] bitCounts, float[][] data)
    {
        int ib = 0;
        for (int row = 0; row < data[0].length; row++)
        {
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

    protected int unsigned(byte b)
    {
        return ((int) b) & 0x000000ff;
    }

    public DataFrame getScaledData(ScriptSettings settings)
    {
        return rawData.translate(settings);
    }


    static public FcsFileFilter FCSFILTER = new FcsFileFilter();

    static class FcsFileFilter implements IOFileFilter
    {
        private FcsFileFilter() {}

        public boolean accept(File file)
        {
            int i;
            if (-1 != (i= file.getName().indexOf(".")))
            {
                String ext = file.getName().substring(i).toLowerCase();
                return ext.equals(".fcs") || ext.equals(".facs");
            }
            else
                return isFCSFile(file);
        }

        public boolean accept(File dir, String name)
        {
            int i;
            if (-1 != (i= name.indexOf(".")))
            {
                String ext = name.substring(i).toLowerCase();
                return ext.equals(".fcs") || ext.equals(".facs");
            }
            else
                return isFCSFile(new File(dir,name));
        }
    }

    
    static public boolean isFCSFile(File file)
    {
        InputStream stream = null;
        try
        {
            stream = new FileInputStream(file);
            byte[] buffer = new byte[6];
            byte[] compare2 = new byte[]{'F', 'C', 'S', '2', '.', '0'};
            byte[] compare3 = new byte[]{'F', 'C', 'S', '3', '.', '0'};
            if (stream.read(buffer) != 6)
                return false;
            if (!Arrays.equals(buffer, compare2) && !Arrays.equals(buffer, compare3))
                return false;
            return true;
        }
        catch (IOException e)
        {
            return false;
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

    public byte[] getFCSBytes(File file, int maxEventCount) throws Exception
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
        byte[] rgbDataLast = strDataLast.getBytes("UTF-8");
        // fill the dataLast with spaces
        Arrays.fill(bytes, 34, 42, (byte) 32);
        System.arraycopy(rgbDataLast, 0, bytes, 34 + 8 - rgbDataLast.length, rgbDataLast.length);

        // now, look for $TOT in the file
        byte[] rgbTotKey = new byte[]{(byte) chDelimiter, '$', 'T', 'O', 'T', (byte) chDelimiter};
        int ibTot = indexOf(bytes, rgbTotKey, textOffset, textLast);
        if (ibTot >= 0)
        {
            int ibNumberStart = ibTot + 6;
            int ibNumberEnd = indexOf(bytes, new byte[]{(byte) chDelimiter}, ibNumberStart, textLast + 1);
            if (ibNumberEnd > 0)
            {
                Arrays.fill(bytes, ibNumberStart, ibNumberEnd, (byte) 32);
                String strNewTot = Integer.toString(newEventCount);
                byte[] rgbNewTot = strNewTot.getBytes("UTF-8");
                System.arraycopy(rgbNewTot, 0, bytes, ibNumberStart, rgbNewTot.length);
            }
        }
        return bytes;
    }
}
