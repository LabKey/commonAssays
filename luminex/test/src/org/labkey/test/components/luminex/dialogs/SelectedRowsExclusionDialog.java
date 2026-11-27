/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.components.luminex.dialogs;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The dialog opened by the results grid's "Exclude Selected Rows" and "Remove Selected Exclusions" menu items.
 */
public class SelectedRowsExclusionDialog extends BaseExclusionDialog
{
    public static final String EXCLUDE_MENU_ITEM = "Exclude Selected Rows";
    public static final String REMOVE_MENU_ITEM = "Remove Selected Exclusions";
    public static final String EXCLUDE_TITLE = "Exclude Selected Rows from Analysis";
    public static final String REMOVE_TITLE = "Remove Selected Exclusions";
    private static final String DATA_FILE_HEADER = "Data File";

    private static final Locator TABLE_HEADER = Locator.css("#selectedrowsexclusions td.labkey-column-header");
    private static final Locator TABLE_ROW = Locator.css("#selectedrowsexclusions tr.labkey-row, #selectedrowsexclusions tr.labkey-alternate-row");
    private static final Locator SAVE_ENABLED = Locator.xpath("//table[@id='saveBtn' and not(contains(@class, 'disabled'))]");

    private final boolean _remove;

    /**
     * One row of the dialog's table; analytes is comma separated. notebookNo and plateId are null when the assay design has no NotebookNo run field
     * or PlateID Excel file run field, and dataFile is null when the dialog hides that column, as it does unless a run's rows span data files.
     */
    public record ExclusionRow(String run, @Nullable String notebookNo, @Nullable String plateId, @Nullable String dataFile, String description, String type,
                               String wells, String dilution, String analytes, String exclusionType)
    {
    }

    private SelectedRowsExclusionDialog(WebDriver driver, boolean remove)
    {
        super(driver);
        _remove = remove;
    }

    @Override
    protected void openDialog()
    {
        clickMenuItem(getDriver(), _remove);
        _extHelper.waitForExtDialog(_remove ? REMOVE_TITLE : EXCLUDE_TITLE);
        waitForElement(TABLE_ROW);
        waitForElement(SAVE_ENABLED);
    }

    /** Opens the dialog for the rows selected in the "Data" results grid. */
    public static SelectedRowsExclusionDialog beginAt(WebDriver driver, boolean remove)
    {
        SelectedRowsExclusionDialog dialog = new SelectedRowsExclusionDialog(driver, remove);
        dialog.openDialog();
        return dialog;
    }

    /** Clicks the menu item when the selection can't be excluded or removed, dismisses the alert and returns its message. */
    public static String getSelectionError(WebDriver driver, boolean remove)
    {
        SelectedRowsExclusionDialog dialog = new SelectedRowsExclusionDialog(driver, remove);
        clickMenuItem(driver, remove);
        dialog._extHelper.waitForExtDialog(remove ? REMOVE_TITLE : EXCLUDE_TITLE);
        dialog.waitForElementToBeVisible(Locator.extButton("OK"));
        String message = dialog.waitForElement(Locator.css(".ext-mb-text")).getText();
        dialog.clickButton("OK", 0);
        return message;
    }

    private static void clickMenuItem(WebDriver driver, boolean remove)
    {
        new DataRegionTable("Data", driver).clickHeaderMenu(MENU_BUTTON, false, remove ? REMOVE_MENU_ITEM : EXCLUDE_MENU_ITEM);
    }

    public List<ExclusionRow> getRows()
    {
        List<String> headers = getHeaders();
        return TABLE_ROW.findElements(getDriver()).stream().map(row -> {
            List<String> cells = Locator.tag("td").findElements(row).stream().map(WebElement::getText).toList();
            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < headers.size(); i++)
                values.put(headers.get(i), cells.get(i));
            String analytes = String.join(", ", values.get("Selected Analyte(s)").split("\n")); // one analyte per line
            return new ExclusionRow(values.get("Assay ID"), values.get("Notebook No."), values.get("Plate ID"), values.get(DATA_FILE_HEADER), values.get("Description"),
                    values.get("Type"), values.get("Well(s)"), values.get("Dilution"), analytes, values.get("Exclusion Type"));
        }).toList();
    }

    private List<String> getHeaders()
    {
        return TABLE_HEADER.findElements(getDriver()).stream().map(WebElement::getText).toList();
    }

    public SelectedRowsExclusionDialog setComment(String comment)
    {
        setFormElement(Locator.id("comment"), comment);
        return this;
    }

    /** Saves and answers the "go to the pipeline job?" prompt with Yes, leaving the browser on the pipeline status page. */
    public void saveAndGoToPipeline()
    {
        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Success", WAIT_FOR_JAVASCRIPT);
        clickButton("Yes");
    }
}
