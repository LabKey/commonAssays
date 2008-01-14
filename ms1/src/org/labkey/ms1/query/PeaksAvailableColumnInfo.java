/*
 * Copyright (c) 2007 LabKey Software Foundation
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

import org.labkey.api.query.ExprColumn;
import org.labkey.api.data.*;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Manager;

/**
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 11, 2008
 * Time: 2:51:07 PM
 */
public class PeaksAvailableColumnInfo extends ExprColumn implements DisplayColumnFactory
{
    public static final String COLUMN_NAME = "PeaksAvailable";

    public PeaksAvailableColumnInfo(TableInfo parent)
    {
        super(parent, COLUMN_NAME,
                new SQLFragment("(SELECT COUNT(f.FileId) FROM ms1.Files AS f\n" +
                "INNER JOIN exp.Data AS d ON (f.ExpDataFileId=d.RowId)\n" +
                "WHERE f.Type=" + MS1Manager.FILETYPE_PEAKS + " AND d.RunId=\n" +
                "(SELECT RunId FROM exp.Data AS d\n" +
                "INNER JOIN ms1.Files AS f ON (d.RowId=f.ExpDataFileId)\n" +
                "WHERE f.FileId=" + ExprColumn.STR_TABLE_ALIAS + ".FileId))"),
                java.sql.Types.INTEGER, parent.getColumn("FeatureId"));

        setCaption("");
        setDisplayColumnFactory(this);
    }

    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new PeakLinksDisplayColumn(this);
    }
}
