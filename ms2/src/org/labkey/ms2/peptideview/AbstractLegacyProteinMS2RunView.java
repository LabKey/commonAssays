package org.labkey.ms2.peptideview;

import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.util.CaseInsensitiveHashMap;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.AACoverageColumn;
import org.labkey.ms2.MS2Manager;

import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Apr 27, 2007
 */
public abstract class AbstractLegacyProteinMS2RunView extends AbstractMS2RunView
{
    protected static Map<String, Class<? extends DisplayColumn>> _calculatedProteinColumns = new CaseInsensitiveHashMap<Class<? extends DisplayColumn>>();

    static
    {
        _calculatedProteinColumns.put("AACoverage", AACoverageColumn.class);
    }

    public AbstractLegacyProteinMS2RunView(ViewContext viewContext, String columnPropertyName, MS2Run... runs)
    {
        super(viewContext, columnPropertyName, runs);
    }

    public void exportToAMT(MS2Controller.ExportForm form, HttpServletResponse response, List<String> selectedRows) throws Exception
    {
        form.setColumns(AMT_PEPTIDE_COLUMN_NAMES);
        form.setExpanded(true);
        form.setProteinColumns("");
        exportToTSV(form, response, selectedRows);
    }

    protected abstract List<DisplayColumn> getProteinDisplayColumns(String requestedProteinColumnNames, boolean forExport) throws SQLException;

    public ProteinTSVGridWriter getTSVProteinGridWriter(String requestedProteinColumnNames, String requestedPeptideColumnNames, boolean expanded) throws SQLException
    {
        List<DisplayColumn> proteinDisplayColumns = getProteinDisplayColumns(requestedProteinColumnNames, true);
        List<DisplayColumn> peptideDisplayColumns = null;

        if (expanded)
        {
            peptideDisplayColumns = getPeptideDisplayColumns(getPeptideColumnNames(requestedPeptideColumnNames));
            changePeptideCaptionsForTsv(peptideDisplayColumns);
        }

        ProteinTSVGridWriter tw = getTSVProteinGridWriter(proteinDisplayColumns, peptideDisplayColumns);
        tw.setExpanded(expanded);
        return tw;
    }

    public abstract ProteinTSVGridWriter getTSVProteinGridWriter(List<DisplayColumn> proteinDisplayColumns, List<DisplayColumn> peptideDisplayColumns);

    protected DataRegion getNestedPeptideGrid(MS2Run run, String requestedPeptideColumnNames, boolean selectSeqId) throws SQLException
    {
        String columnNames = getPeptideColumnNames(requestedPeptideColumnNames);

        DataRegion rgn = getPeptideGrid(columnNames, _maxPeptideRows);

        if (selectSeqId && null == rgn.getDisplayColumn("SeqId"))
        {
            DisplayColumn seqId = MS2Manager.getTableInfoPeptides().getColumn("SeqId").getRenderer();
            seqId.setVisible(false);
            rgn.addColumn(seqId);
        }
        rgn.setTable(MS2Manager.getTableInfoPeptides());
        rgn.setName(MS2Manager.getDataRegionNamePeptides());

        rgn.setShowRecordSelectors(false);
        rgn.setFixedWidthColumns(true);

        ViewURLHelper showUrl = _url.clone();
        String seqId = showUrl.getParameter("seqId");
        showUrl.deleteParameter("seqId");
        String extraParams = "";
        if (selectSeqId)
        {
            extraParams = null == seqId ? "seqId=${SeqId}" : "seqId=" + seqId;
        }
        if ("proteinprophet".equals(_url.getParameter("grouping")) && _url.getParameter("proteinGroupId") == null)
        {
            extraParams = "proteinGroupId=${proteinGroupId}";
        }
        setPeptideUrls(rgn, extraParams);

        ButtonBar bb = new ButtonBar();
        bb.setVisible(false);  // Don't show button bar on nested region; also ensures no nested <form>...</form>
        rgn.setButtonBar(bb, DataRegion.MODE_GRID);

        return rgn;
    }
}
