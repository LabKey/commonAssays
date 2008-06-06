/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.DOM;
import org.labkey.api.gwt.client.util.StringUtils;

import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Apr 8, 2008
 */
public abstract class InputXmlComposite extends SearchFormComposite  implements SourcesChangeEvents
{
    protected TextAreaWrapable inputXmlTextArea = new TextAreaWrapable();
    protected Hidden inputXmlHidden = new Hidden();
    protected HTML inputXmlHtml = new HTML();
    protected HorizontalPanel instance = new HorizontalPanel();
    protected ParamParser params;
    public static final String DEFAULT_XML = "<?xml version=\"1.0\"?>\n" +
                "<bioml>\n" +
                "<!-- Override default parameters here. -->\n" +
                "</bioml>";

    public InputXmlComposite()
    {
        super();
        init();
    }

    public void setDefault()
    {
        update(DEFAULT_XML);
    }

    public String update(String text)
    {
        if(text.equals(""))
            text = DEFAULT_XML;
        inputXmlTextArea.setText(text);
        return validate();
    }

    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        if(readOnly)
        {
            instance.remove(inputXmlTextArea);
            String text = inputXmlTextArea.getText();
            inputXmlHidden.setName(getName());
            inputXmlHidden.setValue(text);
            inputXmlHtml.setHTML(StringUtils.filter(text,true));
            instance.add(inputXmlHidden);
            instance.add(inputXmlHtml);
        }
        else
        {
            instance.remove(inputXmlHidden);
            instance.remove(inputXmlHtml);
            instance.add(inputXmlTextArea);
        }

    }

    public void init()
    {
        inputXmlTextArea.setVisibleLines(10);
        inputXmlTextArea.setWrap("OFF");
        inputXmlHtml.setStylePrimaryName("ms-readonly");
        instance.add(inputXmlTextArea);
        initWidget(instance);
    }

    public String getName()
    {
        return inputXmlTextArea.getName();
    }

    public void setName(String name)
    {
        inputXmlTextArea.setName(name);
    }

    public void setWidth(String width)
    {
        instance.setWidth(width);
        inputXmlTextArea.setWidth(width);
        inputXmlHtml.setWidth(width);
    }

    public String validate()
    {
        return params.validate();
    }

    public void addChangeListener(ChangeListener changeListener)
    {
        inputXmlTextArea.addChangeListener(changeListener);
    }

    public void removeChangeListener(ChangeListener changeListener)
    {
        inputXmlTextArea.removeChangeListener(changeListener);
    }

    public void setSequenceDb(String name) throws SearchFormException
    {
        params.setSequenceDb(name);
    }

    public String getSequenceDb()
    {
        return params.getSequenceDb();
    }

    public void setTaxonomy(String name) throws SearchFormException
    {/*only implemented for Mascot*/}

    public String getTaxonomy()
    {
        return params.getTaxonomy();
    }

    public void setEnzyme(String name) throws SearchFormException
    {
        params.setEnzyme(name);
    }

    public String getEnzyme()
    {
        return params.getEnzyme();
    }

    public Map getStaticMods(Map knownMods)
    {
        return mods2Map(params.getStaticMods(), knownMods);
    }

    public void setStaticMods(Map mods) throws SearchFormException
    {
        params.setStaticMods(mods);
    }

    public Map getDynamicMods(Map knownMods)
    {
        return mods2Map(params.getDynamicMods(), knownMods);
    }

    public void setDynamicMods(Map mods) throws SearchFormException
    {
        params.setDynamicMods(mods);
    }

    public void removeSequenceDb()
    {
        params.removeSequenceDb();
    }

    public void removeTaxonomy()
    {
        params.removeTaxonomy();
    }

    public void removeEnzyme()
    {
        params.removeEnzyme();
    }

    public void writeXml() throws SearchFormException
    {
        params.writeXml();
    }

    private Map mods2Map(String mods, Map knownMods)
    {
        if(knownMods == null || mods == null) return null;
        Map returnMap = new HashMap();
        if(mods.length() == 0) return returnMap;
        String[] modsArray = mods.split(",");
        List modsList = new ArrayList();
        for(int i = 0; i < modsArray.length; i++)
        {
            String checkMod = modsArray[i].trim();
            if(checkMod.length() > 0)
                modsList.add(checkMod);   
        }

        for(Iterator knownIt = knownMods.entrySet().iterator();knownIt.hasNext();)
        {
            Map.Entry  knownModEntry =  (Map.Entry)knownIt.next();
            String[] sites = ((String)knownModEntry.getValue()).split(",");
            boolean found;
            for(int i = 0; i < sites.length; i++)
            {
                found = false;
                for(Iterator modsIt = modsList.iterator(); modsIt.hasNext();)
                {
                    if(((String)modsIt.next()).equals(sites[i]))
                    {
                        found = true;
                        if(i == (sites.length -1))
                        {
                            returnMap.put(knownModEntry.getKey(),knownModEntry.getValue());
                            for(int y = 0; y < sites.length; y++)
                            {
                                modsList.remove(sites[y]);
                            }
                        }
                        break;
                    }
                }
                if(!found)break;
            }
        }
        for(Iterator modsIt = modsList.iterator(); modsIt.hasNext();)
        {
            String mod = (String)modsIt.next();
            returnMap.put(mod, mod);
        }
        return returnMap;
    }

    private class TextAreaWrapable extends TextArea
    {
        public void setWrap(String wrapOption)
        {
                Element textArea = getElement();
                DOM.setElementAttribute(textArea,"wrap",wrapOption);

        }
    }
}
