package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

abstract public class GWTGate implements IsSerializable
{
    String typeName = "";
    boolean open;
    boolean dirty;

    public GWTGate()
    {
    }
    
    protected GWTGate(String type)
    {
        typeName = type;
    }
    
    public boolean isOpen()
    {
        return open;
    }

    public void setOpen(boolean open)
    {
        this.open = open;
    }

    public boolean isDirty()
    {
        return dirty;
    }

    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;
    }

    public String getType()
    {
        return typeName;
    }

//    abstract public GWTGate close();
    abstract public boolean canSave();
}
