package org.labkey.ms2.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.DOM;
import com.google.gwt.core.client.GWT;
import org.labkey.api.gwt.client.util.ServiceUtil;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.api.gwt.client.ui.ImageButton;
import org.labkey.api.gwt.client.ui.WebPartPanel;

import java.util.List;
import java.util.ArrayList;

/**
 * User: jeckels
 * Date: Jun 6, 2007
 */
public class RunComparatorView extends HorizontalPanel
{
    private WebPartPanel _groupWebPart;
    private WebPartPanel _overlapWebPart;
    private Grid _groupGrid;
    private Grid _overlapGrid;
    private List _runGroups = new ArrayList();
    private boolean[][] _hits;
    private String[] _runNames;
    private String[] _runURLs;

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

        _groupWebPart = new WebPartPanel("Run Groups", new Label("Loading..."));
        panel.add(_groupWebPart);

        _overlapWebPart = new WebPartPanel("Number of Overlapping " + PropertyUtil.getServerProperty("comparisonName"), new Label("Loading..."));
        panel.add(_overlapWebPart);

        rootPanel.add(panel);
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
        _runNames = compareResult.getRunNames();
        _runURLs = compareResult.getRunURLs();
        _hits = compareResult.getHits();

        _runGroups.clear();
        for (int i = 0; i < _runNames.length; i++)
        {
            int[] runIndices = new int[1];
            runIndices[0] = i;
            _runGroups.add(new RunGroup(runIndices, false));
        }

