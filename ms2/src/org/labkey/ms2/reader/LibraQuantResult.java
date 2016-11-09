/*
 * Copyright (c) 2011-2016 LabKey Corporation
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

import java.sql.SQLException;
import java.sql.Types;

/**
 * User: jeckels
 * Date: Aug 14, 2011
 */
public class LibraQuantResult extends AbstractQuantAnalysisResult
{
    private Double _targetMass1;
    private Double _absoluteIntensity1;
    private Double _normalized1;
    private Double _targetMass2;
    private Double _absoluteIntensity2;
    private Double _normalized2;
    private Double _targetMass3;
    private Double _absoluteIntensity3;
    private Double _normalized3;
    private Double _targetMass4;
    private Double _absoluteIntensity4;
    private Double _normalized4;
    private Double _targetMass5;
    private Double _absoluteIntensity5;
    private Double _normalized5;
    private Double _targetMass6;
    private Double _absoluteIntensity6;
    private Double _normalized6;
    private Double _targetMass7;
    private Double _absoluteIntensity7;
    private Double _normalized7;
    private Double _targetMass8;
    private Double _absoluteIntensity8;
    private Double _normalized8;
    private Double _targetMass9;
    private Double _absoluteIntensity9;
    private Double _normalized9;
    private Double _targetMass10;
    private Double _absoluteIntensity10;
    private Double _normalized10;

    public Double getTargetMass1()
    {
        return _targetMass1;
    }

    public void setTargetMass1(Double targetMass1)
    {
        _targetMass1 = targetMass1;
    }

    public Double getAbsoluteIntensity1()
    {
        return _absoluteIntensity1;
    }

    public void setAbsoluteIntensity1(Double absoluteIntensity1)
    {
        _absoluteIntensity1 = absoluteIntensity1;
    }

    public Double getNormalized1()
    {
        return _normalized1;
    }

    public void setNormalized1(Double normalized1)
    {
        _normalized1 = normalized1;
    }

    public Double getTargetMass2()
    {
        return _targetMass2;
    }

    public void setTargetMass2(Double targetMass2)
    {
        _targetMass2 = targetMass2;
    }

    public Double getAbsoluteIntensity2()
    {
        return _absoluteIntensity2;
    }

    public void setAbsoluteIntensity2(Double absoluteIntensity2)
    {
        _absoluteIntensity2 = absoluteIntensity2;
    }

    public Double getNormalized2()
    {
        return _normalized2;
    }

    public void setNormalized2(Double normalized2)
    {
        _normalized2 = normalized2;
    }

    public Double getTargetMass3()
    {
        return _targetMass3;
    }

    public void setTargetMass3(Double targetMass3)
    {
        _targetMass3 = targetMass3;
    }

    public Double getAbsoluteIntensity3()
    {
        return _absoluteIntensity3;
    }

    public void setAbsoluteIntensity3(Double absoluteIntensity3)
    {
        _absoluteIntensity3 = absoluteIntensity3;
    }

    public Double getNormalized3()
    {
        return _normalized3;
    }

    public void setNormalized3(Double normalized3)
    {
        _normalized3 = normalized3;
    }

    public Double getTargetMass4()
    {
        return _targetMass4;
    }

    public void setTargetMass4(Double targetMass4)
    {
        _targetMass4 = targetMass4;
    }

    public Double getAbsoluteIntensity4()
    {
        return _absoluteIntensity4;
    }

    public void setAbsoluteIntensity4(Double absoluteIntensity4)
    {
        _absoluteIntensity4 = absoluteIntensity4;
    }

    public Double getNormalized4()
    {
        return _normalized4;
    }

    public void setNormalized4(Double normalized4)
    {
        _normalized4 = normalized4;
    }

    public Double getTargetMass5()
    {
        return _targetMass5;
    }

    public void setTargetMass5(Double targetMass5)
    {
        _targetMass5 = targetMass5;
    }

    public Double getAbsoluteIntensity5()
    {
        return _absoluteIntensity5;
    }

    public void setAbsoluteIntensity5(Double absoluteIntensity5)
    {
        _absoluteIntensity5 = absoluteIntensity5;
    }

    public Double getNormalized5()
    {
        return _normalized5;
    }

    public void setNormalized5(Double normalized5)
    {
        _normalized5 = normalized5;
    }

    public Double getTargetMass6()
    {
        return _targetMass6;
    }

    public void setTargetMass6(Double targetMass6)
    {
        _targetMass6 = targetMass6;
    }

    public Double getAbsoluteIntensity6()
    {
        return _absoluteIntensity6;
    }

    public void setAbsoluteIntensity6(Double absoluteIntensity6)
    {
        _absoluteIntensity6 = absoluteIntensity6;
    }

    public Double getNormalized6()
    {
        return _normalized6;
    }

    public void setNormalized6(Double normalized6)
    {
        _normalized6 = normalized6;
    }

    public Double getTargetMass7()
    {
        return _targetMass7;
    }

