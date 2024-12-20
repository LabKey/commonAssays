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
package org.labkey.api.protein.uniprot;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class uniprot_entry_keyword extends CharactersParseActions
{
    private UniprotIdentifier _currentIdentifier;

    @Override
    public void beginElement(ParseContext context, Attributes attrs) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }

        String kwid = attrs.getValue("id");
        if (kwid == null)
        {
            throw new SAXException("id is not set");
        }
        _accumulated = "";
        _currentIdentifier = new UniprotIdentifier("Uniprot_keyword", kwid, context.getCurrentSequence());
        context.getIdentifiers().add(_currentIdentifier);
    }

    @Override
    public void endElement(ParseContext context)
    {
        if (context.isIgnorable())
        {
            return;
        }

        UniprotAnnotation annot = new UniprotAnnotation(_accumulated, "keyword", _currentIdentifier);
        context.getAnnotations().add(annot);
    }
}
