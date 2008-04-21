package org.labkey.ms2.pipeline.client.tandem;

import com.google.gwt.user.client.ui.ClickListener;
import org.labkey.ms2.pipeline.client.SequenceDbComposite;

/**
 * User: billnelson@uky.edu
 * Date: Apr 17, 2008
 */

/**
 * <code>XtandemSequenceDbComposite</code>
 */

public class XtandemSequenceDbComposite extends SequenceDbComposite
{
    public XtandemSequenceDbComposite()
    {
        super();
    }

    public void init()
    {
        super.init();
        dirPanel.add(refreshPanel);
        refreshPanel.add(refreshButton);
        dirPanel.setSpacing(3);
    }

    public void setWidth(String width)
    {
        super.setWidth(width);
        int intWidth = 0;
        StringBuffer num = new StringBuffer();
        StringBuffer type = new StringBuffer();
        StringBuffer newWidth = new StringBuffer();
        for(int i = 0; i < width.length(); i++)
        {
            char widthChar = width.charAt(i);
            if(Character.isDigit(widthChar))
            {
                num.append(widthChar);
            }
            else
            {
                type.append(widthChar);
            }
        }
        try
        {
            intWidth = Integer.parseInt(num.toString());
            if(intWidth < 60) throw new NumberFormatException("The database path ListBox is too small");
            newWidth.append(Integer.toString(intWidth - (60 + 3)));
            newWidth.append(type);
            sequenceDbPathListBox.setWidth(newWidth.toString());
            refreshPanel.setWidth("60px");

        }
        catch(NumberFormatException e)
        {}
    }

    public void addClickListener(ClickListener listener)
    {
        refreshButton.addClickListener(listener);
    }
}
