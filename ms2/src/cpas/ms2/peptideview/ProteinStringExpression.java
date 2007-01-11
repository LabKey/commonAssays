package cpas.ms2.peptideview;

import org.labkey.api.util.StringExpressionFactory;

import java.util.Map;
import java.io.Writer;
import java.io.IOException;

/**
 * User: arauch
 * Date: Apr 4, 2006
 * Time: 4:15:26 PM
 */
public class ProteinStringExpression implements StringExpressionFactory.StringExpression
{
    private String _localURI;

    public ProteinStringExpression(String localURI)
    {
        _localURI = localURI;
    }

    public String eval(Map ctx)
    {
        Integer seqId = (Integer)ctx.get("SeqId");

        // Always include protein (use as a title in the details page); include SeqId if it's not null
        return _localURI + (null != seqId ? "&seqId=" + seqId : "") + "&protein=" + ctx.get("Protein");
    }

    public String getSource()
    {
        return _localURI + "&seqId={$SeqId}";
    }

    public void render(Writer out, Map ctx) throws IOException
    {
        out.write(eval(ctx));
    }
}
