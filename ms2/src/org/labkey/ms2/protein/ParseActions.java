/*
 * Copyright (c) 2005-2012 Fred Hutchinson Cancer Research Center
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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.File;

public abstract class ParseActions
{
    protected File _file;
    protected String _comment = null;
    protected int _currentInsertId = 0;
    
    public String getComment()
    {
        return _comment;
    }

    public void setComment(String c)
    {
        _comment = c;
    }
    
    public File getFile()
    {
        return _file;
    }

    public void setFile(File file)
    {
        _file = file;
    }


    public void setCurrentInsertId(int id)
    {
        _currentInsertId = id;
    }

    public int getCurrentInsertId()
    {
        return _currentInsertId;
    }

    public void beginElement(ParseContext context, Attributes attrs) throws SAXException
    {
    }

    public void endElement(ParseContext context) throws SAXException
    {
    }

    public void characters(ParseContext context, char ch[], int start, int len) throws SAXException
    {
    }
}