    public void setTargetMass7(Double targetMass7)
    {
        _targetMass7 = targetMass7;
    }

    public Double getAbsoluteIntensity7()
    {
        return _absoluteIntensity7;
    }

    public void setAbsoluteIntensity7(Double absoluteIntensity7)
    {
        _absoluteIntensity7 = absoluteIntensity7;
    }

    public Double getNormalized7()
    {
        return _normalized7;
    }

    public void setNormalized7(Double normalized7)
    {
        _normalized7 = normalized7;
    }

    public Double getTargetMass8()
    {
        return _targetMass8;
    }

    public void setTargetMass8(Double targetMass8)
    {
        _targetMass8 = targetMass8;
    }

    public Double getAbsoluteIntensity8()
    {
        return _absoluteIntensity8;
    }

    public void setAbsoluteIntensity8(Double absoluteIntensity8)
    {
        _absoluteIntensity8 = absoluteIntensity8;
    }

    public Double getNormalized8()
    {
        return _normalized8;
    }

    public void setNormalized8(Double normalized8)
    {
        _normalized8 = normalized8;
    }

    public Double getTargetMass9()
    {
        return _targetMass9;
    }

    public void setTargetMass9(Double targetMass9)
    {
        _targetMass9 = targetMass9;
    }

    public Double getAbsoluteIntensity9()
    {
        return _absoluteIntensity9;
    }

    public void setAbsoluteIntensity9(Double absoluteIntensity9)
    {
        _absoluteIntensity9 = absoluteIntensity9;
    }

    public Double getNormalized9()
    {
        return _normalized9;
    }

    public void setNormalized9(Double normalized9)
    {
        _normalized9 = normalized9;
    }

    public Double getTargetMass10()
    {
        return _targetMass10;
    }

    public void setTargetMass10(Double targetMass10)
    {
        _targetMass10 = targetMass10;
    }

    public Double getAbsoluteIntensity10()
    {
        return _absoluteIntensity10;
    }

    public void setAbsoluteIntensity10(Double absoluteIntensity10)
    {
        _absoluteIntensity10 = absoluteIntensity10;
    }

    public Double getNormalized10()
    {
        return _normalized10;
    }

    public void setNormalized10(Double normalized10)
    {
        _normalized10 = normalized10;
    }

    @Override
    public String getAnalysisType()
    {
        return LibraQuantHandler.ANALYSIS_TYPE;
    }

    @Override
    public void insert(PeptideImporter importer) throws SQLException
    {
        int index = 1;
        importer._iTraqQuantStmt.setLong(index++, getPeptideId());

        if (getTargetMass1() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass1());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity1() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity1());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized1() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized1());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        if (getTargetMass2() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass2());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity2() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity2());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized2() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized2());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        if (getTargetMass3() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass3());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity3() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity3());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized3() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized3());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        if (getTargetMass4() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass4());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity4() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity4());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized4() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized4());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        if (getTargetMass5() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass5());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity5() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity5());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized5() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized5());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        if (getTargetMass6() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass6());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity6() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity6());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized6() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized6());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        if (getTargetMass7() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass7());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity7() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity7());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized7() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized7());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        if (getTargetMass8() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass8());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity8() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity8());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized8() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized8());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        if (getTargetMass9() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass9());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity9() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity9());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized9() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized9());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        if (getTargetMass10() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getTargetMass10());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getAbsoluteIntensity10() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getAbsoluteIntensity10());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }
        if (getNormalized10() != null)
        {
            importer._iTraqQuantStmt.setDouble(index++, getNormalized10());
        }
        else
        {
            importer._iTraqQuantStmt.setNull(index++, Types.DOUBLE);
        }

        importer._iTraqQuantStmt.executeUpdate();
    }

    public String getMatch(float mz, double tolerance)
    {
        if (_targetMass1 != null && Math.abs(mz - _targetMass1.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 1";
        }
        if (_targetMass2 != null && Math.abs(mz - _targetMass2.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 2";
        }
        if (_targetMass3 != null && Math.abs(mz - _targetMass3.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 3";
        }
        if (_targetMass4 != null && Math.abs(mz - _targetMass4.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 4";
        }
        if (_targetMass5 != null && Math.abs(mz - _targetMass5.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 5";
        }
        if (_targetMass6 != null && Math.abs(mz - _targetMass6.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 6";
        }
        if (_targetMass7 != null && Math.abs(mz - _targetMass7.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 7";
        }
        if (_targetMass8 != null && Math.abs(mz - _targetMass8.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 8";
        }
        if (_targetMass9 != null && Math.abs(mz - _targetMass9.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 9";
        }
        if (_targetMass10 != null && Math.abs(mz - _targetMass10.doubleValue()) < tolerance)
        {
            return "iTRAQ Channel 10";
        }
        return null;
    }
}
