/*
 * Copyright (c) 2011 LabKey Corporation
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
