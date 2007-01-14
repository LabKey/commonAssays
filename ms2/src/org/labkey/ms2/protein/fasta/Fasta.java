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
package org.labkey.ms2.protein.fasta;

import java.util.*;
import java.io.*;

import org.labkey.api.util.HashHelpers;

/**
 * Created by IntelliJ IDEA.
 * User: tholzman
 * Date: Apr 18, 2005
 * Time: 10:08:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class Fasta
{
    protected String sequence;
    protected String sha1;
    protected String genus;
    protected String species;
    protected String fullOrg;
    protected String title;
    protected String comment;
    protected String seq_id;
    protected HashMap idents;

    public Fasta()
    {
        this.idents = new HashMap();
    }

    public String getSequence()
    {
        return sequence;
    }

    public void setSequence(String sequence)
    {
        this.sequence = sequence;
    }

    public String getSha1()
    {
        return sha1;
    }

    public void setSha1(String sha1)
    {
        this.sha1 = sha1;
    }

    public String getGenus()
    {
        return genus;
    }

    public void setGenus(String genus)
    {
        this.genus = genus;
    }

    public String getSpecies()
    {
        return species;
    }

    public void setSpecies(String species)
    {
        this.species = species;
    }

    public String getFullOrg()
    {
        return fullOrg;
    }

    public void setFullOrg(String fullName)
    {
        this.fullOrg = fullName;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getSeq_id()
    {
        return seq_id;
    }

    public void setSeq_id(String seq_id)
    {
        this.seq_id = seq_id;
    }

    public HashMap getIdents()
    {
        return idents;
    }

    public void setIdents(HashMap idents)
    {
        this.idents = idents;
    }

    protected static String readSequence(BufferedReader b)
    {
        String seq = "";
        String line;
        for (; ;)
        {
            try
            {
                b.mark(100000);
                line = b.readLine();
                if (line == null) break;
                if (line.charAt(0) == '>')
                {
                    b.reset();
                    break;
                }
                else
                {
                    seq += line;
                }
            }
            catch (Exception e)
            {
                System.err.println("Can't process sequence in readSequence: " + e);
                return null;
            }
        }
        return seq;
    }


    public static Fasta readFasta(BufferedReader b)
    {
        String titleLine = null;
        Fasta tmp = new Fasta();

        try
        {
            do
            {
                titleLine = b.readLine();
            }
            while (titleLine != null && titleLine.charAt(0) != '>');
        }
        catch (Exception e)
        {
            System.err.println("Problem in readFasta: " + e);
            return null;
        }
        if (titleLine == null) return null;
        titleLine = titleLine.substring(1);
        String seq = readSequence(b);
        if (seq == null) return null;
        tmp.setSequence(seq);
        tmp.setSha1(HashHelpers.hash(seq));
        String title = (new StringTokenizer(titleLine)).nextToken();
        tmp.setTitle(title);
        if (title.indexOf('|') != -1)
        {
            String ids[] = title.split("\\|");
            for (int i = 0; (i + 1) < (ids.length); i += 2)
            {
                tmp.getIdents().put(ids[i], ids[i + 1]);
            }
        }
        String remaining = titleLine.substring(title.length()).trim();
        tmp.setComment(remaining);
        int orgIndex = remaining.lastIndexOf('[');
        if (orgIndex == -1) orgIndex = remaining.lastIndexOf('{');
        if (orgIndex != -1)
        {
            char kindOfBrace = remaining.charAt(orgIndex);
            char otherBrace = 0;
            if (kindOfBrace == '[') otherBrace = ']';
            if (kindOfBrace == '{') otherBrace = '}';
            if (otherBrace == 0)
            {
                System.err.println("Ooops.  We're not parsing braces correctly");
                return null;
            }
            int closeIndex = remaining.indexOf(otherBrace, orgIndex);
            if (closeIndex == -1) closeIndex = remaining.length() - 1;
            String fullOrg = remaining.substring(orgIndex + 1, closeIndex);
            String terms[] = fullOrg.split("\\s");
            if (terms.length >= 1) tmp.setGenus(terms[0].replaceAll("\\p{Punct}", ""));
            if (terms.length >= 2) tmp.setSpecies(terms[1].replaceAll("\\p{Punct}", ""));
            if (terms.length >= 5 || terms.length < 2)
            {
/*            System.out.println("Warning - check out this organism: "+titleLine);
*/         }
            tmp.setFullOrg(fullOrg);
        }
        return tmp;
    }
}
