<%
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
%>
<p>
    Choose an export format:<br/>
    <input type="radio" name="exportFormat" value="Excel" checked="checked">Excel (limited to 65,535 rows)<br/>
    <input type="radio" name="exportFormat" value="ExcelBare">Excel with minimal header text (limited to 65,535 rows)<br/>
    <input type="radio" name="exportFormat" value="TSV">TSV<br/>
    <input type="radio" name="exportFormat" value="DTA">Spectra as DTA<br/>
    <input type="radio" name="exportFormat" value="PKL">Spectra as PKL<br/>
    <input type="radio" name="exportFormat" value="AMT">AMT (Accurate Mass &amp; Time) file<br/>
</p>