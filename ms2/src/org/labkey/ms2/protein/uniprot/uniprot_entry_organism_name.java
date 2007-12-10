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
package org.labkey.ms2.protein.uniprot;

import org.xml.sax.*;
import org.labkey.ms2.protein.*;

public class uniprot_entry_organism_name extends CharactersParseActions
{

    private String _curType = null;

    public void beginElement(ParseContext context, Attributes attrs) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }

        _curType = attrs.getValue("type");

        if (_curType == null)
        {
            throw new SAXException("type is not set");
        }
        _accumulated = "";
    }

    public void endElement(ParseContext context) throws SAXException
    {
        if (context.isIgnorable())
        {
            return;
        }

        UniprotOrganism organism = context.getCurrentOrganism();
        if (organism == null)
        {
            throw new SAXException("No current organism");
        }
        if (_curType.equalsIgnoreCase("common"))
        {
            organism.setCommonName(_accumulated.trim());
            return;
        }
        if (_curType.equalsIgnoreCase("scientific") || _curType.equalsIgnoreCase("full"))
        {
            String together = _accumulated.trim();
            String separate[] = together.split(" ");
            if (separate == null || separate.length < 2)
            {
                XMLProteinHandler.parseWarning("Found organism with this name: '" + together + "'");
            }
            if (separate != null && separate.length >= 1)
            {
                organism.setGenus(separate[0].replaceAll("'", ""));
            }
            if (separate != null && separate.length >= 2)
            {
                organism.setSpecies(separate[1].replaceAll("'", ""));
            }
            if (separate != null && (_curType.equalsIgnoreCase("full") || separate.length > 2))
            {
                organism.setComments(together);
            }
            String annotType;
            if (_curType.equalsIgnoreCase("full"))
            {
                annotType = "FullOrganismName";
            }
            else
            {
                annotType = "ScientificOrganismName";
            }

            UniprotAnnotation annot = new UniprotAnnotation(together, annotType, context.getCurrentSequence());
            context.getAnnotations().add(annot);
        }
    }
}
