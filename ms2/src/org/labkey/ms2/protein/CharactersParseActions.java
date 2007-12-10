package org.labkey.ms2.protein;

/**
 * User: jeckels
 * Date: Oct 1, 2007
 */
public class CharactersParseActions extends ParseActions
{
    protected String _accumulated;

    public void characters(ParseContext context, char ch[], int start, int len)
    {
        if (context.isIgnorable())
        {
            return;
        }
        _accumulated += new String(ch, start, len);
    }
}
