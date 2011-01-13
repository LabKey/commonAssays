/*
 * Copyright (c) 2008-2011 LabKey Corporation
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

/**
 * User: billnelson@uky.edu
 * Date: Apr 24, 2008
 */

/**
 * <code>CutSite</code>
 */
public class CutSite
{
    private char[] cuts;
    private char[] noCuts;
    private String signature;
    private boolean nTerm;

    public CutSite(char[] cuts, char[] noCuts, String signature, boolean nTermCut)
    {
        this.cuts = cuts;
        this.noCuts = noCuts;
        this.signature = signature;
        this.nTerm = nTermCut;
    }

    public CutSite(String cutSite)
    {
        if(cutSite == null) throw new EnzymeParseException("null");

        cutSite = cutSite.trim();
        String[] split = cutSite.split("\\|");
        if(split.length != 2) throw new EnzymeParseException("Invalid cut site definition: '" + cutSite + "'.");
        int length1 = split[0].length();
        int length2 = split[1].length();
        char c1 = split[0].charAt(0);
        char c2 = split[0].charAt(length1 - 1);
        char c3 = split[1].charAt(0);
        char c4 = split[1].charAt(length2 - 1);

        if(c1 == '{' && c2 == '}')
        {
                throw new EnzymeParseException("Invalid cut site definition: '" + cutSite + "'.");
        }
        else if(c1 == '[' && c2 == ']')
        {
            loadCuts(split[0]);
            if(c3 == '{' && c4 == '}')
            {
                loadNoCuts(split[1]);
            }
            else if(c3 == '[' && c4 == ']')
            {
                if(cuts[0] == 'X' && cuts.length == 1)
                {
                    nTerm = true;
                    loadCuts(split[1]);
                    noCuts = new char[]{};
                }
                else if(split[1].charAt(1) == 'X')
                {
                    noCuts = new char[]{};
                }
                else
                {
                    throw new EnzymeParseException("Invalid cut site definition: '" + cutSite + "'.");
                }
            }
            else
            {
                throw new EnzymeParseException("Invalid cut site definition: '" + cutSite + "'.");
            }
        }
        else throw new EnzymeParseException("Invalid cut site definition: '" + cutSite + "'.");
        signature = cutSite;
    }

    private void loadCuts(String chars)
    {
        if(chars == null)return;
        chars = chars.substring(1,chars.length() -1);
        int cutCount = chars.length();
        cuts = new char[cutCount];
        for(int i = 0; i < cutCount; i++)
        {
            cuts[i] = chars.charAt(i);
        }
    }

    private void loadNoCuts(String chars)
    {
        if(chars == null)return;
        chars = chars.substring(1,chars.length() -1);
        int cutCount = chars.length();
        noCuts = new char[cutCount];
        for(int i = 0; i < cutCount; i++)
        {
            noCuts[i] = chars.charAt(i);
        }
    }



    public char[] getCuts()
    {
        return cuts;
    }

    public void setCuts(char[] cuts)
    {
        this.cuts = cuts;
    }

    public boolean isNTerm()
    {
        return nTerm;
    }

    public void setNTerm(boolean nTerm)
    {
        this.nTerm = nTerm;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public char[] getNoCuts()
    {
        return noCuts;
    }

    public void setNoCuts(char[] noCuts)
    {
        this.noCuts = noCuts;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
//        String classNameThis = GWT.getTypeName(this);
//        String classNameO = GWT.getTypeName(o);
//        if (o == null || !classNameThis.equals(classNameO)) return false;
        CutSite cutSite = (CutSite) o;

        if(!arrayEquals(cuts, cutSite.cuts)) return false;
        if(!arrayEquals(noCuts, cutSite.noCuts)) return false;
        if(cuts.length == 1 && noCuts.length == 1)
            if(cuts[0] == 'X' && noCuts[0] == 'X') return true;
        return nTerm == cutSite.nTerm;
    }

    private boolean arrayEquals(char[] c1, char[] c2)
    {
        if(c1 == c2 && c1 == null) return true;
        if(c1 == null || c2 == null) return false;
        if(c1.length != c2.length) return false;
        sort(c1);
        sort(c2);
        for(int i = 0; i < c1.length; i++)
        {
          if(c1[i] != c2[i]) return false;
        }
        return true;
    }

    private void sort(char[] c) {
        int n = c.length;
        for (int pass=1; pass < n; pass++)
        {
            for (int i=0; i < n-pass; i++)
            {
                if (c[i] > c[i+1])
                {
                    char temp = c[i];
                    c[i] = c[i+1];
                    c[i+1] = temp;
                }
            }
        }
    }
}
