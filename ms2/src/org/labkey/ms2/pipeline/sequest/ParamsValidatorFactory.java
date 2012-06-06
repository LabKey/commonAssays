/*
 * Copyright (c) 2006-2012 LabKey Corporation
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

package org.labkey.ms2.pipeline.sequest;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 1:06:34 PM
 */
public class ParamsValidatorFactory
{
    private static BooleanParamsValidator _boolean;
    private static NaturalNumberParamsValidator _naturalNumber;
    private static PositiveDoubleParamsValidator _positiveDouble;
    private static RealNumberParamsValidator _realNumber;
    private static NonNegativeIntegerParamsValidator _nonNegativeInteger;
    private static ListParamsValidator _listParamsValidator;


    public static IParamsValidator getBooleanParamsValidator()
    {
        if (_boolean == null)
        {
            _boolean = new BooleanParamsValidator();
        }
        return _boolean;
    }

    public static IParamsValidator getNaturalNumberParamsValidator()
    {
        if (_naturalNumber == null)
        {
            _naturalNumber = new NaturalNumberParamsValidator();
        }
        return _naturalNumber;
    }

    public static IParamsValidator getPositiveDoubleParamsValidator()
    {
        if (_positiveDouble == null)
        {
            _positiveDouble = new PositiveDoubleParamsValidator();
        }
        return _positiveDouble;
    }

    public static IParamsValidator getRealNumberParamsValidator()
    {
        if (_realNumber == null)
        {
            _realNumber = new RealNumberParamsValidator();
        }
        return _realNumber;
    }

    public static IParamsValidator getPositiveIntegerParamsValidator()
    {
        if (_nonNegativeInteger == null)
        {
            _nonNegativeInteger = new NonNegativeIntegerParamsValidator();
        }
        return _nonNegativeInteger;
    }

    public static ListParamsValidator getListParamsValidator(String... list)
    {
        if(_listParamsValidator == null)
        {
            _listParamsValidator = new ListParamsValidator(list);
        }
        return _listParamsValidator;
    }
}
