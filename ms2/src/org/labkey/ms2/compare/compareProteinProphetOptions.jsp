<%
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
%>

<input type="hidden" name="column" value="ProteinProphet"/>
<table>
    <tr>
        <td colspan="2">Please select the columns to include in the comparison:</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="proteinGroup" value="1" checked disabled>Protein Group</td>
        <td><input type="checkbox" name="groupProbability" value="1" checked>Group Probability</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="light2HeavyRatioMean" value="1">Light to Heavy Quantitation</td>
        <td><input type="checkbox" name="heavy2LightRatioMean" value="1">Heavy to Light Quantitation</td>
    </tr>
    <tr>
        <td><input type="checkbox" name="totalPeptides" value="1">Total Peptides</td>
        <td><input type="checkbox" name="uniquePeptides" value="1">Unique Peptides</td>
    </tr>
</table>