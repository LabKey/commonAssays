package org.labkey.luminex;

/**
 * User: jeckels
 * Date: Jun 6, 2011
 */
public class Titration
{
    private int _rowId;
    private int _runId;
    private String _name = "Standard";
    private boolean _standard;
    private boolean _qcControl;
    private boolean _unknown;

    public int getRowId()
    {
        return _rowId;
    }

    public void setRowId(int rowId)
    {
        _rowId = rowId;
    }

    public int getRunId()
    {
        return _runId;
    }

    public void setRunId(int runId)
    {
        _runId = runId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        name = name == null || name.trim().isEmpty() ? "Standard" : name;
        
        _name = name;
    }

    public boolean isStandard()
    {
        return _standard;
    }

    public void setStandard(boolean standard)
    {
        _standard = standard;
    }

    public boolean isQcControl()
    {
        return _qcControl;
    }

    public void setQcControl(boolean qcControl)
    {
        _qcControl = qcControl;
    }

    public void setUnknown(boolean unknown)
    {
        _unknown = unknown;
    }

    public boolean isUnknown()
    {
        return _unknown;
    }

    public enum Type
    {
        standard
        {
            @Override
            public boolean isEnabled(Titration titration)
            {
                return titration.isStandard();
            }
            @Override
            public void setEnabled(Titration titration, boolean enabled)
            {
                titration.setStandard(enabled);
            }
        },
        qccontrol
        {
            @Override
            public boolean isEnabled(Titration titration)
            {
                return titration.isQcControl();
            }
            @Override
            public void setEnabled(Titration titration, boolean enabled)
            {
                titration.setQcControl(enabled);
            }
        },
        unknown
        {
            @Override
            public boolean isEnabled(Titration titration)
            {
                return titration.isUnknown();
            }
            @Override
            public void setEnabled(Titration titration, boolean enabled)
            {
                titration.setUnknown(enabled);
            }
        };

        public abstract boolean isEnabled(Titration titration);
        public abstract void setEnabled(Titration titration, boolean enabled);
    }
}
