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

/**
 * User: tholzman
 * Date: Feb 28, 2005
 */
import org.xml.sax.Attributes;
import org.labkey.ms2.protein.*;

import java.util.*;

public class uniprot_entry_dbReference_property extends ParseActions
{

    public void beginElement(ParseContext context, Attributes attrs)
    {
        if (context.isIgnorable())
        {
            return;
        }
        String propType = attrs.getValue("type");
        String propVal = attrs.getValue("value");
        if (propType.equalsIgnoreCase("term"))
        {
            List<UniprotIdentifier> surroundingRef = context.getIdentifiers();
            if (surroundingRef == null)
            {
                return;
            }
            UniprotIdentifier sRefContents = surroundingRef.get(surroundingRef.size() - 1);
            String refType = sRefContents.getIdentifier();
            if (refType == null || !refType.startsWith("GO:"))
            {
                return;
            }
            String annotType = "GO_" + propVal.substring(0, 1);

            UniprotAnnotation annot = new UniprotAnnotation(refType + " " + propVal, annotType, sRefContents);
            context.getAnnotations().add(annot);
        }
    }
}
