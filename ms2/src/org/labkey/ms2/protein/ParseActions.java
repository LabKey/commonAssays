/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.protein;

import java.util.*;

import org.xml.sax.*;

import java.sql.*;

public abstract class ParseActions
{

    protected String _accumulated;

    public String getAccumulated()
    {
        return _accumulated;
    }

    public void setAccumulated(String a)
    {
        this._accumulated = a;
    }

    protected String comment = null;

    public String getComment()
    {
        return this.comment;
    }

    public void setComment(String c)
    {
        this.comment = c;
    }

    protected String whatImParsing;

    public String getWhatImParsing()
    {
        return whatImParsing;
    }

    public void setWhatImParsing(String whatImParsing)
    {
        this.whatImParsing = whatImParsing;
    }

    protected int currentInsertId = 0;

    public void setCurrentInsertId(int id)
    {
        this.currentInsertId = id;
    }

    public int getCurrentInsertId()
    {
        return this.currentInsertId;
    }

    private int itemCount;

    public int getItemCount()
    {
        return itemCount;
    }

    public void setItemCount(int itemCount)
    {
        this.itemCount = itemCount;
    }

    protected Map<String, Map<String, Object>> allItems = new HashMap<String, Map<String, Object>>();

    public Map<String, Map<String, Object>> getAllItems()
    {
        return allItems;
    }

    public void setAllItems(Map<String, Map<String, Object>> allItems)
    {
        this.allItems = allItems;
    }

    protected Map<String,Object> curItem;

    public Map<String, Object> getCurItem()
    {
        return curItem;
    }

    public void clearCurItems()
    {
        curItem = new HashMap<String, Object>();
    }

    public ParseActions()
    {
    }

    /*
    protected void copyAttribsToColumns(Map m, Attributes attrs, Map target){
       if(m == null || attrs == null || target == null) return;
       for(Iterator it=m.keySet().iterator(); it.hasNext();) {
          String attribName = (String)it.next();
          String attribVal = null;
          if((attribVal=attrs.getValue(attribName)) != null) {
             target.put((String)m.get(attribName),attribVal);
          }
       }
    }
    */
    public void beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs) throws SAXException
    {
        _accumulated = "";
    }

    public void endElement(Connection c, Map<String,ParseActions> tables) throws SAXException
    {
    }

    public void characters(Connection c, Map<String,ParseActions> tables, char ch[], int start, int len) throws SAXException
    {
    }
}
