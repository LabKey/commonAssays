package org.labkey.flow.persist;

public enum ObjectType
{
    fcsKeywords(1),
    compensationControl(2),
    fcsAnalysis(3),
    compensationMatrix(4),
    script(5),
    ;

    final int _typeId;
    ObjectType(int typeId)
    {
        _typeId = typeId;
    }
    public int getTypeId()
    {
        return _typeId;
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
