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

/**
 * User: tholzman
 * Date: Feb 28, 2005
 */
import org.xml.sax.Attributes;
import org.labkey.ms2.protein.*;

import java.sql.Connection;
import java.util.*;

public class uniprot_entry_dbReference_property extends ParseActions
{

    public void beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs)
    {
        _accumulated = null;
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0)
        {
            return;
        }
        String propType = attrs.getValue("type");
        String propVal = attrs.getValue("value");
        clearCurItems();
        if (propType.equalsIgnoreCase("term"))
        {
            Vector surroundingRef = (Vector)tables.get("ProtIdentifiers").getCurItem().get("Identifiers");
            if (surroundingRef == null)
            {
                return;
            }
            Map sRefContents = (Map) surroundingRef.lastElement();
            String refType = (String) sRefContents.get("identifier");
            if (refType == null || !refType.startsWith("GO:"))
            {
                return;
            }
            String annotType = "GO_" + propVal.substring(0, 1);
            Map curSeq = tables.get("ProtSequences").getCurItem();
            if (curSeq == null)
            {
                return;
            }
            Vector annots = (Vector)tables.get("ProtAnnotations").getCurItem().get("Annotations");
            annots.add(getCurItem());
            getCurItem().put("annot_val", refType + " " + propVal);
            getCurItem().put("annotType", annotType);
            getCurItem().put("sequence", curSeq);
            getCurItem().put("identType", sRefContents.get("identType"));
            getCurItem().put("identifier", refType);
        }
    }
}
