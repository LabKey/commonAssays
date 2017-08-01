package org.labkey.flow.controllers;

import org.labkey.flow.data.FlowDataObject;

/**
 * User: kevink
 * Date: 7/31/17
 */
public class FlowDataObjectForm extends FlowObjectForm<FlowDataObject>
{
    private int _dataId;

    public int getDataId()
    {
        return _dataId;
    }

    public void setDataId(int dataId)
    {
        _dataId = dataId;
    }

    @Override
    public FlowDataObject getFlowObject()
    {
        if (this.flowObject == null)
        {
            FlowDataObject fdo = FlowDataObject.fromRowId(_dataId);
            this.flowObject = fdo;
        }
        return this.flowObject;
    }
}
