package org.labkey.ms2.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.DOM;
import com.google.gwt.core.client.GWT;
import org.labkey.api.gwt.client.util.ServiceUtil;
import org.labkey.api.gwt.client.util.PropertyUtil;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * User: jeckels
 * Date: Jun 6, 2007
 */
public class RunComparatorView extends HorizontalPanel
{
    private static final String GROUP_A_COLOR = "ff9955";
    private static final String GROUP_B_COLOR = "7fc659";
    private static final String GROUP_C_COLOR = "75a4fb";
    private static final String OVERLAP_AB_COLOR = "7fa420";
    private static final String OVERLAP_AC_COLOR = "7582c2";
    private static final String OVERLAP_BC_COLOR = "4a91c4";
    private static final String OVERLAP_ABC_COLOR = "4a86b1";

    private FlexTable _groupWebPart;
    private Grid _groupGrid;
    private Image _vennDiagram;
    private ListBox[] _groupListBoxes = new ListBox[3];

    private Label _groupALabel = new Label(" ");
    private Label _groupBLabel = new Label(" ");
    private Label _groupCLabel = new Label(" ");

    private Label _groupACountLabel = new Label(" ");
    private Label _groupBCountLabel = new Label(" ");
    private Label _groupCCountLabel = new Label(" ");
    private Label _overlapABLabel = new Label(" ");
    private Label _overlapACLabel = new Label(" ");
    private Label _overlapBCLabel = new Label(" ");
    private Label _overlapABCLabel = new Label(" ");

    private static final String APPROXIMATE_CHART_DISCLAIMER = "Please note: diagram is only approximate.";

    private Label _warningLabel = new Label(APPROXIMATE_CHART_DISCLAIMER);

    private CompareRunGroup[] _runGroups;
    private boolean[][] _hits;
    private GWTExperimentRun[] _runs;
    private Map _idToRunGroup = new HashMap();

    private CompareServiceAsync _service;
    private CompareServiceAsync getService()
    {
        if (_service == null)
        {
            _service = (CompareServiceAsync) GWT.create(CompareService.class);
            ServiceUtil.configureEndpoint(_service, "compareService");
        }
        return _service;
    }


