/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.ms2.compare;

import org.labkey.api.data.TableInfo;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.SpectraCountConfiguration;

/**
 * User: jeckels
* Date: Jan 22, 2008
*/
public class SpectraCountQueryView extends QueryView
{
    private final MS2Schema _schema;
    private final SpectraCountConfiguration _config;
    private MS2Controller.SpectraCountForm _form;

    public SpectraCountQueryView(MS2Schema schema, QuerySettings settings, SpectraCountConfiguration config, MS2Controller.SpectraCountForm form)
    {
        super(schema, settings);
        _schema = schema;
        _config = config;
        _form = form;

        setViewItemFilter(new ReportService.ItemFilter() {
            public boolean accept(String type, String label)
            {
                if (SpectraCountRReport.TYPE.equals(type)) return true;
                return false;
            }
        });
    }

    protected TableInfo createTable()
    {
        return _schema.createSpectraCountTable(_config, getViewContext(), _form);
    }
}
