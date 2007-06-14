package org.labkey.ms2.client;

/**
 * User: jeckels
 * Date: Jun 12, 2007
 */
public class RunGroup
{
    private int[] _runIndices;
    private boolean _requireAll;

    public RunGroup()
    {
        this(new int[0], false);
    }

    public RunGroup(int[] runIndices, boolean requireAll)
    {
        _requireAll = requireAll;
        _runIndices = runIndices;
    }

    public boolean isRequireAll()
    {
        return _requireAll;
    }

    public int[] getRunIndices()
    {
        return _runIndices;
    }

    public boolean containsRunIndex(int index)
    {
        for (int i = 0; i < _runIndices.length; i++)
        {
            if (_runIndices[i] == index)
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasProtein(int proteinIndex, boolean[][] hits)
    {
        for (int i = 0; i < _runIndices.length; i++)
        {
            if (!_requireAll)
            {
                if (hits[proteinIndex][_runIndices[i]])
                {
                    return true;
                }
            }
            else
            {
                if (!hits[proteinIndex][_runIndices[i]])
                {
                    return false;
                }
            }
        }
        return _requireAll;
    }

    public int getProteinCount(boolean[][] hits)
    {
        int result = 0;
        for (int p = 0; p < hits.length; p++)
        {
            if (hasProtein(p, hits))
            {
                result++;
            }
        }
        return result;
    }
}
