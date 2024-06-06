package org.labkey.api.protein.fasta;

public class FastaParsingForm
{
    private String _header;

    public String getHeader()
    {
        if (_header != null && !_header.isEmpty() && _header.startsWith(">"))
        {
            return _header.substring(1);
        }
        return _header;
    }

    public void setHeader(String headers)
    {
        _header = headers;
    }
}
