package org.labkey.flow.persist;

import org.labkey.flow.data.InputRole;

public enum ObjectType
{
    fcsKeywords(1, InputRole.FCSFile),
    compensationControl(2, null),
    fcsAnalysis(3, null),
    compensationMatrix(4, InputRole.CompensationMatrix),
    script(5, InputRole.AnalysisScript),

    workspace_fcsAnalysis(6, null),
    workspace_script(7, InputRole.AnalysisScript)
    ;

    final int _typeId;
    final InputRole _inputRole;
    ObjectType(int typeId, InputRole role)
    {
        _typeId = typeId;
        _inputRole = role;
    }
    public int getTypeId()
    {
        return _typeId;
    }

    public InputRole getInputRole()
    {
        return _inputRole;
    }

    static public ObjectType fromTypeId(int value)
    {
        for (ObjectType type : values())
        {
            if (type.getTypeId() == value)
                return type;
        }
        return null;
    }
}
