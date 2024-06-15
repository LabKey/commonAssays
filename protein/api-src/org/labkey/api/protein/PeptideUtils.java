package org.labkey.api.protein;

public class PeptideUtils
{
    // Get rid of previous and next amino acid
    public static String trimPeptide(String peptide)
    {
        String[] p = peptide.split("\\.");

        if (2 < p.length)
            return p[1];
        else
            return peptide;
    }

    // Remove variable modification characters, leaving only A-Z
    public static String stripPeptide(String peptide)
    {
        return stripPeptideAZ(peptide);
    }

    // Remove variable modifications and '.', leaving only A-Z
    public static String stripPeptideAZ(String peptide)
    {
        StringBuilder stripped = new StringBuilder();

        for (int i = 0; i < peptide.length(); i++)
        {
            char c = peptide.charAt(i);
            if (c >= 'A' && c <= 'Z')
                stripped.append(c);
        }

        return stripped.toString();
    }

    // String variable modifications and '.', leaving '-' and A-Z
    public static String stripPeptideAZDash(String peptide)
    {
        StringBuilder stripped = new StringBuilder();

        for (int i = 0; i < peptide.length(); i++)
        {
            char c = peptide.charAt(i);
            if (c >= 'A' && c <= 'Z' || c == '-')
                stripped.append(c);
        }

        return stripped.toString();
    }
}
