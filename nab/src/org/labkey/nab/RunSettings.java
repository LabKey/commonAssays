/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

import org.labkey.api.assay.dilution.SafeTextConverter;

import java.io.Serializable;

/**
 * User: brittp
 * Date: Aug 31, 2006
 * Time: 8:05:57 PM
 */
public class RunSettings implements Serializable
{
    public static final int MAX_CUTOFF_OPTIONS = 3;
    private static final long serialVersionUID = -8338877129594854954L;
    private boolean _inferFromFile;
    private boolean _sameInitialValue;
    private boolean _sameMethod;
    private boolean _sameFactor;
    private SafeTextConverter.PercentConverter[] _cutoffs = new SafeTextConverter.PercentConverter[MAX_CUTOFF_OPTIONS];

    public RunSettings()
    {
        this(false);
    }

    /**
     * This is a hacky constructor to handle the fact that HTML posts simply exclude
     * unchecked checkboxes.  Since I want this form to provide the default values
     * for the HTML page, it's necessary to differentiate between the case of a reshow
     * after a post containing unchecked boxes and the case of initially showing the page.
     * In both cases, no setter will ever be called for the boolean parameters, but I want
     * to return different values.  Note that this is only a problem for checkboxes that
     * should default to true.
     *
     * @param returnDefaultForUnsetBools
     */
    public RunSettings(boolean returnDefaultForUnsetBools)
    {
        _inferFromFile = returnDefaultForUnsetBools;
        _sameInitialValue = returnDefaultForUnsetBools;
        _sameMethod = returnDefaultForUnsetBools;
        _sameFactor = returnDefaultForUnsetBools;

        _cutoffs[0] = new SafeTextConverter.PercentConverter(50);
        _cutoffs[1] = new SafeTextConverter.PercentConverter(80);
        for (int i = 2; i < MAX_CUTOFF_OPTIONS; i++)
            _cutoffs[i] = new SafeTextConverter.PercentConverter(null);
    }

    public boolean isInferFromFile()
    {
        return _inferFromFile;
    }

    public void setInferFromFile(boolean inferFromFile)
    {
        _inferFromFile = inferFromFile;
    }


    public boolean isSameMethod()
    {
        return _sameMethod;
    }

    public void setSameMethod(boolean sameMethod)
    {
        _sameMethod = sameMethod;
    }

    public boolean isSameFactor()
    {
        return _sameFactor;
    }

    public void setSameFactor(boolean sameFactor)
    {
        _sameFactor = sameFactor;
    }

    public boolean isSameInitialValue()
    {
        return _sameInitialValue;
    }

    public void setSameInitialValue(boolean sameInitialValue)
    {
        _sameInitialValue = sameInitialValue;
    }

    public SafeTextConverter.PercentConverter[] getCutoffs()
    {
        return _cutoffs;
    }

    public void setCutoffs(SafeTextConverter.PercentConverter[] cutoffs)
    {
        _cutoffs = cutoffs;
    }
}