    public RunComparatorView(Panel rootPanel)
    {
        VerticalPanel panel = new VerticalPanel();
        panel.setVerticalAlignment(HasAlignment.ALIGN_TOP);
        panel.setHorizontalAlignment(HasAlignment.ALIGN_LEFT);

        _groupWebPart = new FlexTable();
        panel.add(_groupWebPart);

        _vennDiagram = new Image();
        _vennDiagram.setHeight("300px");
        _vennDiagram.setWidth("300px");
        _vennDiagram.setVisible(false);
        _vennDiagram.addLoadListener(new LoadListener()
        {
            public void onError(Widget sender)
            {
                _vennDiagram.setVisible(false);
                _warningLabel.setText("Failed to load Venn Diagram image. Only available if you have an active Internet connection.");
            }

            public void onLoad(Widget sender)
            {
                _vennDiagram.setVisible(true);
                _warningLabel.setText(APPROXIMATE_CHART_DISCLAIMER);
            }
        });

        ChangeListener changeListener = new ChangeListener()
        {
            public void onChange(Widget sender)
            {
                refreshDiagram();
            }
        };

        for (int i = 0; i < _groupListBoxes.length; i++)
        {
            _groupListBoxes[i] = new ListBox();
            _groupListBoxes[i].addChangeListener(changeListener);
        }

        FlexTable selectionTable = new FlexTable();
        
        selectionTable.setCellSpacing(5);
        int row = 0;
        int col = 0;

        Label label = new Label("Chart:");
        label.setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
        selectionTable.setWidget(row, col++, label);
        selectionTable.setWidget(row, col++, _groupListBoxes[0]);
        row++;
        col = 0;

        label = new Label("and:");
        label.setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
        selectionTable.setWidget(row, col++, label);
        selectionTable.setWidget(row, col++, _groupListBoxes[1]);
        row++;
        col = 0;

        label = new Label("and:");
        label.setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
        selectionTable.setWidget(row, col++, label);
        selectionTable.setWidget(row, col++, _groupListBoxes[2]);
        row++;
        col = 0;
        
        selectionTable.setWidget(row, col++, _groupACountLabel);
        selectionTable.setWidget(row, col++, _groupALabel);
        row++;
        col = 0;

        selectionTable.setWidget(row, col++, _groupBCountLabel);
        selectionTable.setWidget(row, col++, _groupBLabel);
        row++;
        col = 0;

        selectionTable.setWidget(row, col++, _groupCCountLabel);
        selectionTable.setWidget(row, col++, _groupCLabel);
        row++;
        col = 0;

        selectionTable.setWidget(row, col++, _overlapABLabel);
        selectionTable.setWidget(row, col++, new Label("Overlap between A and B"));
        row++;
        col = 0;

        selectionTable.setWidget(row, col++, _overlapACLabel);
        selectionTable.setWidget(row, col++, new Label("Overlap between A and C"));
        row++;
        col = 0;

        selectionTable.setWidget(row, col++, _overlapBCLabel);
        selectionTable.setWidget(row, col++, new Label("Overlap between B and C"));
        row++;
        col = 0;

        selectionTable.setWidget(row, col++, _overlapABCLabel);
        selectionTable.setWidget(row, col, new Label("Overlap between A, B, and C"));
        row++;
        col = 1;

        selectionTable.setWidget(row++, col, _warningLabel);

        setupCountLabel(_groupACountLabel, GROUP_A_COLOR);
        setupCountLabel(_groupBCountLabel, GROUP_B_COLOR);
        setupCountLabel(_groupCCountLabel, GROUP_C_COLOR);
        setupCountLabel(_overlapABLabel, OVERLAP_AB_COLOR);
        setupCountLabel(_overlapACLabel, OVERLAP_AC_COLOR);
        setupCountLabel(_overlapBCLabel, OVERLAP_BC_COLOR);
        setupCountLabel(_overlapABCLabel, OVERLAP_ABC_COLOR);

        HorizontalPanel diagramPanel = new HorizontalPanel();
        diagramPanel.setVerticalAlignment(HasAlignment.ALIGN_TOP);
        diagramPanel.setHorizontalAlignment(HasAlignment.ALIGN_LEFT);
        diagramPanel.add(selectionTable);
        diagramPanel.add(_vennDiagram);

        panel.add(diagramPanel);

        rootPanel.add(panel);

        refreshDiagram();
    }

    private void setupCountLabel(Widget widget, String color)
    {
        DOM.setStyleAttribute(widget.getElement(), "textAlign", "right");
        DOM.setStyleAttribute(widget.getElement(), "padding", "2px");
        DOM.setStyleAttribute(widget.getElement(), "fontFamily", "courier");
        if (color != null)
        {
            setBackground(widget, color);
        }
    }

    public void requestComparison()
    {
        String originalURL = PropertyUtil.getServerProperty("originalURL");
        String comparisonGroup = PropertyUtil.getServerProperty("comparisonName");
        AsyncCallback callbackHandler = new AsyncCallback()
        {
            public void onFailure(Throwable caught)
            {
            }

            public void onSuccess(Object result)
            {
                setupTable((CompareResult) result);
            }
        };
        if ("Peptides".equalsIgnoreCase(comparisonGroup))
        {
            getService().getPeptideComparison(originalURL, callbackHandler);
        }
        else if ("Proteins".equalsIgnoreCase(comparisonGroup))
        {
            getService().getProteinProphetComparison(originalURL, callbackHandler);
        }
        else
        {
            throw new IllegalArgumentException("Unknown comparison group: " + comparisonGroup);
        }
    }

