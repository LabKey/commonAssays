/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.ms2.peptideview;

import org.labkey.api.data.Container;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.ProteinDisplayColumn;

import java.util.HashMap;
import java.util.Map;

/**
 * User: jeckels
* Date: Apr 6, 2007
*/
public class ProteinDisplayColumnFactory implements DisplayColumnFactory
{
    private final Container _container;
    private final String _url;

    public ProteinDisplayColumnFactory(Container container)
    {
        this(container, null);
    }

    public ProteinDisplayColumnFactory(Container container, String url)
    {
        _container = container;
        _url = url;
    }

    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        ActionURL detailsURL = new ActionURL(MS2Controller.ShowProteinAJAXAction.class, _container);
        Map<String, FieldKey> params = new HashMap<String, FieldKey>();
        params.put("seqId", new FieldKey(colInfo.getFieldKey().getParent(), "SeqId"));
        params.put("run", new FieldKey(new FieldKey(colInfo.getFieldKey().getParent(), "Fraction"), "Run"));
        ProteinDisplayColumn result = new ProteinDisplayColumn(colInfo, detailsURL, params);
        if (_url != null)
        {
            result.setURL(_url);
        }
        return result;
    }
}
