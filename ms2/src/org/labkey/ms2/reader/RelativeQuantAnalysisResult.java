/*
 * Copyright (c) 2006-2016 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.reader;

import org.labkey.ms2.PeptideImporter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Models an analysis result for very minimal relative quantitation
 * of peptides. This is not intended to be instantiated; particular subclasses
 * must implement getAnalysisType();
 */
public abstract class RelativeQuantAnalysisResult extends AbstractQuantAnalysisResult
{
    public static final float SENTINEL_NAN = -1.f;
    public static final float SENTINEL_POSITIVE_INFINITY = 999.f;
    public static final float SENTINEL_NEGATIVE_INFINITY = -999.f;

    private int lightFirstscan;
    private int lightLastscan;
    private float lightMass;
    private int heavyFirstscan;
    private int heavyLastscan;
    private float heavyMass;
    private float lightArea;
    private float heavyArea;
    private float decimalRatio;

    public int getLightFirstscan()
    {
        return lightFirstscan;
    }

    public void setLightFirstscan(int lightFirstscan)
    {
        this.lightFirstscan = lightFirstscan;
    }

    public int getLightLastscan()
    {
        return lightLastscan;
    }

    public void setLightLastscan(int lightLastscan)
    {
        this.lightLastscan = lightLastscan;
    }

    public float getLightMass()
    {
        return lightMass;
    }

    public void setLightMass(float lightMass)
    {
        this.lightMass = lightMass;
    }

    public int getHeavyFirstscan()
    {
        return heavyFirstscan;
    }

    public void setHeavyFirstscan(int heavyFirstscan)
    {
        this.heavyFirstscan = heavyFirstscan;
    }

    public int getHeavyLastscan()
    {
        return heavyLastscan;
    }

    public void setHeavyLastscan(int heavyLastscan)
    {
        this.heavyLastscan = heavyLastscan;
    }

    public float getHeavyMass()
    {
        return heavyMass;
    }

    public void setHeavyMass(float heavyMass)
    {
        this.heavyMass = heavyMass;
    }

    public float getLightArea()
    {
        return lightArea;
    }

    public void setLightArea(float lightArea)
    {
        this.lightArea = lightArea;
    }

    public float getHeavyArea()
    {
        return heavyArea;
    }

    public void setHeavyArea(float heavyArea)
    {
        this.heavyArea = heavyArea;
    }

    public float getDecimalRatio()
    {
        return decimalRatio;
    }

    public void setDecimalRatio(float decimalRatio)
    {
        if (Float.isNaN(decimalRatio))
            this.decimalRatio = SENTINEL_NAN;
        else if (Float.isInfinite(decimalRatio))
            this.decimalRatio = decimalRatio > 0 ? SENTINEL_POSITIVE_INFINITY : SENTINEL_NEGATIVE_INFINITY;
        else
            this.decimalRatio = decimalRatio;
    }

    @Override
    public void insert(PeptideImporter pepXmlImporter) throws SQLException
    {
        int index = 1;
        pepXmlImporter._quantStmt.setLong(index++, getPeptideId());
        pepXmlImporter._quantStmt.setInt(index++, getLightFirstscan());
        pepXmlImporter._quantStmt.setInt(index++, getLightLastscan());
        pepXmlImporter._quantStmt.setFloat(index++, getLightMass());
        pepXmlImporter._quantStmt.setInt(index++, getHeavyFirstscan());
        pepXmlImporter._quantStmt.setInt(index++, getHeavyLastscan());
        pepXmlImporter._quantStmt.setFloat(index++, getHeavyMass());

        index = setRatios(pepXmlImporter._quantStmt, index);

        pepXmlImporter._quantStmt.setFloat(index++, getLightArea());
        pepXmlImporter._quantStmt.setFloat(index++, getHeavyArea());
        pepXmlImporter._quantStmt.setFloat(index++, getDecimalRatio());
        pepXmlImporter._quantStmt.setInt(index++, getQuantId());

        pepXmlImporter._quantStmt.executeUpdate();
    }

    protected abstract int setRatios(PreparedStatement stmt, int index) throws SQLException;
}
