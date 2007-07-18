package org.labkey.luminex;

import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.*;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

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
        try
        {
            FileInputStream fIn = new FileInputStream(dataFile);
            Workbook workbook = Workbook.getWorkbook(fIn);
            LuminexRun run = new LuminexRun(dataFile.getName());
            run.setCreatedBy(info.getUser().getDisplayName());
            run.setCreatedOn(DateFormat.getInstance().format(new Date()));
            for (int sheetIndex = 1; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++)
            {
                Sheet analyteSheet = workbook.getSheet(sheetIndex);
                AnalyteInfo analyteInfo = new AnalyteInfo(analyteSheet.getName());

                int row = 0;
                while (!"".equals(analyteSheet.getCell(0, row++).getContents()));

                List<String> colNames = new ArrayList<String>();
                for (int col = 1; col < analyteSheet.getColumns(); col++)
                {
                    colNames.add(analyteSheet.getCell(col, row).getContents());
                }
                row++;

                for (; row < analyteSheet.getRows() && !"".equals(analyteSheet.getCell(0, row).getContents()); row++)
                {
                    Map<String, String> rowValues = new LinkedHashMap<String, String>();
                    for (int col = 1; col < analyteSheet.getColumns(); col++)
                    {
                        rowValues.put(colNames.get(col - 1), analyteSheet.getCell(col, row).getContents());
                    }
                    String specimenID = null;//thawMap.get(rowValues.get("Type"));
                    if (specimenID == null)
                    {
                        specimenID = rowValues.get("Type");
                    }
                    rowValues.put("SpecimenID", specimenID);

                    analyteInfo.addValue(rowValues);
                }
                run.addAnalyteInfo(analyteInfo);
            }

            run.getSpecimenInfos();
        }
        catch (IOException e)
        {
            throw new ExperimentException("Failed to read from data file " + dataFile.getAbsolutePath(), e);
        }
        catch (BiffException e)
        {
            throw new XarFormatException("Failed to parse Excel file " + dataFile.getAbsolutePath(), e);
        }

    }

    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        return null;
    }

    public void deleteData(Data data, Container container) throws ExperimentException
    {
        
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
