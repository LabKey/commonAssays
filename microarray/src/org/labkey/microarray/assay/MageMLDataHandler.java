package org.labkey.microarray.assay;

import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.study.assay.ParticipantVisitResolver;
import org.labkey.microarray.pipeline.ArrayPipelineManager;
import org.labkey.common.tools.SimpleXMLStreamReader;
import org.labkey.common.tools.TabLoader;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Jan 3, 2008
 */
public class MageMLDataHandler extends AbstractAssayTsvDataHandler
{
    public Priority getPriority(ExpData data)
    {
        File f = data.getFile();
        if (f != null && ArrayPipelineManager.isMageFile(f))
        {
            return Priority.HIGH;
        }
        return null;
    }

    protected Map<String, Object>[] loadFileData(PropertyDescriptor[] columns, File dataFile, ParticipantVisitResolver resolver) throws IOException, ExperimentException
    {
        FileInputStream fIn = null;
        try
        {
            fIn = new FileInputStream(dataFile);
            SimpleXMLStreamReader reader = new SimpleXMLStreamReader(fIn);
            List<String> columnNames = new ArrayList<String>();
            if (reader.skipToStart("QuantitationTypes_assnreflist"))
            {
                boolean endOfTypes = false;
                while (!endOfTypes)
                {
                    if (reader.isStartElement())
                    {
                        String identifier = reader.getAttributeValue(null, "identifier");
                        if (identifier != null)
                        {
                            if (identifier.startsWith("Local:QT:"))
                            {
                                identifier = identifier.substring("Local:QT:".length());
                            }
                            columnNames.add(identifier);
                        }
                    }
                    else if (reader.isEndElement() && reader.getLocalName().equals("QuantitationTypes_assnreflist"))
                    {
                        endOfTypes = true;
                    }
                    reader.next();
                }
            }

            if (reader.skipToStart("DataInternal"))
            {
                reader.next();
                int startingOffset = reader.getLocation().getCharacterOffset();
                if (reader.skipToEnd("DataInternal"))
                {
                    int endingOffset = reader.getLocation().getCharacterOffset();
                    reader.close();
                    InputStream tsvIn = new TrimmedFileInputStream(dataFile, startingOffset, endingOffset);
                    try
                    {
                        Map<String, Class> expectedColumns = new HashMap<String, Class>(columns.length);
                        for (PropertyDescriptor col : columns)
                            expectedColumns.put(col.getName().toLowerCase(), col.getPropertyType().getJavaType());
                        Reader fileReader = new InputStreamReader(tsvIn);

                        TabLoader loader = new TabLoader(fileReader, false);
                        TabLoader.ColumnDescriptor[] tabColumns = new TabLoader.ColumnDescriptor[columnNames.size()];
                        for (int i = 0; i < columnNames.size(); i++)
                        {
                            String name = columnNames.get(i);
                            Class colClass = expectedColumns.get(name.toLowerCase());
                            tabColumns[i] = new TabLoader.ColumnDescriptor(name, colClass);
                            if (colClass == null)
                            {
                                tabColumns[i].load = false;
                            }
                            tabColumns[i].errorValues = ERROR_VALUE;
                        }
                        loader.setColumns(tabColumns);
                        return (Map<String, Object>[]) loader.load();
                    }
                    finally
                    {
                        try { tsvIn.close(); } catch (IOException e) {}
                    }
                }
            }
        }
        catch (XMLStreamException e)
        {
            throw new ExperimentException(e);
        }
        finally
        {
            if (fIn != null) { try { fIn.close(); } catch (IOException e) {} }
        }
        throw new UnsupportedOperationException();
    }
}
