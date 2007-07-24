package org.labkey.luminex;

import org.labkey.api.exp.api.*;
import org.labkey.api.exp.*;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.PropertyService;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.study.SimpleAssayDataImportHelper;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.sql.SQLException;

import jxl.Workbook;
import jxl.Sheet;
import jxl.read.biff.BiffException;

/**
 * User: jeckels
 * Date: Jul 16, 2007
 */
public class LuminexExcelDataHandler extends AbstractExperimentDataHandler
{
    static final String LUMINEX_DATA_LSID_PREFIX = "LuminexDataFile";

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        if (!dataFile.exists())
        {
            log.warn("Could not find file " + dataFile.getAbsolutePath() + " on disk for data with LSID " + data.getLSID());
            return;
        }
        ExpRun expRun = data.getRun();
        if (expRun == null)
        {
            throw new ExperimentException("Could not load Luminex file " + dataFile.getAbsolutePath() + " because it is not owned by an experiment run");
        }
        try
        {
            ExpProtocol expProtocol = expRun.getProtocol();
            Protocol protocol = ExperimentService.get().getProtocol(expProtocol.getRowId());
            Domain dataDomain = null;
            Domain analyteDomain = null;
            for (String uri : protocol.retrieveObjectProperties().keySet())
            {
                Lsid lsid = new Lsid(uri);
                if (lsid.getNamespacePrefix() != null && lsid.getNamespacePrefix().startsWith(Protocol.ASSAY_DOMAIN_DATA))
                {
                    dataDomain = PropertyService.get().getDomain(info.getContainer(), uri);
                }
                else if (lsid.getNamespacePrefix() != null && lsid.getNamespacePrefix().startsWith(LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE))
                {
                    analyteDomain = PropertyService.get().getDomain(info.getContainer(), uri);
                }
            }
            if (dataDomain == null)
            {
                throw new ExperimentException("Could not find data domain for protocol with LSID " + protocol.getLSID());
            }
            if (analyteDomain == null)
            {
                throw new ExperimentException("Could not find analyte domain for protocol with LSID " + protocol.getLSID());
            }

            FileInputStream fIn = new FileInputStream(dataFile);
            Workbook workbook = Workbook.getWorkbook(fIn);
            Set<String> unknownColumns = new HashSet<String>();

            Integer id = OntologyManager.ensureObject(info.getContainer().getId(), data.getLSID());

            PropertyDescriptor[] dataColumns = OntologyManager.getPropertiesForType(dataDomain.getTypeURI(), info.getContainer());
            PropertyDescriptor[] analyteColumns = OntologyManager.getPropertiesForType(analyteDomain.getTypeURI(), info.getContainer());
            Map<String, Object>[] dataRows = parseFile(dataColumns, analyteColumns, workbook, unknownColumns, expRun, info.getContainer(), data, id);
            OntologyManager.insertTabDelimited(info.getContainer(), id,
                    new SimpleAssayDataImportHelper(data.getLSID()), dataColumns, dataRows, true);
        }
        catch (IOException e)
        {
            throw new ExperimentException("Failed to read from data file " + dataFile.getAbsolutePath(), e);
        }
        catch (SQLException e)
        {
            throw new ExperimentException("Failed to load from data file " + dataFile.getAbsolutePath(), e);
        }
        catch (BiffException e)
        {
            throw new XarFormatException("Failed to parse Excel file " + dataFile.getAbsolutePath(), e);
        }
    }

    public static class Analyte
    {
        private int _analyteId;
        private String _name;
        private final String _lsid;
        private Map<String, Object> _properties;

        public Analyte(int analyteId, String name, String lsid, Map<String, Object> properties)
        {
            _analyteId = analyteId;
            _name = name;
            _lsid = lsid;
            _properties = properties;
        }

        public String getLSID()
        {
            return _lsid;
        }

        public int getAnalyteId()
        {
            return _analyteId;
        }

        public String getName()
        {
            return _name;
        }

        public Map<String, Object> getProperties()
        {
            return _properties;
        }
    }

    private String getPropertyDescriptorURI(String name, PropertyDescriptor[] props)
    {
        for (PropertyDescriptor prop : props)
        {
            if (prop.getName().equals(name))
            {
                return prop.getPropertyURI();
            }
        }
        return null;
    }

    private Map<String, Object>[] parseFile(PropertyDescriptor[] columns, PropertyDescriptor[] analyteColumns, Workbook workbook, Set<String> unknownColumns, ExpRun expRun, Container container, ExpData data, Integer id) throws SQLException, ExperimentException
    {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        
        Map<String, Analyte> analytes = new HashMap<String, Analyte>();

        String analyteNamePropURI = getPropertyDescriptorURI("Name", analyteColumns);
        if (analyteNamePropURI == null)
        {
            throw new ExperimentException("Could not find Name property on Analyte domain");
        }

        for (int sheetIndex = 1; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
        {
            Sheet analyteSheet = workbook.getSheet(sheetIndex);

            String analyteName = analyteSheet.getName();
            String analyteLsid = new Lsid(data.getLSID() + ".Analyte" + analyteName).toString();
            Integer analyteId = OntologyManager.ensureObject(container.getId(), analyteLsid, data.getLSID());
            Map<String, Object> analyteProps = new HashMap<String, Object>();

            analyteProps.put(analyteNamePropURI, analyteName);

            int row = 0;
            do
            {
                String cellValue = analyteSheet.getCell(0, row).getContents();
                int index = cellValue.indexOf(":");
                if (index != -1)
                {
                    String propName = cellValue.substring(0, index);
                    String propURI = getPropertyDescriptorURI(propName, analyteColumns);
                    if (propURI != null)
                    {
                        analyteProps.put(propURI, cellValue.substring((propName + ":").length()).trim());
                    }
                }
            }
            while ((row + 1) <= analyteSheet.getRows() && !"".equals(analyteSheet.getCell(0, ++row).getContents()));

            // Skip over the blank line
            row++;
            
            Analyte analyte = new Analyte(analyteId, analyteName, analyteLsid.toString(), analyteProps);
            analytes.put(analyte._name, analyte);

            List<String> colNames = new ArrayList<String>();
            for (int col = 0; col < analyteSheet.getColumns(); col++)
            {
                colNames.add(analyteSheet.getCell(col, row).getContents());
            }
            row++;

            Map<String, String> namesToURIs = new HashMap<String, String>();
            for (PropertyDescriptor pd : columns)
            {
                namesToURIs.put(pd.getName(), pd.getPropertyURI());
            }

            do
            {
                Map<String, Object> rowValues = new LinkedHashMap<String, Object>();
                for (int col = 0; col < analyteSheet.getColumns(); col++)
                {
                    String columnName = colNames.get(col);
                    String propertyURI = namesToURIs.get(columnName);
                    if (propertyURI != null)
                    {
                        String value = analyteSheet.getCell(col, row).getContents();
                        if ("***".equals(value) || "---".equals(value))
                        {
                            value = "";
                        }
                        if (columnName.equals("Analyte"))
                        {
                            value = Integer.toString(analytes.get(value)._analyteId);
                        }
                        rowValues.put(propertyURI, value);
                    }
                    else
                    {
                        unknownColumns.add(columnName);
                    }
                }

                result.add(rowValues);
            }
            while (row++ < analyteSheet.getRows() && !"".equals(analyteSheet.getCell(0, row).getContents()));
        }

        Map<String, Object>[] analyteRows = new HashMap[analytes.size()];
        int index = 0;
        for (Analyte analyte : analytes.values())
        {
            analyteRows[index++] = analyte._properties;
        }
        OntologyManager.insertTabDelimited(container, id,
                new AnalyteImportHelper(analytes.values(), analyteNamePropURI), analyteColumns, analyteRows, true);

        return (Map<String, Object>[])result.toArray(new Map[0]);
    }

    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        ExpRun run = data.getRun();
        if (run != null)
        {
            ExpProtocol protocol = run.getProtocol();
            ViewURLHelper url = new ViewURLHelper("GenericAssay", "assaySummary.view", container);
            url.addParameter("activeView", "RUN_DATA");
            url.addParameter("runId", run.getRowId());
            url.addParameter("rowId", protocol.getRowId());
            return url;
        }
        return null;
    }

        public void deleteData(Data data, Container container) throws ExperimentException
    {
        try
        {
            OntologyManager.deleteOntologyObject(container.getId(), data.getLSID());
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    public void runMoved(Data newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    public Priority getPriority(Data data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (LUMINEX_DATA_LSID_PREFIX.equals(lsid.getNamespacePrefix()))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