    private void setupTable(CompareResult compareResult)
    {
        _runs = compareResult.getRuns();
        _runGroups = new CompareRunGroup[compareResult.getRunGroups().length];
        for (int i = 0; i < compareResult.getRunGroups().length; i++)
        {
            GWTRunGroup runGroup = compareResult.getRunGroups()[i];
            List runs = runGroup.getRuns();
            int[] runIndices = new int[runs.size()];
            for (int j = 0; j < runs.size(); j++)
            {
                runIndices[j] = findRunIndex((GWTExperimentRun)runs.get(j));
            }

            _runGroups[i] = new CompareRunGroup(runGroup.getName(), runGroup.getURL(), runIndices, false);
        }

        for (int i = 0; i < _groupListBoxes.length; i++)
        {
            setupListBox(_groupListBoxes[i]);
        }

        int index = 0;
        // Select the first three run groups, if available
        while (index < _runGroups.length && index < 3)
        {
            _groupListBoxes[index].setSelectedIndex(index++);
        }

        // Fill up the rest of the selection with individual runs, if available
        while (index < _runs.length && index < 3)
        {
            // One blank line between the groups and the individual runs
            _groupListBoxes[index].setSelectedIndex(1 + index++);
        }

        _hits = compareResult.getHits();

        refreshTables();
    }

    private void setupListBox(ListBox listBox)
    {
        listBox.clear();
        _idToRunGroup = new HashMap();
        for (int i = 0; i < _runGroups.length; i++)
        {
            CompareRunGroup group = _runGroups[i];
            listBox.addItem(group.getName() + "  (Run Group)", "group" + i);
            _idToRunGroup.put("group" + i, group);
        }
        listBox.addItem("");
        for (int i = 0; i < _runs.length; i++)
        {
            CompareRunGroup group = new CompareRunGroup(_runs[i].getName(), null, new int[] { findRunIndex(_runs[i]) }, true);
            listBox.addItem(_runs[i].getName() + "  (Individual Run)", "run" + i);
            _idToRunGroup.put("run" + i, group);
        }
    }

    private int findRunIndex(GWTExperimentRun run)
    {
        for (int i = 0; i < _runs.length; i++)
        {
            if (run.getRowId() == _runs[i].getRowId())
            {
                return i;
            }
        }
        return -1;
    }

    private void refreshGroupsTable()
    {
        _groupGrid.resize(_runGroups.length + 2, 6);

        _groupGrid.setCellPadding(2);
        _groupGrid.setCellSpacing(0);
        Label runGroupNumberLabel = new Label("Group");
        _groupGrid.setWidget(0, 0, runGroupNumberLabel);
        DOM.setStyleAttribute(runGroupNumberLabel.getElement(), "fontWeight", "bold");

        Label runsInGroupLabel = new Label("Runs");
        _groupGrid.setWidget(0, 1, runsInGroupLabel);
        DOM.setStyleAttribute(runsInGroupLabel.getElement(), "fontWeight", "bold");

        for (int i = 0; i < _groupGrid.getColumnCount(); i++)
        {
            Widget widget = _groupGrid.getWidget(0, i);
            if (widget == null)
            {
                widget = new HTML("&nbsp;");
                _groupGrid.setWidget(0, i, widget);
            }
            DOM.setStyleAttribute(DOM.getParent(widget.getElement()), "borderBottom", "1px solid rgb(170, 170, 170)");
        }

        if (_runGroups.length == 0)
        {
            _groupGrid.setWidget(1, 1, new Label("None of the selected runs are part of a run group. You can configure run groups using the \"Add to run group\" button from the list of runs."));
        }

        for (int x = 0; x < _runGroups.length; x++)
        {
            CompareRunGroup runGroup = _runGroups[x];
            _groupGrid.setWidget(x + 1, 0, new HTML("<a href=\"" + runGroup.getUrl() + "\">" + runGroup.getName() + "</a>"));
            int[] runIndices = runGroup.getRunIndices();
            String separator = "";
            String name = "";
            for (int i = 0; i < runIndices.length; i++)
            {
                name = name + separator + "<a href=\"" + _runs[runIndices[i]].getUrl() + "\">" + _runs[runIndices[i]].getName() + "</a>";
                separator = "<br/>";
            }
            _groupGrid.setWidget(x + 1, 1, new HTML(name));
        }

        for (int row = 0; row < _groupGrid.getRowCount(); row++)
        {
            for (int col = 0; col < _groupGrid.getColumnCount(); col++)
            {
                Widget w = _groupGrid.getWidget(row, col);
                if (w != null)
                {
                    DOM.setStyleAttribute(DOM.getParent(w.getElement()), "verticalAlign", "top");
                }
            }
        }
    }

