/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.nab;

import org.labkey.api.util.DateUtil;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
* User: brittp
* Date: Jun 26, 2009
* Time: 10:22:06 AM
*/
public class NabMaterialKey
{
    private String _specimenId;
    private String _participantId;
    private Double _visitId;
    private Date _date;

    public NabMaterialKey(String specimenId, String participantId, Double visitId, Date date)
    {
        _specimenId = specimenId;
        _participantId = participantId;
        _visitId = visitId;
        _date = date;
    }

    private void appendAndSeparate(StringBuilder builder, String append)
    {
        if (append != null)
        {
            if (builder.length() > 0)
                builder.append(", ");
            builder.append(append);
        }
    }

    public String getDisplayString(boolean longForm)
    {
        if (!longForm)
        {
            if (_specimenId != null)
                return _specimenId;
            else if (_visitId == null && _date != null)
            {
                if (_date.getHours() == 0 && _date.getMinutes() == 0 && _date.getSeconds() == 0)
                    return _participantId + ", " + DateUtil.formatDate(_date);
                else
                    return _participantId + ", " + DateUtil.formatDateTime(_date);
            }
            else
                return _participantId + ", Vst " + _visitId;
        }
        else
        {
            StringBuilder builder = new StringBuilder();
            appendAndSeparate(builder, _specimenId);
            appendAndSeparate(builder, _participantId);
            if (_visitId == null && _date != null)
            {
                if (_date.getHours() == 0 && _date.getMinutes() == 0 && _date.getSeconds() == 0)
                    appendAndSeparate(builder, DateUtil.formatDate(_date));
                else
                    appendAndSeparate(builder, DateUtil.formatDateTime(_date));
            }
            else if (_visitId != null)
                appendAndSeparate(builder, "Vst " + _visitId);
            return builder.toString();
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NabMaterialKey that = (NabMaterialKey) o;

        if (_date != null ? !_date.equals(that._date) : that._date != null) return false;
        if (_participantId != null ? !_participantId.equals(that._participantId) : that._participantId != null)
            return false;
        if (_specimenId != null ? !_specimenId.equals(that._specimenId) : that._specimenId != null) return false;
        if (_visitId != null ? !_visitId.equals(that._visitId) : that._visitId != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = _specimenId != null ? _specimenId.hashCode() : 0;
        result = 31 * result + (_participantId != null ? _participantId.hashCode() : 0);
        result = 31 * result + (_visitId != null ? _visitId.hashCode() : 0);
        result = 31 * result + (_date != null ? _date.hashCode() : 0);
        return result;
    }
}
