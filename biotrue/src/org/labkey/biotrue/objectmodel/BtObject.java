package org.labkey.biotrue.objectmodel;

import org.labkey.api.data.Container;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.biotrue.controllers.BtController;

abstract public class BtObject
{
    abstract public Container getContainer();
    abstract public ViewURLHelper detailsURL();
    abstract public ViewURLHelper urlFor(BtController.Action action);
    abstract public String getLabel();
}