    private int getOverlapCount(CompareRunGroup group1, CompareRunGroup group2)
    {
        if (_hits == null)
        {
            return 0;
        }
        int result = 0;
        for (int p = 0; p < _hits.length; p++)
        {
            if ((group1 != null && group1.hasProtein(p, _hits)) && (group2 != null && group2.hasProtein(p, _hits)))
            {
                result++;
            }
        }
        return result;
    }

    private int getOverlapCount(CompareRunGroup group1, CompareRunGroup group2, CompareRunGroup group3)
    {
        if (_hits == null)
        {
            return 0;
        }
        int result = 0;
        for (int p = 0; p < _hits.length; p++)
        {
            if ((group1 != null && group1.hasProtein(p, _hits)) && (group2 != null && group2.hasProtein(p, _hits)) && (group3 != null && group3.hasProtein(p, _hits)))
            {
                result++;
            }
        }
        return result;
    }

    private void setGroupLabel(Label groupLabel, ListBox listBox, char groupLetter)
    {
        int index = listBox.getSelectedIndex();
        if (index == -1 || listBox.getItemText(index).length() == 0)
        {
            groupLabel.setText(groupLetter + ": (Nothing selected)");
        }
        else
        {
            groupLabel.setText(groupLetter + ": " + listBox.getItemText(index));
        }
    }