        refreshTables();
    }

    private void refreshGroupsTable()
    {
        _groupGrid.resize(_runGroups.size() + 2, 6);

        _groupGrid.setCellPadding(2);
        _groupGrid.setCellSpacing(0);
        Label runGroupNumberLabel = new Label("#");
        _groupGrid.setWidget(0, 0, runGroupNumberLabel);
        DOM.setStyleAttribute(runGroupNumberLabel.getElement(), "fontWeight", "bold");

        Label runsInGroupLabel = new Label("Runs in Group");
        _groupGrid.setWidget(0, 1, runsInGroupLabel);
        DOM.setStyleAttribute(runsInGroupLabel.getElement(), "fontWeight", "bold");

        Label typeLabel = new Label("Type");
        _groupGrid.setWidget(0, 2, typeLabel);
        DOM.setStyleAttribute(typeLabel.getElement(), "fontWeight", "bold");

        Label proteinCountLabel = new Label("Proteins");
        _groupGrid.setWidget(0, 3, proteinCountLabel);
        DOM.setStyleAttribute(proteinCountLabel.getElement(), "fontWeight", "bold");

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

        for (int x = 0; x < _runGroups.size(); x++)
        {
            _groupGrid.setWidget(x + 1, 0, new Label(Integer.toString(x + 1)));
            RunGroup runGroup = (RunGroup)_runGroups.get(x);
            int[] runIndices = runGroup.getRunIndices();
            String separator = "";
            String name = "";
            for (int i = 0; i < runIndices.length; i++)
            {
                name = name + separator + "<a href=\"" + _runURLs[runIndices[i]] + "\">" + _runNames[runIndices[i]] + "</a>";
                separator = "<br/>";
            }
            _groupGrid.setWidget(x + 1, 1, new HTML(name));

            _groupGrid.setWidget(x + 1, 2, new Label(runGroup.isRequireAll() ? "ALL" : "ANY"));

            Label groupProteinCountLabel = new Label(Integer.toString(runGroup.getProteinCount(_hits)));
            _groupGrid.setWidget(x + 1, 3, groupProteinCountLabel);
            DOM.setStyleAttribute(groupProteinCountLabel.getElement(), "textAlign", "right");
            DOM.setStyleAttribute(groupProteinCountLabel.getElement(), "fontFamily", "courier");


            final int groupIndex = x;
            ImageButton editButton = new ImageButton("Edit", new ClickListener()
            {
                public void onClick(Widget sender)
                {
                    RunGroupDialogBox box = new RunGroupDialogBox(_runNames, (RunGroup) _runGroups.get(groupIndex))
                    {
                        public void commit(RunGroup runGroup)
                        {
                            _runGroups.set(groupIndex, runGroup);
                            refreshTables();
                        }
                    };
                    box.show();
                }
            });
            _groupGrid.setWidget(x + 1, 4, editButton);

            ImageButton deleteButton = new ImageButton("Delete", new ClickListener()
            {
                public void onClick(Widget sender)
                {
                    _runGroups.remove(groupIndex);
                    refreshTables();
                }
            });
            _groupGrid.setWidget(x + 1, 5, deleteButton);
        }

        _groupGrid.setWidget(_runGroups.size() + 1, 1, new ImageButton("New Group", new ClickListener()
        {
            public void onClick(Widget sender)
            {
                RunGroupDialogBox box = new RunGroupDialogBox(_runNames, new RunGroup())
                {
                    public void commit(RunGroup runGroup)
                    {
                        _runGroups.add(runGroup);
                        refreshTables();
                    }
                };
                box.show();
            }
        }));

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

    private void refreshOverlapTable()
    {
        _overlapGrid.clear();
        _overlapGrid.resize(_runGroups.size(), _runGroups.size());

        for (int row = 1; row < _runGroups.size(); row++)
        {
            final int groupIndex = row;

            Label rowGroupLabel = new Label("Group #" + (row + 1));
            rowGroupLabel.addMouseListener(new MouseListenerAdapter()
            {
                public void onMouseEnter(Widget sender)
                {
                    highlightGroupMembers("#DDDDDD", groupIndex, null, -1);
                }

                public void onMouseLeave(Widget sender)
                {
                    highlightGroupMembers("#FFFFFF", groupIndex, null, -1);
                }
            });
            DOM.setStyleAttribute(rowGroupLabel.getElement(), "fontWeight", "bold");
            DOM.setStyleAttribute(rowGroupLabel.getElement(), "fontFamily", "courier");
            DOM.setStyleAttribute(rowGroupLabel.getElement(), "textAlign", "right");

            _overlapGrid.setWidget(row, 0, rowGroupLabel);

            for (int col = 0; col < _runGroups.size() - 1; col++)
            {
                final int group2Index = col;
                Label colGroupLabel = new Label("Group #" + (col + 1));
                colGroupLabel.addMouseListener(new MouseListenerAdapter()
                {
                    public void onMouseEnter(Widget sender)
                    {
                        highlightGroupMembers(null, -1, "#DDDDDD", group2Index);
                    }

                    public void onMouseLeave(Widget sender)
                    {
                        highlightGroupMembers(null, -1, "#FFFFFF", group2Index);
                    }
                });
                DOM.setStyleAttribute(colGroupLabel.getElement(), "fontWeight", "bold");
                DOM.setStyleAttribute(colGroupLabel.getElement(), "fontFamily", "courier");
                DOM.setStyleAttribute(colGroupLabel.getElement(), "textAlign", "right");
                _overlapGrid.setWidget(0, col + 1, colGroupLabel);

                if (col < row)
                {
                    int proteinCount = 0;
                    for (int p = 0; p < _hits.length; p++)
                    {
                        if (((RunGroup)_runGroups.get(row)).hasProtein(p, _hits) && ((RunGroup)_runGroups.get(col)).hasProtein(p, _hits))
                        {
                            proteinCount++;
                        }
                    }
                    Label label = new Label(Integer.toString(proteinCount));
                    DOM.setStyleAttribute(label.getElement(), "fontFamily", "courier");
                    DOM.setStyleAttribute(label.getElement(), "textAlign", "right");
                    label.addMouseListener(new MouseListenerAdapter()
                    {
                        public void onMouseEnter(Widget sender)
                        {
                            String group1Color = "#FFDDDD";
                            String group2Color = "#DDFFDD";
                            if (groupIndex == group2Index)
                            {
                                group1Color = group2Color = "#DDDDDD";
                            }

                            highlightGroupMembers(group1Color, groupIndex, group2Color, group2Index);
                        }

                        public void onMouseLeave(Widget sender)
                        {
                            highlightGroupMembers("#FFFFFF", groupIndex, "#FFFFFF", group2Index);
                        }
                    });
                    _overlapGrid.setWidget(row, col + 1, label);
                }
                else
                {
                    Label label = new Label("-");
                    DOM.setStyleAttribute(label.getElement(), "fontFamily", "courier");
                    DOM.setStyleAttribute(label.getElement(), "textAlign", "right");
                    _overlapGrid.setWidget(row, col + 1, label);
                }
            }
        }
    }

    private void refreshTables()
    {
        _groupGrid = new Grid();
        _overlapGrid = new Grid();
        
        refreshGroupsTable();
        refreshOverlapTable();

        _groupWebPart.setContent(_groupGrid);
        _overlapWebPart.setContent(_overlapGrid);
    }

    private void highlightGroupMembers(String color1, int group1Index, String color2, int group2Index)
    {
        if (color1 != null && group1Index >= 0)
        {
            toggleHighlight(_groupGrid.getWidget(group1Index + 1, 1), color1);
            toggleHighlight(_groupGrid.getWidget(group1Index + 1, 0), color1);
            toggleHighlight(_overlapGrid.getWidget(group1Index, 0), color1);
        }

        if (color2 != null && group2Index >= 0)
        {
            toggleHighlight(_groupGrid.getWidget(group2Index + 1, 1), color2);
            toggleHighlight(_groupGrid.getWidget(group2Index + 1, 0), color2);
            toggleHighlight(_overlapGrid.getWidget(0, group2Index + 1), color2);
        }
    }

    private void toggleHighlight(Widget widget, String color)
    {
        DOM.setStyleAttribute(widget.getElement(), "backgroundColor", color);
    }

}
