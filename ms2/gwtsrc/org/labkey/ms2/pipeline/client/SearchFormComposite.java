package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * User: billnelson@uky.edu
 * Date: Mar 25, 2008
 */
public abstract class SearchFormComposite extends Composite implements HasName
{
    protected boolean readOnly;
    protected Widget labelWidget;

    protected SearchFormComposite()
    {
        super();
    }

    public abstract void init();

    public abstract void setWidth(String width);

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public abstract Widget getLabel(String style);



    public abstract String validate();
}