    private void refreshDiagram()
    {
        CompareRunGroup group1 = getSelectedRunGroup(_groupListBoxes[0]);
        CompareRunGroup group2 = getSelectedRunGroup(_groupListBoxes[1]);
        CompareRunGroup group3 = getSelectedRunGroup(_groupListBoxes[2]);

        int size1 = group1 == null ? 0 : group1.getProteinCount(_hits);
        int size2 = group2 == null ? 0 : group2.getProteinCount(_hits);
        int size3 = group3 == null ? 0 : group3.getProteinCount(_hits);

        int sizeA;
        int sizeB;
        int sizeC;
        CompareRunGroup groupA;
        CompareRunGroup groupB;
        CompareRunGroup groupC;
        // Need to reorder the groups since Google Charts chooses colors based on size
        if (size1 >= size2 && size1 >= size3)
        {
            // 1 is biggest
            sizeA = size1;
            groupA = group1;
            setGroupLabel(_groupALabel, _groupListBoxes[0], 'A');
            if (size2 >= size3)
            {
                // 2 is middle, 3 is smallest
                sizeB = size2;
                groupB = group2;
                setGroupLabel(_groupBLabel, _groupListBoxes[1], 'B');
                sizeC = size3;
                groupC = group3;
                setGroupLabel(_groupCLabel, _groupListBoxes[2], 'C');
            }
            else
            {
                // 3 is middle, 2 is smallest
                sizeB = size3;
                groupB = group3;
                setGroupLabel(_groupBLabel, _groupListBoxes[2], 'B');
                sizeC = size2;
                groupC = group2;
                setGroupLabel(_groupCLabel, _groupListBoxes[1], 'C');
            }
        }
        else if (size2 >= size1 && size2 >= size3)
        {
            // 2 is biggest
            sizeA = size2;
            groupA = group2;
            setGroupLabel(_groupALabel, _groupListBoxes[1], 'A');
            if (size1 >= size3)
            {
                // 1 is middle, 3 is smallest
                sizeB = size1;
                groupB = group1;
                setGroupLabel(_groupBLabel, _groupListBoxes[0], 'B');
                sizeC = size3;
                groupC = group3;
                setGroupLabel(_groupCLabel, _groupListBoxes[2], 'C');
            }
            else
            {
                // 3 is middle, 1 is smallest
                sizeB = size3;
                groupB = group3;
                setGroupLabel(_groupBLabel, _groupListBoxes[2], 'B');
                sizeC = size1;
                groupC = group1;
                setGroupLabel(_groupCLabel, _groupListBoxes[0], 'C');
            }
        }
        else
        {
            // 3 is biggest
            sizeA = size3;
            groupA = group3;
            setGroupLabel(_groupALabel, _groupListBoxes[2], 'A');
            if (size1 >= size2)
            {
                // 1 is middle, 2 is smallest
                sizeB = size1;
                groupB = group1;
                setGroupLabel(_groupBLabel, _groupListBoxes[0], 'B');
                sizeC = size2;
                groupC = group2;
                setGroupLabel(_groupCLabel, _groupListBoxes[1], 'C');
            }
            else
            {
                // 2 is middle, 1 is smallest
                sizeB = size2;
                groupB = group2;
                setGroupLabel(_groupBLabel, _groupListBoxes[1], 'B');
                sizeC = size1;
                groupC = group1;
                setGroupLabel(_groupCLabel, _groupListBoxes[0], 'C');
            }
        }

        int overlapAB = getOverlapCount(groupA, groupB);
        int overlapAC = getOverlapCount(groupA, groupC);
        int overlapBC = getOverlapCount(groupB, groupC);
        int overlapABC = getOverlapCount(groupA, groupB, groupC);

        _groupACountLabel.setText(Integer.toString(sizeA));
        _groupBCountLabel.setText(Integer.toString(sizeB));
        _groupCCountLabel.setText(Integer.toString(sizeC));
        _overlapABLabel.setText(Integer.toString(overlapAB));
        _overlapACLabel.setText(Integer.toString(overlapAC));
        _overlapBCLabel.setText(Integer.toString(overlapBC));
        _overlapABCLabel.setText(Integer.toString(overlapABC));

        double maxSize = Math.max(sizeA, Math.max(sizeB, sizeC));
        double scale = maxSize / 100;

        int scaledSizeA = (int)(sizeA / scale);
        int scaledSizeB = (int)(sizeB / scale);
        int scaledSizeC = (int)(sizeC / scale);
        int scaledOverlapAB = (int)(overlapAB / scale);
        int scaledOverlapAC = (int)(overlapAC / scale);
        int scaledOverlapBC = (int)(overlapBC / scale);
        int scaledOverlapABC = (int)(overlapABC / scale);

        _vennDiagram.setUrl("http://chart.apis.google.com/chart?cht=v&chd=t:" + scaledSizeA + "," + scaledSizeB + "," + scaledSizeC + "," + scaledOverlapAB + "," + scaledOverlapAC + "," + scaledOverlapBC + "," + scaledOverlapABC + "&chs=300x300&chco=" + GROUP_A_COLOR + "|" + GROUP_B_COLOR + "|" + GROUP_C_COLOR);
    }

    private CompareRunGroup getSelectedRunGroup(ListBox listBox)
    {
        int index = listBox.getSelectedIndex();
        if (index == -1)
        {
            return null;
        }
        String id = listBox.getValue(index);
        return (CompareRunGroup)_idToRunGroup.get(id);
    }

    private void refreshTables()
    {
        _groupGrid = new Grid();
        
        refreshGroupsTable();
        refreshDiagram();

        _groupWebPart.setWidget(1, 0, _groupGrid);
    }

    private void highlightGroupMembers(String color1, int group1Index, String color2, int group2Index)
    {
        if (color1 != null && group1Index >= 0)
        {
            setBackground(_groupGrid.getWidget(group1Index + 1, 1), color1);
            setBackground(_groupGrid.getWidget(group1Index + 1, 0), color1);
//            setBackground(_overlapGrid.getWidget(group1Index, 0), color1);
        }

        if (color2 != null && group2Index >= 0)
        {
            setBackground(_groupGrid.getWidget(group2Index + 1, 1), color2);
            setBackground(_groupGrid.getWidget(group2Index + 1, 0), color2);
//            setBackground(_overlapGrid.getWidget(0, group2Index + 1), color2);
        }
    }

    private void setBackground(Widget widget, String color)
    {
        DOM.setStyleAttribute(widget.getElement(), "backgroundColor", color);
    }

}
