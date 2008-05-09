package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;

/**
 * User: billnelson@uky.edu
 * Date: Mar 31, 2008
 */
public class MzXmlComposite extends SearchFormComposite
{
    VerticalPanel instance = new VerticalPanel();
    public static final String RUNNING = "RUNNING";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String ANNOTATED = "ANNOTATED";
    public static final String COMPLETE = "COMPLETE";
    boolean hasWork;

    public MzXmlComposite()
    {
        super();
        init();
    }

     public MzXmlComposite(Map mzXmlmap)
     {
         super();
         update(mzXmlmap);
         init();

     }

    public void init()
    {
        initWidget(instance);
        labelWidget = new Label();
    }

    public void update(Map mzXmlMap)
    {
        Set keySet =  mzXmlMap.keySet();
        hasWork = false;
        instance.clear();
        int count = 1;
        int num = keySet.size();
        if(num > 1)
        {
            ((Label)labelWidget).setText("Analyze files:");
        }
        if(num > 0)
        {
            for(Iterator it = keySet.iterator(); it.hasNext();)
            {
                StringBuffer name = new StringBuffer((String)it.next());
                String status = (String)mzXmlMap.get(name.toString());
                if(status.equals(ANNOTATED) || status.equals(UNKNOWN))
                {
                    hasWork = true;
                }
                else
                {
                    name.append("(<b>");
                    name.append(status);
                    name.append("</b>)");
                    if(count < (num))
                    {
                        name.append("<br>");
                        count++;
                    }    
                }
                HTML html = new HTML(name.toString());
                html.setStylePrimaryName("ms-readonly");
                instance.add(html);
            }
        }
        else
        {
            instance.add(new Label("No mzXML files found"));
        }
    }

    public String getName()
    {
        return null;  //Not needed. Just Labels.
    }

    public void setName(String s)
    {
        //Not needed. Just Labels.
    }

    public void setWidth(String width)
    {
        //defaults are okay for now
    }

    public Widget getLabel(String style)
    {
        ((Label)labelWidget).setText("Analyze file:");
        labelWidget.setStylePrimaryName(style);
        return labelWidget;
    }

    public String validate()
    {
        return null;  //No need for now.
    }

    public boolean hasWork()
    {
        return hasWork;
    }

    public void setHasWork(boolean hasWork)
    {
        this.hasWork = hasWork;
    }

    public void clearStatus()
    {
        for(Iterator it = instance.iterator(); it.hasNext();)
        {
            Label mz = (Label)it.next();
            String name = mz.getText();
            if(name.indexOf("(") != -1)
                name = name.substring(0, name.indexOf("("));
            mz.setText(name);
        }
        setHasWork(true);
    }
}
