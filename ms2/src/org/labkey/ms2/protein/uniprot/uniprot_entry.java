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

import org.apache.log4j.Logger;
import org.labkey.ms2.protein.ParseActions;
import org.xml.sax.Attributes;

import java.sql.Connection;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

/**
 * User: tholzman
 * Date: Apr 4, 2005
 * Time: 11:58:19 AM
 */
public class uniprot_entry extends ParseActions
{

    private static Logger _log = Logger.getLogger(uniprot_entry.class);

    public static final int REPORT_MOD = 50;

    public int MOUTHFUL = 5000;

    public int getMOUTHFUL()
    {
        return MOUTHFUL;
    }

    public void setMOUTHFUL(int MOUTHFUL)
    {
        this.MOUTHFUL = MOUTHFUL;
    }

    public boolean Verbose = false;

    public boolean isVerbose()
    {
        return Verbose;
    }

    public void setVerbose(boolean verbose)
    {
        Verbose = verbose;
    }

    private int skipTracer = 0;

    public boolean beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs)
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (root.getSkipEntries() > 0) return true;

        try
        {
            this.clearCurItems();
            tables.put("ProtSequences", this);

        }
        catch (Exception e)
        {
            return false;
        }
        if (Verbose)
        {
            if (this.getItemCount() % REPORT_MOD == 0 && this.getItemCount() > 0)
            {
                System.out.println(this.getItemCount());
            }
            else
            {
                System.out.print(".");
            }
        }
        if (attrs.getValue("dataset") != null)
            this.getCurItem().put("source", attrs.getValue("dataset"));
        if (attrs.getValue("created") != null)
            this.getCurItem().put("source_insert_date", attrs.getValue("created"));
        if (attrs.getValue("modified") != null)
            this.getCurItem().put("source_change_date", attrs.getValue("modified"));
        return true;
    }

    public boolean endElement(Connection c, Map<String,ParseActions> tables)
    {
        uniprot root = (uniprot) tables.get("UniprotRoot");
        if (!root.unBumpSkip())
        {
            skipTracer++;
            if (Verbose)
            {
                if ((skipTracer % 1000) == 0)
                    _log.info((new Date()) + " 1000 <entry> elements skipped, " + root.getSkipEntries() + " more to go");
            }
            return true;
        }

        String uniqKey =
                ((String) this.getCurItem().get("genus")).toUpperCase() +
                        " " +
                        ((String) this.getCurItem().get("species")).toUpperCase() +
                        " " +
                        ((String) this.getCurItem().get("hash"));
        this.getAllItems().put(uniqKey, this.getCurItem());
        this.setItemCount(this.getItemCount() + 1);
        if (this.getAllItems().size() >= MOUTHFUL)
        {
            root.insertTables(tables, c);
            this.getAllItems().clear();
            tables.get("Organism").getAllItems().clear();
            ((Vector)tables.get("ProtIdentifiers").getCurItem().get("Identifiers")).clear();
            ((Vector)tables.get("ProtAnnotations").getCurItem().get("Annotations")).clear();
        }
        return true;
    }

    public boolean characters(Connection c, Map<String,ParseActions> tables, char ch[], int start, int len)
    {
        return true;
    }
}

