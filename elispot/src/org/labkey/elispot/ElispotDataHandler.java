package org.labkey.elispot;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.Table;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ViewBackgroundInfo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 8, 2008
 */
public class ElispotDataHandler extends AbstractExperimentDataHandler
{
    public static final DataType ELISPOT_DATA_TYPE = new DataType("ElispotAssayData");

    public static final String ELISPOT_DATA_LSID_PREFIX = "ElispotAssayData";
    public static final String ELISPOT_DATA_ROW_LSID_PREFIX = "ElispotAssayDataRow";
    public static final String ELISPOT_PROPERTY_LSID_PREFIX = "ElispotProperty";
    public static final String ELISPOT_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
        Container container = data.getContainer();
        AssayProvider provider = AssayService.get().getProvider(protocol);
        PlateTemplate template = provider.getPlateTemplate(container, protocol);

        double[][] cellValues = new double[0][0];
        String fileName = dataFile.getName().toLowerCase();
        try {
            if (fileName.endsWith(".xls"))
                cellValues = loadFromXls(dataFile, template);
            else if (fileName.endsWith(".txt"))
                cellValues = loadFromTxt(dataFile, template);

            insertPlateData(data, info, cellValues);
        }
        catch (IOException ioe)
        {
            throw new ExperimentException(ioe);
        }
    }

    public static Plate loadDataFile(File dataFile, PlateTemplate template) throws ExperimentException
    {
        double[][] cellValues = new double[0][0];
        String fileName = dataFile.getName().toLowerCase();
        try {
            if (fileName.endsWith(".xls"))
                cellValues = loadFromXls(dataFile, template);
            else if (fileName.endsWith(".txt"))
                cellValues = loadFromTxt(dataFile, template);

            return PlateService.get().createPlate(template, cellValues);
        }
        catch (IOException ioe)
        {
            throw new ExperimentException(ioe);
        }
    }

    private void insertPlateData(ExpData data, ViewBackgroundInfo info, double[][] cellValues) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(run.getProtocol().getLSID());
        Container container = data.getContainer();
        AssayProvider provider = AssayService.get().getProvider(protocol);
        PlateTemplate template = provider.getPlateTemplate(container, protocol);

        Plate plate = PlateService.get().createPlate(template, cellValues);
        boolean ownTransaction = !ExperimentService.get().isTransactionActive();
        try {
            if (ownTransaction)
            {
                ExperimentService.get().beginTransaction();
            }

            try
            {
                List<ObjectProperty> results = new ArrayList<ObjectProperty>();
                List<? extends WellGroup> antigens = plate.getWellGroups(WellGroup.Type.ANTIGEN);

                Map<String, ExpMaterial> materialMap = new HashMap<String, ExpMaterial>();
                for (ExpMaterial material : run.getMaterialInputs().keySet())
                    materialMap.put(material.getName(), material);

                for (WellGroup group : plate.getWellGroups(WellGroup.Type.SPECIMEN))
                {
                    for (Position pos : group.getPositions())
                    {
                        results.clear();
                        Well well = plate.getWell(pos.getRow(), pos.getColumn());

                        // find the antigen group associated with this well
                        WellGroup antigen = getAntigenGroup(antigens, pos);

                        ExpMaterial material = materialMap.get(group.getName());
                        if (material != null)
                        {
                            Lsid dataRowLsid = new Lsid(data.getLSID());
                            dataRowLsid.setNamespacePrefix(ELISPOT_DATA_ROW_LSID_PREFIX);
                            dataRowLsid.setObjectId(dataRowLsid.getObjectId() + "-" + pos.getRow() + ':' + pos.getColumn());

                            results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), ELISPOT_INPUT_MATERIAL_DATA_PROPERTY, material.getLSID(), PropertyType.STRING));
                            results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "SFU", well.getValue(), PropertyType.DOUBLE, "0.0"));
                            results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "WellgroupName", group.getName(), PropertyType.STRING));
                            results.add(getResultObjectProperty(container, protocol, dataRowLsid.toString(), "WellLocation", pos.toString(), PropertyType.STRING));

                            OntologyManager.ensureObject(container.getId(), dataRowLsid.toString(),  data.getLSID());
                            OntologyManager.insertProperties(container.getId(), results.toArray(new ObjectProperty[results.size()]), dataRowLsid.toString());
                        }
                    }
                }
            }
            finally
            {
                if (ownTransaction)
                {
                    ExperimentService.get().rollbackTransaction();
                }
            }
        }
        catch (SQLException se)
        {
            throw new ExperimentException(se);
        }
    }

    private WellGroup getAntigenGroup(List<? extends WellGroup> groups, Position pos)
    {
        for (WellGroup group : groups)
        {
            if (group.contains(pos))
                return group;
        }
        return null;
    }

    private ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type)
    {
        return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, null);
    }

    private ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type, String format)
    {
        Lsid propertyURI = new Lsid(ELISPOT_PROPERTY_LSID_PREFIX, protocol.getName(), propertyName);
        ObjectProperty prop = new ObjectProperty(objectURI, container.getId(), propertyURI.toString(), value, type, propertyName);
        prop.setFormat(format);
        return prop;
    }

    private static double[][] loadFromXls(File dataFile, PlateTemplate template) throws ExperimentException
    {
        WorkbookSettings settings = new WorkbookSettings();
        settings.setGCDisabled(true);
        Workbook workbook = null;
        try
        {
            workbook = Workbook.getWorkbook(dataFile, settings);
        }
        catch (IOException e)
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: " + e.getMessage(), e);
        }
        catch (BiffException e)
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: " + e.getMessage(), e);
        }
        double[][] cellValues = new double[template.getRows()][template.getColumns()];

        Sheet plateSheet = workbook.getSheet(0);

        int startRow = -1;
        int startCol = -1;

        for (int row = 0; row < plateSheet.getRows(); row++)
        {
            startCol = getStartColumn(plateSheet.getRow(row));
            if (startCol != -1)
            {
                startRow = getStartRow(plateSheet, row);
                break;
            }
        }

        if (startRow == -1 || startCol == -1)
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: unable to locate spot counts");
        }

        if (template.getRows() + startRow > plateSheet.getRows() || template.getColumns() + startCol > plateSheet.getColumns())
        {
            throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: expected " +
                    (template.getRows() + startRow) + " rows and " + (template.getColumns() + startCol) + " columns, but found "+
                    plateSheet.getRows() + " rows and " + plateSheet.getColumns() + " columns.");
        }

        for (int row = 0; row < template.getRows(); row++)
        {
            for (int col = 0; col < template.getColumns(); col++)
            {
                Cell cell = plateSheet.getCell(col + startCol, row + startRow);
                String cellContents = cell.getContents();
                try
                {
                    cellValues[row][col] = Double.parseDouble(cellContents);
                }
                catch (NumberFormatException e)
                {
                    throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: could not parse '" +
                            cellContents + "' as a number.", e);
                }
            }
        }
        return cellValues;
    }

    private static int getStartColumn(Cell[] row)
    {
        int col = 0;
        while (col < row.length)
        {
            if (StringUtils.equals(row[col].getContents(), "1"))
            {
                for (int i=1; i < 12; i++)
                {
                    if (!StringUtils.equals(row[col+i].getContents(), String.valueOf(1 + i)))
                        return -1;
                }
                return col;
            }
            col++;
        }
        return -1;
    }

    private static int getStartRow(Sheet sheet, int row)
    {
        while (row < sheet.getRows())
        {
            for (Cell cell : sheet.getRow(row))
            {
                if (StringUtils.equalsIgnoreCase(cell.getContents(), "A"))
                {
                    int col = cell.getColumn();
                    char start = 'B';
                    for (int i=1; i < 8; i++)
                    {
                        String val = String.valueOf(start++);
                        if (!StringUtils.equalsIgnoreCase(sheet.getRow(row+i)[col].getContents(), val))
                            return -1;
                    }
                    return row;
                }
            }
            row++;
        }
        return -1;
    }

    private static double[][] loadFromTxt(File dataFile, PlateTemplate template) throws IOException, ExperimentException
    {
        double[][] cellValues = new double[template.getRows()][template.getColumns()];
        LineNumberReader reader = new LineNumberReader(new FileReader(dataFile));
        try
        {
            List<String> data = new ArrayList<String>();
            String line;
            while((line = reader.readLine()) != null)
            {
                if (!StringUtils.isEmpty(line))
                    data.add(line);
            }

            int startRow = getStartRow(data);
            if (startRow == -1)
            {
                throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: unable to locate spot counts");
            }

            for (int i=0; i < template.getRows(); i++)
            {
                if (!getRowData(cellValues[i], data.get(startRow + i), i))
                    throw new ExperimentException(dataFile.getName() + " does not appear to be a valid data file: unable to locate spot counts");
            }
        }
        finally
        {
            try { reader.close(); } catch (IOException e) {}
        }
        return cellValues;
    }

    private static int getStartRow(List<String> data)
    {
        Pattern rowHeader = Pattern.compile("\\s+1\\s+2\\s+3\\s+4\\s+5\\s+6\\s+7\\s+8\\s+9\\s+10\\s+11\\s+12");
        for (int row = 0; row < data.size(); row++)
        {
            String line = data.get(row);
            if (rowHeader.matcher(line).find())
                return row + 1;
        }
        return -1;
    }

    private static boolean getRowData(double[] row, String line, int index)
    {
        StringTokenizer tokenizer = new StringTokenizer(line);
        char start = 'A';
        start += index;

        if (tokenizer.nextToken().equalsIgnoreCase(String.valueOf(start)))
        {
            int i=0;
            while (tokenizer.hasMoreTokens())
            {
                String token = tokenizer.nextToken();
                if (!NumberUtils.isNumber(token))
                    return false;
                row[i++] = NumberUtils.toDouble(token);
                if (i == row.length)
                    return true;
            }
        }
        return false;
    }

    public URLHelper getContentURL(Container container, ExpData data) throws ExperimentException
    {
        return null;
    }

    public void deleteData(ExpData data, Container container, User user) throws ExperimentException
    {
        try {
            OntologyManager.deleteOntologyObject(container.getId(), data.getLSID());
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (ELISPOT_DATA_LSID_PREFIX.equals(lsid.getNamespacePrefix()))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
