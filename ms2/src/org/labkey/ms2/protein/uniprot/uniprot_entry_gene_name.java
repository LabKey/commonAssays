/*
 * Copyright (c) 2005-2008 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.protein.uniprot;

import org.xml.sax.*;
import org.labkey.ms2.protein.*;

public class uniprot_entry_gene_name extends CharactersParseActions
{

    private String curType = null;

    public void beginElement(ParseContext context, Attributes attrs) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }

        String nameType = attrs.getValue("type");
        if (nameType == null)
        {
            throw new SAXException("No type is currently set");
        }
        curType = nameType;
        _accumulated = "";
    }

    public void endElement(ParseContext context) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }

        UniprotSequence curSeq = context.getCurrentSequence();
        if (curSeq == null)
        {
            throw new SAXException("No current ProtSequences is available");
        }
        _accumulated = _accumulated.trim();
        if (curType.equalsIgnoreCase("primary") && _accumulated.length() > 0)
        {
            context.getIdentifiers().add(new UniprotIdentifier(IdentifierType.GeneName.toString(), _accumulated, curSeq));
            if (curSeq.getBestName() == null || curSeq.getBestName().trim().equals(""))
            {
                curSeq.setBestName(_accumulated);
            }
            curSeq.setBestGeneName(_accumulated);
        }
    }
}
