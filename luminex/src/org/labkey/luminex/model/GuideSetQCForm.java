/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.luminex.model;

import org.json.JSONObject;
import org.labkey.api.action.CustomApiForm;
import org.labkey.api.study.actions.ProtocolIdForm;

import java.util.Map;

/**
 * Created by aaronr on 11/13/14.
 */
//public class GuideSetQCForm extends SimpleApiJsonForm
// NOTE: do not think I need to implement CustomApiForm?
public class GuideSetQCForm extends ProtocolIdForm implements CustomApiForm
{
    private int _currentGuideSetId;
    private boolean _ec504plEnabled;
    private boolean _ec505plEnabled;
    private boolean _mfiEnabled;
    private boolean _aucEnabled;

    // JSONObjects and bindProperties ripped from SimpleApiJsonForm
    protected JSONObject json;

    public JSONObject getJsonObject()
    {
        return json;
    }

    // NOTE: why do I have to do this? Spring you suck.
    public void bindProperties(Map<String, Object> properties)
    {
        //super.bindProperties(properties);
        if (properties instanceof JSONObject)
            json = (JSONObject)properties;
        else
            json = new JSONObject(properties);

        JSONObject json = getJsonObject();
        if (json == null)
            throw new IllegalArgumentException("Empty request");

        this.setCurrentGuideSetId(getIntPropIfExists(json, "currentGuideSetId"));
        this.setEc504plEnabled(getBooleanPropIfExists(json, "ec504plEnabled"));
        this.setEc505plEnabled(getBooleanPropIfExists(json, "ec505plEnabled"));
        this.setMfiEnabled(getBooleanPropIfExists(json, "mfiEnabled"));
        this.setAucEnabled(getBooleanPropIfExists(json, "aucEnabled"));
        this.setAssayName(getStringPropIfExists(json, "assayName"));
    }

    // copy past from LuminexSaveExclusionsForm
    private String getStringPropIfExists(JSONObject json, String propName)
    {
        return json.containsKey(propName) ? json.getString(propName) : null;
    }

    private Integer getIntPropIfExists(JSONObject json, String propName)
    {
        return json.containsKey(propName) ? json.getInt(propName) : null;
    }

    private boolean getBooleanPropIfExists(JSONObject json, String propName)
    {
        // defaults to true if key not found
        return !json.containsKey(propName) || Boolean.parseBoolean(json.getString(propName));
    }

    public boolean isEc504plEnabled()
    {
        return _ec504plEnabled;
    }

    public void setEc504plEnabled(boolean ec504plEnabled)
    {
        _ec504plEnabled = ec504plEnabled;
    }

    public boolean isEc505plEnabled()
    {
        return _ec505plEnabled;
    }

    public void setEc505plEnabled(boolean ec505plEnabled)
    {
        _ec505plEnabled = ec505plEnabled;
    }

    public boolean isMfiEnabled()
    {
        return _mfiEnabled;
    }

    public void setMfiEnabled(boolean mfiEnabled)
    {
        _mfiEnabled = mfiEnabled;
    }

    public boolean isAucEnabled()
    {
        return _aucEnabled;
    }

    public void setAucEnabled(boolean aucEnabled)
    {
        _aucEnabled = aucEnabled;
    }

    public int getCurrentGuideSetId()
    {
        return _currentGuideSetId;
    }

    public void setCurrentGuideSetId(int currentGuideSetId)
    {
        _currentGuideSetId = currentGuideSetId;
    }

}
