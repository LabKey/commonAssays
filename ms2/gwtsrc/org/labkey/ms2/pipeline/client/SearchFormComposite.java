package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: Mar 25, 2008
 */
public abstract class SearchFormComposite extends Composite implements HasName
{
    protected boolean readOnly;
    protected Widget labelWidget;


    public boolean isReadOnly()
     {
         return readOnly;
     }

     public void setReadOnly(boolean readOnly)
     {
         this.readOnly = readOnly;
     }

    abstract public void init();

    abstract public void setWidth(String width);

    abstract public Widget getLabel(String style);

    abstract public String validate();
}
