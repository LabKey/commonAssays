package org.labkey.ms1;

/**
 * Base class for a bean that can track its new and dirty state
 * Created by IntelliJ IDEA.
 * User: DaveS
 * Date: Sep 28, 2007
 * Time: 8:59:09 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DbBean
{
    public static final int ID_INVALID = -1;

    /**
     * Derived classes must override this to return their row id for an update.
     * @return the unique row id
     */
    public abstract Object getRowID();

    public boolean isNew()
    {
        return _new;
    }

    public void setNew(boolean aNew)
    {
        _new = aNew;
    }

    public boolean isDirty()
    {
        return _dirty;
    }

    public void setDirty(boolean dirty)
    {
        _dirty = dirty;
    }

    protected boolean _new = true;
    protected boolean _dirty = false;
}
