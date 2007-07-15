package org.labkey.luminex;

import org.labkey.api.study.DefaultAssayProvider;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.Protocol;
import org.labkey.api.exp.Lsid;

/**
 * User: jeckels
 * Date: Jul 13, 2007
 */
public class LuminexAssayProvider extends DefaultAssayProvider
{
    public LuminexAssayProvider()
    {
        super("LuminexAssayProtocol", "LuminexAssayRun", "LuminexDataFile");
    }

    public String getName()
    {
        return "Luminex";
    }
}
