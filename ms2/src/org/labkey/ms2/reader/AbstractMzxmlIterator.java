package org.labkey.ms2.reader;

/**
 * User: jeckels
 * Date: May 8, 2006
 */
public abstract class AbstractMzxmlIterator implements SimpleScanIterator
{
    final int _msLevel;

    AbstractMzxmlIterator(int msLevel)
    {
        _msLevel = msLevel;
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
