package org.labkey.ms2;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.XarContext;
import org.labkey.api.security.User;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * User: adam
 * Date: Jul 23, 2007
 * Time: 11:06:32 AM
 */
public class MascotDatImporter extends MS2Importer
{
    public MascotDatImporter(User user, Container c, String description, String fullFileName, Logger log, XarContext context)
    {
        super(context, user, c, description, fullFileName, log);
    }

    protected String getType()
    {
        return "Mascot";
    }

    public void importRun() throws IOException, SQLException, XMLStreamException
    {
        throw new UnsupportedOperationException();
    }
}
