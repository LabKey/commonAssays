/*
 * Copyright (c) 2008-2013 LabKey Corporation
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
package org.labkey.ms1.query;

import org.labkey.api.data.*;
import org.labkey.api.query.ExprColumn;
import org.labkey.ms1.MS1Manager;

/**
 * User: Dave
 * Date: Jan 11, 2008
 * Time: 2:51:07 PM
 */
public class PeaksAvailableColumnInfo extends ExprColumn implements DisplayColumnFactory
{
    public static final String COLUMN_NAME = "PeaksAvailable";
    public static final String COLUMN_CAPTION = "Details and Peaks Links";

    /**
     * Use this constructor if you don't know if peaks are available for all features or not.
     * This constructor will use a sub-query to determine if the peaks are available for each
     * feature, so it will be a little less efficient, but it will also display the links
     * conditionally on a row-by-row basis.
     *
     * @param parent parent table info.
     */
    public PeaksAvailableColumnInfo(TableInfo parent)
    {
        super(parent, COLUMN_NAME,
                new SQLFragment("(SELECT COUNT(f.FileId) FROM ms1.Files AS f\n" +
                "INNER JOIN exp.Data AS d ON (f.ExpDataFileId=d.RowId)\n" +
                "WHERE f.Type=" + MS1Manager.FILETYPE_PEAKS + " AND d.RunId=\n" +
                "(SELECT RunId FROM exp.Data AS d\n" +
                "INNER JOIN ms1.Files AS f ON (d.RowId=f.ExpDataFileId)\n" +
                "WHERE f.FileId=" + ExprColumn.STR_TABLE_ALIAS + ".FileId))"),
                JdbcType.INTEGER, parent.getColumn("FeatureId"));

        commonInit();
    }

    /**
     * Use this constructor when you know if the peaks are available or not for all features
     * that will be displayed (e.g., when filtering on an given experiment run).
     *
     * @param parent parent table info
     * @param peaksAvailable pass true if you know that peaks are available, or false if you know they are not
     */
    public PeaksAvailableColumnInfo(TableInfo parent, boolean peaksAvailable)
    {
        super(parent, COLUMN_NAME,
                new SQLFragment(peaksAvailable ? "(1)" : "(0)"),
                JdbcType.INTEGER);

        commonInit();
    }

    protected void commonInit()
    {
        setLabel(COLUMN_CAPTION);
        setDisplayColumnFactory(this);
    }

    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new PeakLinksDisplayColumn(colInfo);
    }
}
