package org.labkey.microarray.assay;

import java.io.*;

/**
 * User: jeckels
 * Date: Jan 3, 2008
 */
public class TrimmedFileInputStream extends InputStream
{
    private final InputStream _in;
    private int _currentOffset;
    private final int _endingOffset;

    public TrimmedFileInputStream(File file, int startingOffset, int endingOffset) throws IOException
    {
        _endingOffset = endingOffset;
        _in = new BufferedInputStream(new FileInputStream(file));
        _in.skip(startingOffset);
        _currentOffset = startingOffset;
    }

    public int read() throws IOException
    {
        if (_currentOffset < _endingOffset)
        {
            _currentOffset++;
            return _in.read();
        }
        return -1;
    }

    public void close() throws IOException
    {
        _in.close();
    }
}
