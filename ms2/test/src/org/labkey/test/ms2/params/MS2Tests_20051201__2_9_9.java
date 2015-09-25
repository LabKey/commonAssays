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
package org.labkey.test.ms2.params;

import org.labkey.test.pipeline.PipelineWebTestBase;

/**
 * MS2Tests_20051201__2_9_9 class
* <p/>
* Created: Aug 15, 2007
*
* @author bmaclean
*/
public class MS2Tests_20051201__2_9_9 extends MS2TestsBase
{
public MS2Tests_20051201__2_9_9(PipelineWebTestBase test)
{
    super(test);
}

public void addTestsScoringOrganisms()
{
    // Scoring tests
    _listParams.add(new MS2ScoringParams(_test, "yeast/Paulovich_101705_ltq", "xt_yeastp",
            new String[] { "YAL003W", "YAL005C", "YAL012W", "YAL023C", "YAL035W", "YAL038W", "YAR002C-A",
                            "YBL002W", "YBL003C", "YBL024W", "YBL027W", "YBL030C", "YBL032W", "YBL039C",
                            "YBL041W", "YBL072C", "YBL075C", "YBL076C", "YBL087C", "YBL092W", "YBL099W",
                            "YBR009C", "YBR010W", "YBR011C", "YBR025C", "YBR031W", "YBR048W", "YBR078W",
                            "YBR079C", "YBR106W", "YBR109C", "YBR115C", "YBR118W", "YBR121C", "YBR127C",
                            "YBR143C", "YBR149W", "YBR154C", "YBR181C", "YBR189W", "YBR191W", "YBR196C",
                            "YBR218C", "YBR221C", "YBR249C", "YBR283C", "YBR286W", "YCL009C", "YCL030C",
                            "YCL037C", "YCL040W", "YCL043C", "YCL050C", "YCR012W", "YCR031C", "YCR053W",
                            "YCR088W", "YDL007W", "YDL014W", "YDL022W", "YDL055C", "YDL061C", "YDL066W",
                            "YDL075W", "YDL081C", "YDL083C", "YDL084W", "YDL100C", "YDL103C", "YDL124W",
                            "YDL126C", "YDL182W", "YDL136W", "YDL140C", "YDL143W", "YDL160C", "YDL185W",
                            "YDL192W", "YDL195W", "YDL229W", "YDR002W", "YDR023W", "YDR032C", "YDR033W",
                            "YDR035W", "YDR037W", "YDR050C", "YDR064W", "YDR071C", "YDR091C", "YDR099W",
                            "YDR127W", "YDR129C", "YDR155C", "YDR158W", "YDR168W", "YDR172W", "YDR174W",
                            "YDR188W", "YDR212W", "YDR226W", "YDR233C", "YDR238C", "YDR304C", "YDR341C",
                            "YDR353W", "YDR381W", "YDR382W", "YDR385W", "YDR390C", "YDR418W", "YDR429C",
                            "YDR432W", "YDR447C", "YDR450W", "YDR454C", "YDR471W", "YDR487C", "YDR500C",
                            "YDR502C", "YEL026W", "YEL031W", "YEL034W", "YEL046C", "YEL047C", "YEL071W",
                            "YER003C", "YER009W", "YER025W", "YER036C", "YER043C", "YER052C", "YER055C",
                            "YER056C-A", "YER057C", "YER069W", "YER070W", "YER073W", "YER074W", "YER090W",
                            "YER091C", "YER110C", "YER120W", "YER131W", "YER133W", "YER136W", "YER165W",
                            "YER177W", "YER178W", "YFL014W", "YFL022C", "YFL037W", "YFL039C", "YFL045C",
                            "YFL048C", "YFR030W", "YFR031C-A", "YFR044C", "YFR053C", "YGL008C", "YGL009C",
                            "YGL011C", "YGL026C", "YGL030W", "YGL031C", "YGL037C", "YGL062W", "YGL076C",
                            "YGL100W", "YGL103W", "YGL105W", "YGL120C", "YGL123W", "YGL135W", "YGL147C",
                            "YGL148W", "YGL173C", "YGL195W", "YGL202W", "YGL206C", "YGL234W", "YGL245W",
                            "YGL253W", "YGR027C", "YGR034W", "YGR037C", "YGR061C", "YGR085C", "YGR086C",
                            "YGR094W", "YGR118W", "YGR124W", "YGR148C", "YGR155W", "YGR157W", "YGR159C",
                            "YGR162W", "YGR180C", "YGR185C", "YGR192C", "YGR204W", "YGR209C", "YGR214W",
                            "YGR234W", "YGR240C", "YGR253C", "YGR254W", "YGR264C", "YGR279C", "YGR282C",
                            "YGR285C", "YHL001W", "YHL011C", "YHL015W", "YHL033C", "YHL034C", "YHR008C",
                            "YHR018C", "YHR019C", "YHR020W", "YHR021C", "YHR025W", "YHR027C", "YHR039C-A",
                            "YHR042W", "YHR064C", "YHR089C", "YHR104W", "YHR128W", "YHR132C", "YHR141C",
                            "YHR163W", "YHR174W", "YHR179W", "YHR183W", "YHR193C", "YHR203C", "YHR208W",
                            "YIL041W", "YIL043C", "YIL051C", "YIL053W", "YIL075C", "YIL078W", "YIL094C",
                            "YIL109C", "YIL116W", "YIL125W", "YIL133C", "YIL142W", "YIL148W", "YJL001W",
                            "YJL008C", "YJL014W", "YJL026W", "YJL034W", "YJL052W", "YJL080C", "YJL111W",
                            "YJL130C", "YJL136C", "YJL138C", "YJL159W", "YJL167W", "YJL172W", "YJL177W",
                            "YJL189W", "YJL190C", "YJR007W", "YJR009C", "YJR010W", "YJR016C", "YJR045C",
                            "YJR064W", "YJR070C", "YJR077C", "YJR094W-A", "YJR104C", "YJR105W", "YJR109C",
                            "YJR121W", "YJR123W", "YJR137C", "YJR139C", "YKL006W", "YKL009W", "YKL016C",
                            "YKL024C", "YKL035W", "YKL054C", "YKL056C", "YKL060C", "YKL080W", "YKL081W",
                            "YKL085W", "YKL096W", "YKL104C", "YKL152C", "YKL180W", "YKL181W", "YKL182W",
                            "YKL210W", "YKL212W", "YKL216W", "YKR001C", "YKR043C", "YKR057W", "YLL018C",
                            "YLL024C", "YLL026W", "YLL045C", "YLL050C", "YLR027C", "YLR028C", "YLR029C",
                            "YLR043C", "YLR044C", "YLR058C", "YLR060W", "YLR061W", "YLR075W", "YLR109W",
                            "YLR134W", "YLR150W", "YLR153C", "YLR167W", "YLR175W", "YLR179C", "YLR180W",
                            "YLR185W", "YLR192C", "YLR196W", "YLR197W", "YLR208W", "YLR216C", "YLR229C",
                            "YLR244C", "YLR249W", "YLR259C", "YLR264W", "YLR287C-A", "YLR293C", "YLR300W",
                            "YLR301W", "YLR304C", "YLR325C", "YLR340W", "YLR342W", "YLR344W", "YLR347C",
                            "YLR354C", "YLR355C", "YLR359W", "YLR388W", "YLR390W-A", "YLR406C", "YLR432W",
                            "YLR438W", "YLR441C", "YLR448W", "YML008C", "YML010W", "YML028W", "YML048W",
                            "YML056C", "YML063W", "YML069W", "YML070W", "YML073C", "YML074C", "YML078W",
                            "YML106W", "YML126C", "YMR012W", "YMR062C", "YMR079W", "YMR083W", "YMR099C",
                            "YMR108W", "YMR116C", "YMR120C", "YMR142C", "YMR146C", "YMR186W", "YMR194W",
                            "YMR205C", "YMR217W", "YMR226C", "YMR229C", "YMR230W", "YMR235C", "YOR312C",
                            "YMR246W", "YMR290C", "YMR303C", "YMR307W", "YMR308C", "YMR309C", "YMR318C",
                            "YNL007C", "YNL010W", "YNL014W", "YNL016W", "YNL055C", "YNL064C", "YNL069C",
                            "YNL071W", "YNL079C", "YNL096C", "YNL104C", "YNL112W", "YNL113W", "YNL134C",
                            "YNL135C", "YNL138W", "YNL175C", "YNL178W", "YNL208W", "YNL209W", "YNL220W",
                            "YNL241C", "YNL244C", "YNL247W", "YNL255C", "YNL287W", "YNL301C", "YNL302C",
                            "YNR016C", "YNR043W", "YOL038W", "YOL039W", "YOL040C", "YOL058W", "YOL059W",
                            "YOL086C", "YOL097C", "YOL109W", "YOL127W", "YOL139C", "YOL143C", "YOR007C",
                            "YOR020C", "YOR027W", "YOR063W", "YOR095C", "YOR096W", "YOR117W", "YOR122C",
                            "YOR153W", "YOR168W", "YOR184W", "YOR187W", "YOR198C", "YOR204W", "YOR230W",
                            "YOR234C", "YOR254C", "YOR261C", "YOR270C", "YOR298C-A", "YOR310C", "YOR317W",
                            "YOR332W", "YOR335C", "YOR341W", "YOR361C", "YOR369C", "YOR374W", "YOR375C",
                            "YPL004C", "YPL028W", "YPL037C", "YPL048W", "YPL061W", "YPL091W", "YPL106C",
                            "YPL126W", "YPL131W", "YPL143W", "YPL154C", "YPL160W", "YPL198W", "YPL218W",
                            "YPL226W", "YPL231W", "YPL237W", "YPL240C", "YPL249C-A", "YPL262W", "YPR010C",
                            "YPR033C", "YPR035W", "YPR036W", "YPR041W", "YPR069C", "YPR074C", "YPR118W",
                            "YPR145W", "YPR149W", "YPR163C", "YPR181C", "YPR183W", "YPR191W", "rev_AECC2_ARATH",
                            "rev_DPO3_STRPN", "rev_HIW_DROME", "rev_VIT1_FUNHE", "rev_VIT2_CHICK", "YDR261C-D",
                            "YAR010C", "YBL005W-A", "YBR012W-A", "YDR098C-A", "YDR098C-B", "YDR210C-C",
                            "YDR210C-D", "YNL054W-A", "YNL284C-A", "YOL103W-A", "YOL103W-B" },
            0.9973, 157, 1.0, 61));
    _listParams.add(new MS2ScoringParams(_test, "yeast/Paulovich_101705_ltq", "xc_yeastp",
            new String[] { "YAL003W", "YAL005C", "YAL012W", "YAL023C", "YAL035W", "YAL038W", "YAR002C-A",
                            "YBL002W", "YBL003C", "YBL024W", "YBL027W", "YBL030C", "YBL039C", "YBL045C",
                            "YBL072C", "YBL075C", "YBL076C", "YBL087C", "YBL092W", "YBL099W", "YBR009C",
                            "YBR010W", "YBR011C", "YBR025C", "YBR031W", "YBR048W", "YBR078W", "YBR079C",
                            "YBR080C", "YBR106W", "YBR109C", "YBR118W", "YBR121C", "YBR127C", "YBR143C",
                            "YBR181C", "YBR189W", "YBR191W", "YBR196C", "YBR218C", "YBR221C", "YBR249C",
                            "YBR283C", "YBR286W", "YCL009C", "YCL030C", "YCL037C", "YCL040W", "YCL043C",
                            "YCL050C", "YCR009C", "YCR012W", "YCR031C", "YCR053W", "YCR088W", "YDL014W",
                            "YDL022W", "YDL055C", "YDL061C", "YDL066W", "YDL075W", "YDL081C", "YDL083C",
                            "YDL084W", "YDL095W", "YDL100C", "YDL124W", "YDL126C", "YDL131W", "YDL136W",
                            "YDL137W", "YDL143W", "YDL160C", "YDL185W", "YDL192W", "YDL195W", "YDL229W",
                            "YDR002W", "YDR012W", "YDR023W", "YDR032C", "YDR033W", "YDR035W", "YDR037W",
                            "YDR050C", "YDR064W", "YDR071C", "YDR091C", "YDR099W", "YDR101C", "YDR127W",
                            "YDR129C", "YDR155C", "YDR158W", "YDR172W", "YDR174W", "YDR188W", "YDR212W",
                            "YDR226W", "YDR233C", "YDR238C", "YDR341C", "YDR353W", "YDR381W", "YDR382W",
                            "YDR385W", "YDR390C", "YDR418W", "YDR429C", "YDR432W", "YDR447C", "YDR450W",
                            "YDR471W", "YDR487C", "YDR500C", "YDR502C", "YDR510W", "YEL026W", "YEL031W",
                            "YEL034W", "YEL040W", "YEL046C", "YEL047C", "YEL071W", "YER003C", "YER009W",
                            "YER025W", "YER031C", "YER036C", "YER043C", "YER052C", "YER056C-A", "YER057C",
                            "YER070W", "YER073W", "YER074W", "YER086W", "YER090W", "YER091C", "YER110C",
                            "YER120W", "YER131W", "YER133W", "YER136W", "YER165W", "YER177W", "YER178W",
                            "YFL014W", "YFL022C", "YFL037W", "YFL039C", "YFL045C", "YFL048C", "YFR030W",
                            "YFR031C-A", "YFR044C", "YFR053C", "YGL008C", "YGL009C", "YGL011C", "YGL026C",
                            "YGL030W", "YGL031C", "YGL076C", "YGL103W", "YGL105W", "YGL106W", "YGL120C",
                            "YGL123W", "YGL135W", "YGL147C", "YGL148W", "YGL173C", "YGL195W", "YGL202W",
                            "YGL206C", "YGL234W", "YGL245W", "YGL253W", "YGR027C", "YGR034W", "YGR037C",
                            "YGR061C", "YGR085C", "YGR086C", "YGR094W", "YGR118W", "YGR124W", "YGR148C",
                            "YGR155W", "YGR157W", "YGR159C", "YGR162W", "YGR180C", "YGR185C", "YGR192C",
                            "YGR204W", "YGR209C", "YGR211W", "YGR214W", "YGR234W", "YGR240C", "YGR245C",
                            "YGR253C", "YGR254W", "YGR264C", "YGR279C", "YGR282C", "YGR285C", "YHL001W",
                            "YHL015W", "YHL033C", "YHL034C", "YHR018C", "YHR019C", "YHR020W", "YHR025W",
                            "YHR027C", "YHR039C-A", "YHR042W", "YHR064C", "YHR089C", "YHR104W", "YHR128W",
                            "YHR170W", "YHR174W", "YHR179W", "YHR183W", "YHR190W", "YHR193C", "YHR203C",
                            "YHR208W", "YIL022W", "YIL041W", "YIL051C", "YIL053W", "YIL075C", "YIL078W",
                            "YIL094C", "YIL109C", "YIL125W", "YIL133C", "YIL142W", "YIL148W", "YJL001W",
                            "YJL008C", "YJL014W", "YJL026W", "YJL034W", "YJL052W", "YJL080C", "YJL130C",
                            "YJL136C", "YJL138C", "YJL167W", "YJL172W", "YJL177W", "YJL189W", "YJL190C",
                            "YJR007W", "YJR009C", "YJR010W", "YJR016C", "YJR045C", "YJR064W", "YJR070C",
                            "YJR077C", "YJR094W-A", "YJR104C", "YJR105W", "YJR109C", "YJR121W", "YJR123W",
                            "YJR137C", "YJR139C", "YKL006W", "YKL009W", "YKL024C", "YKL029C", "YKL035W",
                            "YKL054C", "YKL056C", "YKL060C", "YKL067W", "YKL080W", "YKL081W", "YKL085W",
                            "YKL096W", "YKL104C", "YKL127W", "YKL152C", "YKL180W", "YKL181W", "YKL182W",
                            "YKL210W", "YKL212W", "YKL216W", "YKR001C", "YKR043C", "YKR057W", "YLL018C",
                            "YLL024C", "YLL026W", "YLL045C", "YLL050C", "YLR027C", "YLR028C", "YLR029C",
                            "YLR043C", "YLR044C", "YLR058C", "YLR060W", "YLR061W", "YLR075W", "YLR109W",
                            "YLR134W", "YLR150W", "YLR153C", "YLR167W", "YLR175W", "YLR179C", "YLR180W",
                            "YLR185W", "YLR192C", "YLR197W", "YLR208W", "YLR216C", "YLR229C", "YLR249W",
                            "YLR259C", "YLR264W", "YLR287C-A", "YLR293C", "YLR300W", "YLR301W", "YLR304C",
                            "YLR325C", "YLR340W", "YLR342W", "YLR344W", "YLR347C", "YLR354C", "YLR355C",
                            "YLR359W", "YLR388W", "YLR390W-A", "YLR406C", "YLR432W", "YLR438W", "YLR441C",
                            "YLR447C", "YLR448W", "YML008C", "YML028W", "YML056C", "YML063W", "YML070W",
                            "YML072C", "YML073C", "YML085C", "YML106W", "YML126C", "YMR012W", "YMR079W",
                            "YMR083W", "YMR099C", "YMR108W", "YMR116C", "YMR142C", "YMR146C", "YMR186W",
                            "YMR194W", "YMR205C", "YMR217W", "YMR226C", "YMR229C", "YMR230W", "YMR235C",
                            "YOR312C", "YMR307W", "YMR309C", "YMR318C", "YNL007C", "YNL010W", "YNL014W",
                            "YNL016W", "YNL055C", "YNL064C", "YNL069C", "YNL071W", "YNL079C", "YNL096C",
                            "YNL104C", "YNL134C", "YNL135C", "YNL138W", "YNL178W", "YNL189W", "YNL208W",
                            "YNL209W", "YNL220W", "YNL241C", "YNL247W", "YNL255C", "YNL287W", "YNL301C",
                            "YNL302C", "YNR016C", "YNR043W", "YOL039W", "YOL040C", "YOL058W", "YOL059W",
                            "YOL086C", "YOL097C", "YOL109W", "YOL127W", "YOL139C", "YOL143C", "YOR007C",
                            "YOR020C", "YOR027W", "YOR063W", "YOR086C", "YOR096W", "YOR122C", "YOR142W",
                            "YOR153W", "YOR168W", "YOR184W", "YOR198C", "YOR204W", "YOR209C", "YOR230W",
                            "YOR254C", "YOR270C", "YOR298C-A", "YOR310C", "YOR317W", "YOR323C", "YOR332W",
                            "YOR335C", "YOR341W", "YOR361C", "YOR369C", "YOR375C", "YPL004C", "YPL020C",
                            "YPL028W", "YPL037C", "YPL048W", "YPL061W", "YPL091W", "YPL106C", "YPL126W",
                            "YPL131W", "YPL143W", "YPL154C", "YPL160W", "YPL218W", "YPL226W", "YPL231W",
                            "YPL240C", "YPL249C-A", "YPR010C", "YPR033C", "YPR035W", "YPR036W", "YPR041W",
                            "YPR069C", "YPR074C", "YPR118W", "YPR145W", "YPR149W", "YPR163C", "YPR183W",
                            "YPR191W", "rev_CAFF_RIFPA", "rev_DPOE1_HUMAN", "YAR010C", "YBL005W-A",
                            "YBR012W-A", "YDR098C-B", "YDR210C-C", "YNL054W-A", "YNL284C-A", "YOL103W-A",
                            "YOL103W-B" },
            0.9973, 337, 0.98, 148));
    _listParams.add(new MS2ScoringParams(_test, "yeast/Paulovich_101705_ltq", "xk_yeastp",
            new String[] { "YAL003W", "YAL005C", "YAL012W", "YAL023C", "YAL035W", "YAL038W", "YAR002C-A",
                            "YBL002W", "YBL003C", "YBL024W", "YBL027W", "YBL030C", "YBL039C", "YBL045C",
                            "YBL072C", "YBL075C", "YBL076C", "YBL087C", "YBL092W", "YBL099W", "YBR009C",
                            "YBR010W", "YBR011C", "YBR025C", "YBR031W", "YBR048W", "YBR078W", "YBR079C",
                            "YBR080C", "YBR106W", "YBR109C", "YBR118W", "YBR121C", "YBR127C", "YBR143C",
                            "YBR181C", "YBR189W", "YBR191W", "YBR196C", "YBR218C", "YBR221C", "YBR249C",
                            "YBR283C", "YBR286W", "YCL009C", "YCL030C", "YCL037C", "YCL040W", "YCL043C",
                            "YCL050C", "YCR009C", "YCR012W", "YCR031C", "YCR053W", "YCR088W", "YDL014W",
                            "YDL022W", "YDL055C", "YDL061C", "YDL066W", "YDL075W", "YDL081C", "YDL083C",
                            "YDL084W", "YDL095W", "YDL100C", "YDL124W", "YDL126C", "YDL131W", "YDL136W",
                            "YDL137W", "YDL143W", "YDL160C", "YDL185W", "YDL192W", "YDL195W", "YDL229W",
                            "YDR002W", "YDR012W", "YDR023W", "YDR032C", "YDR033W", "YDR035W", "YDR037W",
                            "YDR050C", "YDR064W", "YDR071C", "YDR091C", "YDR099W", "YDR101C", "YDR127W",
                            "YDR129C", "YDR155C", "YDR158W", "YDR172W", "YDR174W", "YDR188W", /*"YDR212W", not k-score*/
                            "YDR226W", "YDR233C", "YDR238C", "YDR341C", "YDR353W", "YDR381W", "YDR382W",
                            "YDR385W", /*"YDR390C", not k-score*/ "YDR418W", "YDR429C", "YDR432W", "YDR447C", "YDR450W",
                            "YDR471W", "YDR487C", "YDR500C", "YDR502C", "YDR510W", "YEL026W", "YEL031W",
                            "YEL034W", "YEL040W", "YEL046C", "YEL047C", "YEL071W", "YER003C", "YER009W",
                            "YER025W", "YER031C", "YER036C", "YER043C", "YER052C", "YER056C-A", "YER057C",
                            /*"YER070W", not k-score*/ "YER073W", "YER074W", "YER086W", "YER090W", "YER091C", "YER110C",
                            "YER120W", "YER131W", "YER133W", "YER136W", "YER165W", "YER177W", "YER178W",
                            "YFL014W", "YFL022C", "YFL037W", "YFL039C", "YFL045C", "YFL048C", "YFR030W",
                            "YFR031C-A", "YFR044C", "YFR053C", "YGL008C", "YGL009C", "YGL011C", "YGL026C",
                            "YGL030W", "YGL031C", "YGL076C", "YGL103W", "YGL105W", "YGL106W", "YGL120C",
                            "YGL123W", "YGL135W", "YGL147C", "YGL148W", "YGL173C", "YGL195W", "YGL202W",
                            "YGL206C", "YGL234W", "YGL245W", "YGL253W", "YGR027C", "YGR034W", "YGR037C",
                            "YGR061C", "YGR085C", "YGR086C", "YGR094W", "YGR118W", "YGR124W", "YGR148C",
                            "YGR155W", "YGR157W", "YGR159C", "YGR162W", "YGR180C", "YGR185C", "YGR192C",
                            "YGR204W", "YGR209C", "YGR211W", "YGR214W", "YGR234W", "YGR240C", /*"YGR245C", not k-score*/
                            "YGR253C", "YGR254W", "YGR264C", "YGR279C", "YGR282C", "YGR285C", "YHL001W",
                            "YHL015W", "YHL033C", "YHL034C", "YHR018C", "YHR019C", "YHR020W", "YHR025W",
                            "YHR027C", "YHR039C-A", "YHR042W", "YHR064C", "YHR089C", "YHR104W", "YHR128W",
                            "YHR170W", "YHR174W", "YHR179W", "YHR183W", "YHR190W", "YHR193C", "YHR203C",
                            "YHR208W", "YIL022W", "YIL041W", "YIL051C", "YIL053W", "YIL075C", "YIL078W",
                            "YIL094C", "YIL109C", "YIL125W", "YIL133C", "YIL142W", "YIL148W", "YJL001W",
                            "YJL008C", "YJL014W", "YJL026W", "YJL034W", "YJL052W", "YJL080C", "YJL130C",
                            "YJL136C", "YJL138C", "YJL167W", "YJL172W", "YJL177W", "YJL189W", "YJL190C",
                            "YJR007W", "YJR009C", "YJR010W", "YJR016C", "YJR045C", "YJR064W", "YJR070C",
                            "YJR077C", "YJR094W-A", "YJR104C", "YJR105W", "YJR109C", "YJR121W", "YJR123W",
                            "YJR137C", "YJR139C", "YKL006W", "YKL009W", "YKL024C", "YKL029C", "YKL035W",
                            "YKL054C", "YKL056C", "YKL060C", "YKL067W", "YKL080W", "YKL081W", "YKL085W",
                            "YKL096W", "YKL104C", "YKL127W", "YKL152C", "YKL180W", "YKL181W", "YKL182W",
                            "YKL210W", "YKL212W", "YKL216W", "YKR001C", "YKR043C", "YKR057W", "YLL018C",
                            "YLL024C", "YLL026W", "YLL045C", "YLL050C", "YLR027C", "YLR028C", "YLR029C",
                            "YLR043C", "YLR044C", "YLR058C", "YLR060W", "YLR061W", "YLR075W", "YLR109W",
                            "YLR134W", "YLR150W", "YLR153C", "YLR167W", "YLR175W", "YLR179C", "YLR180W",
                            "YLR185W", "YLR192C", "YLR197W", "YLR208W", "YLR216C", "YLR229C", "YLR249W",
                            "YLR259C", "YLR264W", "YLR287C-A", "YLR293C", "YLR300W", "YLR301W", "YLR304C",
                            "YLR325C", "YLR340W", "YLR342W", "YLR344W", "YLR347C", "YLR354C", "YLR355C",
                            "YLR359W", "YLR388W", "YLR390W-A", "YLR406C", "YLR432W", "YLR438W", "YLR441C",
                            "YLR447C", "YLR448W", "YML008C", "YML028W", "YML056C", "YML063W", "YML070W",
                            "YML072C", "YML073C", "YML085C", "YML106W", "YML126C", "YMR012W", "YMR079W",
                            "YMR083W", "YMR099C", "YMR108W", "YMR116C", "YMR142C", "YMR146C", "YMR186W",
                            "YMR194W", "YMR205C", "YMR217W", "YMR226C", "YMR229C", "YMR230W", "YMR235C",
                            "YOR312C", "YMR307W", "YMR309C", "YMR318C", "YNL007C", "YNL010W", "YNL014W",
                            "YNL016W", "YNL055C", "YNL064C", "YNL069C", "YNL071W", "YNL079C", "YNL096C",
                            "YNL104C", "YNL134C", "YNL135C", "YNL138W", "YNL178W", "YNL189W", "YNL208W",
                            "YNL209W", "YNL220W", "YNL241C", "YNL247W", "YNL255C", "YNL287W", "YNL301C",
                            "YNL302C", "YNR016C", "YNR043W", "YOL039W", "YOL040C", "YOL058W", "YOL059W",
                            "YOL086C", "YOL097C", "YOL109W", "YOL127W", "YOL139C", "YOL143C", "YOR007C",
                            "YOR020C", "YOR027W", "YOR063W", /*"YOR086C", not k-score*/ "YOR096W", "YOR122C", "YOR142W",
                            "YOR153W", "YOR168W", "YOR184W", "YOR198C", "YOR204W", "YOR209C", "YOR230W",
                            "YOR254C", "YOR270C", "YOR298C-A", "YOR310C", "YOR317W", "YOR323C", "YOR332W",
                            "YOR335C", "YOR341W", "YOR361C", "YOR369C", "YOR375C", "YPL004C", "YPL020C",
                            "YPL028W", "YPL037C", "YPL048W", "YPL061W", "YPL091W", "YPL106C", "YPL126W",
                            "YPL131W", "YPL143W", "YPL154C", "YPL160W", "YPL218W", "YPL226W", "YPL231W",
                            "YPL240C", "YPL249C-A", "YPR010C", "YPR033C", "YPR035W", "YPR036W", "YPR041W",
                            "YPR069C", "YPR074C", "YPR118W", "YPR145W", "YPR149W", "YPR163C", "YPR183W",
                            "YPR191W", "rev_CAFF_RIFPA", "rev_DPOE1_HUMAN", "YAR010C", "YBL005W-A",
                            "YBR012W-A", "YDR098C-B", "YDR210C-C", "YNL054W-A", "YNL284C-A", "YOL103W-A",
                            "YOL103W-B",
                    // k-score only
                    "YBR082C", "YIL066C", "YGL189C", "YHR008C", "YHR021C", "YJL159W"
            },
            0.9973, 360, 0.98, 150));

    _listParams.add(new MS2ScoringParams(_test, "yeast/comp12vs12standSCX", "xt_yeast",
            new String[] { "YAL003W", "YAL005C", "YAL012W", "YAL016W", "YAL023C", "YAL035W", "YAL038W",
                            "YAL044C", "YAL060W", "YAR015W", "YBL002W", "YBL003C", "YBL015W", "YBL024W",
                            "YBL027W", "YBL030C", "YBL045C", "YBL047C", "YBL050W", "YBL064C", "YBL072C",
                            "YBL076C", "YBL092W", "YBL099W", "YBR009C", "YBR011C", "YBR025C", "YBR031W",
                            "YBR035C", "YBR041W", "YBR048W", "YBR054W", "YBR072W", "YBR078W", "YBR079C",
                            "YBR082C", "YBR086C", "YBR088C", "YBR106W", "YBR109C", "YBR111C", "YBR118W",
                            "YBR121C", "YBR126C", "YBR127C", "YBR143C", "YBR145W", "YBR149W", "YBR169C",
                            "YBR181C", "YBR189W", "YBR191W", "YBR196C", "YBR208C", "YBR214W", "YGL062W",
                            "YBR221C", "YBR222C", "YBR248C", "YBR249C", "YBR256C", "YBR283C", "YBR286W",
                            "YCL009C", "YCL030C", "YCL040W", "YCL043C", "YCL050C", "YCL057W", "YCR004C",
                            "YCR009C", "YCR012W", "YCR021C", "YCR031C", "YCR053W", "YCR088W", "YDL007W",
                            "YDL014W", "YDL022W", "YDL052C", "YDL055C", "YDL066W", "YDL072C", "YDL075W",
                            "YDL078C", "YDL081C", "YDL082W", "YDL083C", "YDL084W", "YDL097C", "YDL100C",
                            "YDL124W", "YDL126C", "YDL130W", "YDL131W", "YDL136W", "YDL143W", "YDL145C",
                            "YDL171C", "YDL185W", "YDL192W", "YDL215C", "YDL226C", "YDL229W", "YDR002W",
                            "YDR023W", "YDR032C", "YDR035W", "YDR037W", "YDR050C", "YDR064W", "YDR071C",
                            "YDR091C", "YDR099W", "YDR127W", "YDR129C", "YDR135C", "YDR148C", "YDR155C",
                            "YDR158W", "YDR172W", "YDR174W", "YDR214W", "YDR226W", "YDR232W", "YDR233C",
                            "YDR258C", "YDR341C", "YDR342C", "YDR345C", "YDR353W", "YDR368W", "YDR382W",
                            "YDR385W", "YDR388W", "YDR399W", "YDR418W", "YDR447C", "YDR450W", "YDR454C",
                            "YDR471W", "YDR481C", "YDR483W", "YDR487C", "YDR513W", "YDR516C", "YDR533C",
                            "YEL026W", "YEL034W", "YEL037C", "YJR048W", "YEL047C", "YEL060C", "YEL071W",
                            "YER003C", "YER009W", "YER025W", "YER036C", "YER043C", "YER052C", "YER055C",
                            "YER057C", "YER062C", "YER074W", "YER081W", "YER090W", "YER091C", "YER094C",
                            "YER103W", "YER110C", "YER120W", "YER133W", "YER165W", "YER177W", "YER178W",
                            "YFL004W", "YFL005W", "YFL014W", "YFL022C", "YFL037W", "YFL039C", "YFL045C",
                            "YFR030W", "YFR031C-A", "YFR044C", "YFR053C", "YGL008C", "YGL009C", "YGL026C",
                            "YGL030W", "YGL031C", "YGL037C", "YGL054C", "YGL055W", "YGL068W", "YGL076C",
                            "YGL103W", "YGL105W", "YGL106W", "YGL123W", "YGL135W", "YGL137W", "YGL147C",
                            "YGL148W", "YGL187C", "YGL189C", "YGL195W", "YGL202W", "YGL206C", "YGL234W",
                            "YGL242C", "YGL245W", "YGL253W", "YGL256W", "YGR001C", "YGR019W", "YGR020C",
                            "YGR027C", "YGR037C", "YGR054W", "YGR061C", "YGR085C", "YGR086C", "YGR094W",
                            "YGR116W", "YGR118W", "YGR124W", "YGR135W", "YGR155W", "YGR159C", "YGR162W",
                            "YGR167W", "YGR178C", "YGR180C", "YGR185C", "YGR192C", "YGR193C", "YGR204W",
                            "YGR209C", "YGR214W", "YGR234W", "YGR240C", "YGR244C", "YGR254W", "YGR264C",
                            "YGR282C", "YGR285C", "YHL001W", "YHL015W", "YHL021C", "YHL033C", "YHL034C",
                            "YHR007C", "YHR008C", "YHR018C", "YHR019C", "YHR020W", "YHR021C", "YHR025W",
                            "YHR027C", "YHR030C", "YHR037W", "YHR039C-A", "YHR042W", "YHR047C", "YHR064C",
                            "YHR068W", "YHR087W", "YHR104W", "YHR128W", "YHR141C", "YHR146W", "YHR174W",
                            "YHR179W", "YHR183W", "YHR193C", "YHR203C", "YHR208W", "YIL033C", "YIL034C",
                            "YIL041W", "YIL043C", "YIL051C", "YIL053W", "YIL074C", "YIL076W", "YIL078W",
                            "YIL094C", "YIL116W", "YIL125W", "YIL133C", "YIL136W", "YIL142W", "YIR034C",
                            "YIR037W", "YJL008C", "YJL012C", "YJL026W", "YJL034W", "YJL052W", "YJL055W",
                            "YJL080C", "YJL111W", "YJL117W", "YJL123C", "YJL130C", "YJL136C", "YJL138C",
                            "YJL167W", "YJL171C", "YJL172W", "YJL173C", "YJL189W", "YJL190C", "YJL200C",
                            "YJL217W", "YJR007W", "YJR009C", "YJR010W", "YJR016C", "YJR025C", "YJR045C",
                            "YJR059W", "YJR065C", "YJR070C", "YJR073C", "YJR077C", "YJR104C", "YJR105W",
                            "YJR109C", "YJR121W", "YJR123W", "YJR137C", "YJR139C", "YKL007W", "YKL016C",
                            "YKL029C", "YKL035W", "YKL056C", "YKL060C", "YKL065C", "YKL067W", "YKL080W",
                            "YKL081W", "YKL085W", "YKL103C", "YKL104C", "YKL127W", "YKL142W", "YKL148C",
                            "YKL150W", "YKL152C", "YKL157W", "YKL180W", "YKL182W", "YKL193C", "YKL195W",
                            "YKL210W", "YKL212W", "YKR001C", "YKR014C", "YKR046C", "YKR048C", "YKR057W",
                            "YKR076W", "YKR080W", "YLL001W", "YLL018C", "YLL024C", "YLL026W", "YLL045C",
                            "YLL050C", "YLR017W", "YLR027C", "YLR028C", "YLR029C", "YLR043C", "YLR044C",
                            "YLR058C", "YLR060W", "YLR061W", "YLR075W", "YLR109W", "YLR113W", "YLR150W",
                            "YLR153C", "YLR167W", "YLR175W", "YLR178C", "YLR180W", "YLR192C", "YLR197W",
                            "YLR231C", "YLR249W", "YLR257W", "YLR258W", "YLR259C", "YLR264W", "YLR270W",
                            "YLR289W", "YLR293C", "YLR301W", "YLR303W", "YLR304C", "YLR325C", "YLR335W",
                            "YLR340W", "YLR342W", "YLR344W", "YLR354C", "YLR355C", "YLR359W", "YLR389C",
                            "YLR429W", "YLR441C", "YLR447C", "YLR448W", "YML008C", "YML028W", "YML048W",
                            "YML057W", "YML063W", "YML070W", "YML073C", "YML085C", "YML086C", "YML106W",
                            "YML124C", "YML126C", "YML128C", "YML130C", "YML131W", "YMR012W", "YMR027W",
                            "YMR038C", "YMR072W", "YMR079W", "YMR083W", "YMR090W", "YMR092C", "YMR099C",
                            "YMR105C", "YMR108W", "YMR116C", "YMR120C", "YMR145C", "YMR146C", "YMR173W",
                            "YMR183C", "YMR186W", "YMR189W", "YMR202W", "YMR205C", "YMR217W", "YMR226C",
                            "YMR230W", "YMR237W", "YMR241W", "YOR312C", "YMR250W", "YMR261C", "YMR300C",
                            "YMR307W", "YMR309C", "YMR314W", "YMR315W", "YMR318C", "YNL007C", "YNL010W",
                            "YNL015W", "YNL037C", "YNL044W", "YNL045W", "YNL055C","YNL064C", "YNL069C",
                            "YNL071W", "YNL079C", "YNL085W", "YNL096C", "YNL098C", "YOR108W", "YNL113W",
                            "YNL121C", "YNL131W", "YNL134C", "YNL135C", "YNL138W", "YNL160W", "YNL178W",
                            "YNL189W", "YNL209W", "YNL220W", "YNL239W", "YNL241C", "YNL244C", "YNL247W",
                            "YNL281W", "YNL287W", "YNL301C", "YNL302C", "YNR001C", "YNR016C", "YNR021W",
                            "YNR034W-A", "YNR043W", "YNR050C", "YOL039W", "YOL040C", "YOL058W", "YOL059W",
                            "YOL064C", "YOL086C", "YOL109W", "YOL123W", "YOL127W", "YOL139C", "YOL151W",
                            "YOR007C", "YOR020C", "YOR027W", "YOR046C", "YOR063W", "YOR096W", "YOR117W",
                            "YOR120W", "YOR136W", "YOR142W", "YOR184W", "YOR198C", "YOR204W", "YOR209C",
                            "YOR230W", "YOR234C", "YOR239W", "YOR261C", "YOR270C", "YOR285W", "YOR310C",
                            "YOR317W", "YOR323C", "YOR326W", "YOR332W", "YOR335C", "YOR341W", "YOR361C",
                            "YOR362C", "YOR369C", "YOR374W", "YOR375C", "YPL004C", "YPL028W", "YPL037C",
                            "YPL048W", "YPL061W", "YPL078C", "YPL088W", "YPL091W", "YPL106C", "YPL117C",
                            "YPL131W", "YPL143W", "YPL145C", "YPL154C", "YPL160W", "YPL218W", "YPL225W",
                            "YPL226W", "YPL231W", "YPL237W", "YPL240C", "YPL249C-A", "YPL260W", "YPL262W",
                            "YPR033C", "YPR035W", "YPR036W", "YPR041W", "YPR069C", "YPR074C", "YPR103W",
                            "YPR133C", "YPR145W", "YPR148C", "YPR159W", "YPR160W", "YPR163C", "YPR181C",
                            "YPR183W", "YPR191W", "rev_DNAA_LACPL", "rev_S12A2_MOUSE", "rev_Y512_BUCAP",
                            "YBL005W-B", "YML045W", "YCL019W", "YCL020W", "YAR009C", "YAR010C", "YBL005W-A",
                            "YBL100W-A", "YBL100W-B", "YBR012W-A", "YBR012W-B", "YDR034C-C", "YDR034C-D",
                            "YDR098C-B", "YDR210C-C", "YDR210C-D", "YDR210W-A", "YDR210W-B", "YDR261C-D",
                            "YDR261W-A", "YDR261W-B", "YDR316W-B", "YDR365W-B", "YER138C", "YER160C", "YGR027W-B",
                            "YGR161C-D", "YHR214C-B", "YJR027W", "YJR029W", "YLR035C-A", "YLR157C-B", "YLR227W-B",
                            "YNL054W-A", "YNL284C-A", "YNL284C-B", "YOL103W-B", "YOR142W-B", "YPR158C-D",
                            "YPR158W-B", "YHR216W", "YLR432W", "YAR073W", "YML056C" },
            0.9983, 88, 1.0, 52));
    _listParams.add(new MS2ScoringParams(_test, "yeast/comp12vs12standSCX", "xc_yeast",
            new String[] { "YAL003W", "YAL005C", "YAL012W", "YAL016W", "YAL023C", "YAL035W", "YAL038W",
                            "YAL044C", "YAL049C", "YAL060W", "YAR015W", "YBL002W", "YBL003C", "YBL015W",
                            "YBL024W", "YBL027W", "YBL030C", "YBL045C", "YBL047C", "YBL050W", "YBL064C",
                            "YBL072C", "YBL076C", "YBL092W", "YBL099W", "YBR009C", "YBR011C", "YBR025C",
                            "YBR031W", "YBR035C", "YBR041W", "YBR048W", "YBR054W", "YBR072W", "YBR078W",
                            "YBR079C", "YBR086C", "YBR088C", "YBR109C", "YBR111C", "YBR118W", "YBR121C",
                            "YBR126C", "YBR127C", "YBR143C", "YBR145W", "YBR149W", "YBR169C", "YBR177C",
                            "YBR181C", "YBR189W", "YBR191W", "YBR196C", "YBR208C", "YBR214W", "YGL062W",
                            "YBR221C", "YBR222C", "YBR248C", "YBR249C", "YBR256C", "YBR283C", "YBR286W",
                            "YCL009C", "YCL030C", "YCL040W", "YCL043C", "YCL050C", "YCL057W", "YCR002C",
                            "YCR004C", "YCR009C", "YCR012W", "YCR021C", "YCR031C", "YCR053W", "YCR088W",
                            "YDL004W", "YDL007W", "YDL014W", "YDL022W", "YDL055C", "YDL066W", "YDL072C",
                            "YDL075W", "YDL078C", "YDL081C", "YDL083C", "YDL084W", "YDL097C", "YDL100C",
                            "YDL124W", "YDL126C", "YDL130W", "YDL131W", "YDL136W", "YDL143W", "YDL145C",
                            "YDL171C", "YDL182W", "YDL185W", "YDL192W", "YDL215C", "YDL226C", "YDL229W",
                            "YDR002W", "YDR023W", "YDR032C", "YDR033W", "YDR035W", "YDR037W", "YDR050C",
                            "YDR064W", "YDR091C", "YDR099W", "YDR127W", "YDR129C", "YDR135C", "YDR148C",
                            "YDR155C", "YDR158W", "YDR168W", "YDR174W", "YDR214W", "YDR226W", "YDR232W",
                            "YDR233C", "YDR258C", "YDR341C", "YDR342C", "YDR353W", "YDR368W", "YDR382W",
                            "YDR385W", "YDR388W", "YDR418W", "YDR432W", "YDR447C", "YDR450W", "YDR454C",
                            "YDR471W", "YDR481C", "YDR483W", "YDR487C", "YDR510W", "YDR513W", "YDR516C",
                            "YDR533C", "YEL026W", "YEL034W", "YEL037C", "YJR048W", "YEL046C", "YEL047C",
                            "YEL060C", "YEL071W", "YER003C", "YER009W", "YER025W", "YER036C", "YER043C",
                            "YER049W", "YER052C", "YER057C", "YER062C", "YER074W", "YER081W", "YER090W",
                            "YER091C", "YER094C", "YER103W", "YER110C", "YER120W", "YER133W", "YER165W",
                            "YER177W", "YER178W", "YFL005W", "YFL014W", "YFL016C", "YFL022C", "YFL037W",
                            "YFL039C", "YFL045C", "YFR030W", "YFR031C-A", "YFR044C", "YFR052W", "YFR053C",
                            "YGL008C", "YGL009C", "YGL026C", "YGL030W", "YGL031C", "YGL037C", "YGL054C",
                            "YGL055W", "YGL068W", "YGL076C", "YGL103W", "YGL105W", "YGL106W", "YGL123W",
                            "YGL135W", "YGL137W", "YGL147C", "YGL148W", "YGL187C", "YGL189C", "YGL195W",
                            "YGL202W", "YGL206C", "YGL234W", "YGL242C", "YGL245W", "YGL253W", "YGL256W",
                            "YGR001C", "YGR008C", "YGR019W", "YGR020C", "YGR027C", "YLR344W", "YGR037C",
                            "YGR054W", "YGR061C", "YGR085C", "YGR086C", "YGR094W", "YGR116W", "YGR118W",
                            "YGR124W", "YGR135W", "YGR155W", "YGR159C", "YGR162W", "YGR167W", "YGR180C",
                            "YGR185C", "YGR192C", "YGR193C", "YGR204W", "YGR209C", "YGR214W", "YGR234W",
                            "YGR240C", "YGR244C", "YGR254W", "YGR264C", "YGR282C", "YGR285C", "YHL001W",
                            "YHL015W", "YHL021C", "YHL033C", "YHL034C", "YHR007C", "YHR008C", "YHR018C",
                            "YHR019C", "YHR020W", "YHR021C", "YHR025W", "YHR027C", "YHR030C", "YHR037W",
                            "YHR039C-A", "YHR042W", "YHR047C", "YHR049W", "YHR064C", "YHR068W", "YHR087W",
                            "YHR104W", "YHR128W", "YHR141C", "YHR146W", "YHR158C", "YHR174W", "YHR179W",
                            "YHR183W", "YHR193C", "YHR201C", "YHR203C", "YHR208W", "YIL033C", "YIL034C",
                            "YIL041W", "YIL051C", "YIL053W", "YIL074C", "YIL076W", "YIL078W", "YIL109C",
                            "YIL116W", "YIL125W", "YIL133C", "YIL136W", "YIL142W", "YIR034C", "YIR037W",
                            "YJL008C", "YJL012C", "YJL020C", "YJL026W", "YJL034W", "YJL052W", "YJL080C",
                            "YJL111W", "YJL117W", "YJL123C", "YJL130C", "YJL136C", "YJL138C", "YJL167W",
                            "YJL171C", "YJL172W", "YJL173C", "YJL177W", "YJL190C", "YJL200C", "YJL217W",
                            "YJR007W", "YJR009C", "YJR010W", "YJR016C", "YJR025C", "YJR045C", "YJR059W",
                            "YJR070C", "YJR073C", "YJR077C", "YJR104C", "YJR105W", "YJR121W", "YJR123W",
                            "YJR137C", "YJR139C", "YKL007W", "YKL016C", "YKL029C", "YKL035W", "YKL054C",
                            "YKL056C", "YKL060C", "YKL065C", "YKL067W", "YKL080W", "YKL081W", "YKL085W",
                            "YKL103C", "YKL104C", "YKL127W", "YKL142W", "YKL150W", "YKL152C", "YKL157W",
                            "YKL172W", "YKL180W", "YKL182W", "YKL193C", "YKL195W", "YKL210W", "YKL211C",
                            "YKL212W", "YKR001C", "YKR014C", "YKR046C", "YKR048C", "YKR057W", "YKR076W",
                            "YKR080W", "YLL001W", "YLL018C", "YLL024C", "YLL026W", "YLL045C", "YLL050C",
                            "YLR017W", "YLR027C", "YLR028C", "YLR029C", "YLR043C", "YLR044C", "YLR048W",
                            "YLR058C", "YLR060W", "YLR061W", "YLR075W", "YLR109W", "YLR113W", "YLR150W",
                            "YLR153C", "YLR167W", "YLR175W", "YLR178C", "YLR180W", "YLR192C", "YLR197W",
                            "YLR216C", "YLR249W", "YLR257W", "YLR258W", "YLR259C", "YLR264W", "YLR270W",
                            "YLR289W", "YLR293C", "YLR301W", "YLR303W", "YLR304C", "YLR325C", "YLR335W",
                            "YLR340W", "YLR342W", "YLR354C", "YLR355C", "YLR359W", "YLR429W", "YLR441C",
                            "YLR447C", "YLR448W", "YML008C", "YML022W", "YML028W", "YML048W", "YML057W",
                            "YML063W", "YML070W", "YML072C", "YML073C", "YML085C", "YML086C", "YML106W",
                            "YML124C", "YML126C", "YML128C", "YML130C", "YML131W", "YMR012W", "YMR027W",
                            "YMR038C", "YMR072W", "YMR074C", "YMR079W", "YMR083W", "YMR090W", "YMR092C",
                            "YMR099C", "YMR105C", "YMR108W", "YMR116C", "YMR120C", "YMR142C", "YMR145C",
                            "YMR146C", "YMR173W", "YMR183C", "YMR186W", "YMR189W", "YMR196W", "YMR202W",
                            "YMR203W", "YMR205C", "YMR208W", "YMR217W", "YMR226C", "YMR230W", "YMR237W",
                            "YMR241W", "YOR312C", "YMR250W", "YMR261C", "YMR300C", "YMR309C", "YMR314W",
                            "YMR315W", "YMR318C", "YNL007C", "YNL010W", "YNL015W", "YNL037C", "YNL044W",
                            "YNL045W", "YNL055C", "YNL064C", "YNL069C", "YNL071W", "YNL079C", "YNL085W",
                            "YNL096C", "YNL098C", "YNL104C", "YNL113W", "YNL121C", "YNL131W", "YNL134C",
                            "YNL135C", "YNL138W", "YNL160W", "YNL178W", "YNL189W", "YNL209W", "YNL220W",
                            "YNL239W", "YNL241C", "YNL244C", "YNL247W", "YNL281W", "YNL287W", "YNL301C",
                            "YNL302C", "YNL307C", "YNR001C", "YNR016C", "YNR021W", "YNR034W-A", "YNR043W",
                            "YNR050C", "YOL027C", "YOL039W", "YOL040C", "YOL058W", "YOL059W", "YOL064C",
                            "YOL086C", "YOL109W", "YOL123W", "YOL127W", "YOL139C", "YOL151W", "YOR007C",
                            "YOR020C", "YOR027W", "YOR046C", "YOR063W", "YOR096W", "YOR117W", "YOR120W",
                            "YOR122C", "YOR136W", "YOR142W", "YOR184W", "YOR187W", "YOR198C", "YOR204W",
                            "YOR209C", "YOR230W", "YOR232W", "YOR234C", "YOR239W", "YOR261C", "YOR270C",
                            "YOR285W", "YOR310C", "YOR317W", "YOR326W", "YOR332W", "YOR335C", "YOR341W",
                            "YOR361C", "YOR362C", "YOR369C", "YOR374W", "YOR375C", "YPL004C", "YPL028W",
                            "YPL037C", "YPL048W", "YPL061W", "YPL078C", "YPL091W", "YPL106C", "YPL117C",
                            "YPL131W", "YPL143W", "YPL145C", "YPL154C", "YPL160W", "YPL171C", "YPL218W",
                            "YPL225W", "YPL226W", "YPL231W", "YPL237W", "YPL240C", "YPL249C-A", "YPL260W",
                            "YPL262W", "YPR035W", "YPR036W", "YPR041W", "YPR069C", "YPR074C", "YPR103W",
                            "YPR133C", "YPR145W", "YPR148C", "YPR159W", "YPR160W", "YPR163C", "YPR165W",
                            "YPR181C", "YPR183W", "YPR191W", "rev_AATM_HORSE", "rev_SYT_METAC", "rev_URIC_BACSU", "rev_YMFD_ECOLI",
                            "YBL005W-B", "YCL019W", "YML045W", "YCL020W", "YAR009C", "YAR010C", "YBL005W-A",
                            "YBL100W-A", "YBL100W-B", "YBR012W-A", "YBR012W-B", "YDR034C-C", "YDR034C-D", "YDR098C-B",
                            "YDR210C-C", "YDR210C-D", "YDR210W-A", "YDR210W-B", "YDR261C-D", "YDR261W-A", "YDR261W-B",
                            "YDR316W-B", "YDR365W-B", "YER138C", "YER160C", "YFL002W-A", "YGR027W-B", "YGR038C-B",
                            "YGR161C-D", "YHR214C-B", "YJR027W", "YJR029W", "YLR035C-A", "YLR157C-B", "YLR227W-B",
                            "YNL054W-A", "YNL284C-A", "YNL284C-B", "YOL103W-A", "YOL103W-B", "YOR142W-B", "YOR343W-B",
                            "YPR158C-D", "YPR158W-B", "YHR216W", "YLR432W", "YAR073W", "YML056C" },
            0.9945, 67, 1.0, 37));
    _listParams.add(new MS2ScoringParams(_test, "yeast/comp12vs12standSCX", "xk_yeast",
            new String[] { "YAL003W", "YAL005C", "YAL012W", "YAL016W", "YAL023C", "YAL035W", "YAL038W",
                            "YAL044C", "YAL049C", "YAL060W", "YAR015W", "YBL002W", "YBL003C", "YBL015W",
                            "YBL024W", "YBL027W", "YBL030C", "YBL045C", "YBL047C", "YBL050W", "YBL064C",
                            "YBL072C", "YBL076C", "YBL092W", "YBL099W", "YBR009C", "YBR011C", "YBR025C",
                            "YBR031W", "YBR035C", "YBR041W", "YBR048W", "YBR054W", "YBR072W", "YBR078W",
                            "YBR079C", "YBR086C", "YBR088C", "YBR109C", "YBR111C", "YBR118W", "YBR121C",
                            "YBR126C", "YBR127C", "YBR143C", "YBR145W", "YBR149W", "YBR169C", "YBR177C",
                            "YBR181C", "YBR189W", "YBR191W", "YBR196C", "YBR208C", "YBR214W", "YGL062W",
                            "YBR221C", "YBR222C", "YBR248C", "YBR249C", "YBR256C", "YBR283C", "YBR286W",
                            "YCL009C", "YCL030C", "YCL040W", "YCL043C", "YCL050C", "YCL057W", "YCR002C",
                            "YCR004C", "YCR009C", "YCR012W", "YCR021C", "YCR031C", "YCR053W", "YCR088W",
                            "YDL004W", "YDL007W", "YDL014W", "YDL022W", "YDL055C", "YDL066W", "YDL072C",
                            "YDL075W", "YDL078C", "YDL081C", "YDL083C", "YDL084W", "YDL097C", "YDL100C",
                            "YDL124W", "YDL126C", "YDL130W", "YDL131W", "YDL136W", "YDL143W", "YDL145C",
                            "YDL171C", "YDL182W", "YDL185W", "YDL192W", "YDL215C", "YDL226C", "YDL229W",
                            "YDR002W", "YDR023W", "YDR032C", "YDR033W", "YDR035W", "YDR037W", "YDR050C",
                            "YDR064W", "YDR091C", "YDR099W", "YDR127W", "YDR129C", "YDR135C", "YDR148C",
                            "YDR155C", "YDR158W", "YDR168W", "YDR174W", "YDR214W", "YDR226W", "YDR232W",
                            "YDR233C", "YDR258C", "YDR341C", "YDR342C", "YDR353W", "YDR368W", "YDR382W",
                            "YDR385W", "YDR388W", "YDR418W", "YDR432W", "YDR447C", "YDR450W", "YDR454C",
                            "YDR471W", "YDR481C", "YDR483W", "YDR487C", "YDR510W", "YDR513W", "YDR516C",
                            "YDR533C", "YEL026W", "YEL034W", "YEL037C", "YJR048W", "YEL046C", "YEL047C",
                            "YEL060C", "YEL071W", "YER003C", "YER009W", "YER025W", "YER036C", "YER043C",
                            "YER049W", "YER052C", "YER057C", "YER062C", "YER074W", "YER081W", "YER090W",
                            "YER091C", "YER094C", "YER103W", "YER110C", "YER120W", "YER133W", "YER165W",
                            "YER177W", "YER178W", "YFL005W", "YFL014W", "YFL016C", "YFL022C", "YFL037W",
                            "YFL039C", "YFL045C", "YFR030W", "YFR031C-A", "YFR044C", "YFR052W", "YFR053C",
                            "YGL008C", "YGL009C", "YGL026C", "YGL030W", "YGL031C", "YGL037C", "YGL054C",
                            "YGL055W", "YGL068W", "YGL076C", "YGL103W", "YGL105W", "YGL106W", "YGL123W",
                            "YGL135W", "YGL137W", "YGL147C", "YGL148W", "YGL187C", "YGL189C", "YGL195W",
                            "YGL202W", "YGL206C", "YGL234W", "YGL242C", "YGL245W", "YGL253W", "YGL256W",
                            "YGR001C", "YGR008C", "YGR019W", "YGR020C", "YGR027C", "YLR344W", "YGR037C",
                            "YGR054W", "YGR061C", "YGR085C", "YGR086C", "YGR094W", "YGR116W", "YGR118W",
                            "YGR124W", "YGR135W", "YGR155W", "YGR159C", "YGR162W", "YGR167W", "YGR180C",
                            "YGR185C", "YGR192C", "YGR193C", "YGR204W", "YGR209C", "YGR214W", "YGR234W",
                            "YGR240C", "YGR244C", "YGR254W", "YGR264C", "YGR282C", "YGR285C", "YHL001W",
                            "YHL015W", "YHL021C", "YHL033C", "YHL034C", "YHR007C", "YHR008C", "YHR018C",
                            "YHR019C", "YHR020W", "YHR021C", "YHR025W", "YHR027C", "YHR030C", "YHR037W",
                            "YHR039C-A", "YHR042W", "YHR047C", "YHR049W", "YHR064C", "YHR068W", "YHR087W",
                            "YHR104W", "YHR128W", "YHR141C", "YHR146W", "YHR158C", "YHR174W", "YHR179W",
                            "YHR183W", "YHR193C", "YHR201C", "YHR203C", "YHR208W", "YIL033C", "YIL034C",
                            "YIL041W", "YIL051C", "YIL053W", "YIL074C", "YIL076W", "YIL078W", "YIL109C",
                            "YIL116W", "YIL125W", "YIL133C", "YIL136W", "YIL142W", "YIR034C", "YIR037W",
                            "YJL008C", "YJL012C", "YJL020C", "YJL026W", "YJL034W", "YJL052W", "YJL080C",
                            "YJL111W", "YJL117W", "YJL123C", "YJL130C", "YJL136C", "YJL138C", "YJL167W",
                            "YJL171C", "YJL172W", "YJL173C", "YJL177W", "YJL190C", "YJL200C", "YJL217W",
                            "YJR007W", "YJR009C", "YJR010W", "YJR016C", "YJR025C", "YJR045C", "YJR059W",
                            "YJR070C", "YJR073C", "YJR077C", "YJR104C", "YJR105W", "YJR121W", "YJR123W",
                            "YJR137C", "YJR139C", "YKL007W", "YKL016C", "YKL029C", "YKL035W", "YKL054C",
                            "YKL056C", "YKL060C", "YKL065C", "YKL067W", "YKL080W", "YKL081W", "YKL085W",
                            "YKL103C", "YKL104C", "YKL127W", "YKL142W", "YKL150W", "YKL152C", "YKL157W",
                            "YKL172W", "YKL180W", "YKL182W", "YKL193C", "YKL195W", "YKL210W", "YKL211C",
                            "YKL212W", "YKR001C", "YKR014C", "YKR046C", "YKR048C", "YKR057W", "YKR076W",
                            "YKR080W", "YLL001W", "YLL018C", "YLL024C", "YLL026W", "YLL045C", "YLL050C",
                            "YLR017W", "YLR027C", "YLR028C", "YLR029C", "YLR043C", "YLR044C", "YLR048W",
                            "YLR058C", "YLR060W", "YLR061W", "YLR075W", "YLR109W", "YLR113W", "YLR150W",
                            "YLR153C", "YLR167W", "YLR175W", "YLR178C", "YLR180W", "YLR192C", "YLR197W",
                            "YLR216C", "YLR249W", "YLR257W", "YLR258W", "YLR259C", "YLR264W", "YLR270W",
                            "YLR289W", "YLR293C", "YLR301W", "YLR303W", "YLR304C", "YLR325C", "YLR335W",
                            "YLR340W", "YLR342W", "YLR354C", "YLR355C", "YLR359W", "YLR429W", "YLR441C",
                            "YLR447C", "YLR448W", "YML008C", "YML022W", "YML028W", "YML048W", "YML057W",
                            "YML063W", "YML070W", "YML072C", "YML073C", "YML085C", "YML086C", "YML106W",
                            "YML124C", "YML126C", "YML128C", "YML130C", "YML131W", "YMR012W", "YMR027W",
                            "YMR038C", "YMR072W", "YMR074C", "YMR079W", "YMR083W", "YMR090W", "YMR092C",
                            "YMR099C", "YMR105C", "YMR108W", "YMR116C", "YMR120C", "YMR142C", "YMR145C",
                            "YMR146C", "YMR173W", "YMR183C", "YMR186W", "YMR189W", "YMR196W", "YMR202W",
                            "YMR203W", "YMR205C", "YMR208W", "YMR217W", "YMR226C", "YMR230W", "YMR237W",
                            "YMR241W", "YOR312C", "YMR250W", "YMR261C", "YMR300C", "YMR309C", "YMR314W",
                            "YMR315W", "YMR318C", "YNL007C", "YNL010W", "YNL015W", "YNL037C", "YNL044W",
                            "YNL045W", "YNL055C", "YNL064C", "YNL069C", "YNL071W", "YNL079C", "YNL085W",
                            "YNL096C", "YNL098C", "YNL104C", "YNL113W", "YNL121C", "YNL131W", "YNL134C",
                            "YNL135C", "YNL138W", "YNL160W", "YNL178W", "YNL189W", "YNL209W", "YNL220W",
                            "YNL239W", "YNL241C", "YNL244C", "YNL247W", "YNL281W", "YNL287W", "YNL301C",
                            "YNL302C", "YNL307C", "YNR001C", "YNR016C", "YNR021W", "YNR034W-A", "YNR043W",
                            "YNR050C", "YOL027C", "YOL039W", "YOL040C", "YOL058W", "YOL059W", "YOL064C",
                            "YOL086C", "YOL109W", "YOL123W", "YOL127W", "YOL139C", "YOL151W", "YOR007C",
                            "YOR020C", "YOR027W", "YOR046C", "YOR063W", "YOR096W", "YOR117W", "YOR120W",
                            "YOR122C", "YOR136W", "YOR142W", "YOR184W", "YOR187W", "YOR198C", "YOR204W",
                            "YOR209C", "YOR230W", "YOR232W", "YOR234C", "YOR239W", "YOR261C", "YOR270C",
                            "YOR285W", "YOR310C", "YOR317W", "YOR326W", "YOR332W", "YOR335C", "YOR341W",
                            "YOR361C", "YOR362C", "YOR369C", "YOR374W", "YOR375C", "YPL004C", "YPL028W",
                            "YPL037C", "YPL048W", "YPL061W", "YPL078C", "YPL091W", "YPL106C", "YPL117C",
                            "YPL131W", "YPL143W", "YPL145C", "YPL154C", "YPL160W", "YPL171C", "YPL218W",
                            "YPL225W", "YPL226W", "YPL231W", "YPL237W", "YPL240C", "YPL249C-A", "YPL260W",
                            "YPL262W", "YPR035W", "YPR036W", "YPR041W", "YPR069C", "YPR074C", "YPR103W",
                            "YPR133C", "YPR145W", "YPR148C", "YPR159W", "YPR160W", "YPR163C", "YPR165W",
                            "YPR181C", "YPR183W", "YPR191W", "rev_AATM_HORSE", "rev_SYT_METAC", "rev_URIC_BACSU", "rev_YMFD_ECOLI",
                            "YBL005W-B", "YCL019W", "YML045W", "YCL020W", "YAR009C", "YAR010C", "YBL005W-A",
                            "YBL100W-A", "YBL100W-B", "YBR012W-A", "YBR012W-B", "YDR034C-C", "YDR034C-D", "YDR098C-B",
                            "YDR210C-C", "YDR210C-D", "YDR210W-A", "YDR210W-B", "YDR261C-D", "YDR261W-A", "YDR261W-B",
                            "YDR316W-B", "YDR365W-B", "YER138C", "YER160C", "YFL002W-A", "YGR027W-B", "YGR038C-B",
                            "YGR161C-D", "YHR214C-B", "YJR027W", "YJR029W", "YLR035C-A", "YLR157C-B", "YLR227W-B",
                            "YNL054W-A", "YNL284C-A", "YNL284C-B", "YOL103W-A", "YOL103W-B", "YOR142W-B", "YOR343W-B",
                            "YPR158C-D", "YPR158W-B", "YHR216W", "YLR432W", "YAR073W", "YML056C",
                            // k-score only
                            "YBR082C", "YBR093C", "YDR071C", "YFR015C", "YIL094C", "YJL189W",
                            "YJR065C", "YJR094W-A", "YJR109C", "YLR195C", "YNL016W", "YPR033C",
                            "YMR050C"
            },
//                  0.9945, 67, 1.0, 37)); X! Comet
            0.9967, 105, 1.0, 57));

    _listParams.add(new MS2ScoringParams(_test, "human/Hupo_PPP", "xt_hupo",
            new String[] { "IPI00017601", "IPI00019580", "IPI00641179", "IPI00020091", "IPI00021841", "IPI00021854", "IPI00021857",
                            "IPI00029717", "IPI00219713", "IPI00022229", "IPI00022371", "IPI00556632", "IPI00480192", "IPI00022429",
                            "IPI00022431", "IPI00022432", "IPI00022434", "IPI00022463", "IPI00022488", "IPI00029863", "IPI00032179",
                            "IPI00215894", "IPI00386785", "IPI00164623", "IPI00166729", "IPI00218732", "IPI00291262", "IPI00556459",
                            "IPI00292530", "IPI00298497", "IPI00298971", "IPI00304273", "IPI00645038", "IPI00385252", "IPI00399007",
                            "IPI00418163", "IPI00465313", "IPI00472610", "IPI00477597", "IPI00550991", "IPI00553177", "IPI00555746",
                            "IPI00555812", "IPI00022895", "IPI00646799", "IPI00644019", "IPI00030205", "IPI00550996", "IPI00430808",
                            "IPI00550315", "IPI00556287", "IPI00641270", "IPI00061977", "IPI00423460", "IPI00430844", "IPI00384952",
                            "IPI00423461", "IPI00473015", "IPI00646045", "IPI00168728", "IPI00448800", "IPI00382606", "IPI00383732",
                            "IPI00645627", "IPI00423464", "IPI00441196", "IPI00442911", "IPI00470798", "IPI00549304", "IPI00550640",
                            "IPI00551005", "IPI00640198", "IPI00642082", "IPI00642632", "IPI00642967", "IPI00645071", "IPI00645569",
                            "IPI00645844", "IPI00654769", "IPI00218192", "IPI00294193", "IPI00556036", "IPI00384697", "IPI00216773",
                            "IPI00385264", "IPI00479169", "IPI00477090", "IPI00645352", "IPI00647394", "IPI00641737", "IPI00431645",
                            "IPI00478493" },
            0.9999, 19, 0.99, 11));
    _listParams.add(new MS2ScoringParams(_test, "human/Hupo_PPP", "xc_hupo",
            new String[] { "IPI00017601", "IPI00019568", "IPI00019580", "IPI00641179", "IPI00019943", "IPI00020091", "IPI00021841",
                            "IPI00021854", "IPI00021857", "IPI00029717", "IPI00219713", "IPI00022229", "IPI00022371", "IPI00480192",
                            "IPI00022429", "IPI00022431", "IPI00022432", "IPI00022434", "IPI00022463", "IPI00022488", "IPI00513782",
                            "IPI00029863", "IPI00032179", "IPI00032220", "IPI00032291", "IPI00215894", "IPI00386785", "IPI00163207",
                            "IPI00164623", "IPI00166729", "IPI00514475", "IPI00218732", "IPI00291262", "IPI00291866", "IPI00292530",
                            "IPI00298497", "IPI00298828", "IPI00298971", "IPI00304273", "IPI00645038", "IPI00334432", "IPI00477069",
                            "IPI00385252", "IPI00399007", "IPI00418163", "IPI00472610", "IPI00477597", "IPI00478003", "IPI00550991",
                            "IPI00553177", "IPI00555746", "IPI00555812", "TRYP_PIG", "rev_UniRef50_Q22F33", "rev_UniRef50_Q3SKD1", "IPI00339224",
                            "IPI00339319", "IPI00556632", "IPI00022895", "IPI00646799", "IPI00644019", "IPI00030205", "IPI00550996",
                            "IPI00430808", "IPI00550315", "IPI00556287", "IPI00641270", "IPI00168728", "IPI00550640", "IPI00448800",
                            "IPI00382606", "IPI00383732", "IPI00645627", "IPI00423464", "IPI00642129", "IPI00426069", "IPI00441196",
                            "IPI00442911", "IPI00470798", "IPI00549304", "IPI00549440", "IPI00549462", "IPI00549576", "IPI00551005",
                            "IPI00640198", "IPI00642082", "IPI00642193", "IPI00642632", "IPI00642967", "IPI00645071", "IPI00645569",
                            "IPI00645822", "IPI00645844", "IPI00654769", "IPI00216773", "IPI00384697", "IPI00218192", "IPI00294193",
                            "IPI00556036", "IPI00385264", "IPI00479169", "IPI00477090", "IPI00645352", "IPI00647394", "IPI00641737",
                            "IPI00431645", "IPI00478493", "IPI00646736", "IPI00061977", "IPI00646280", "IPI00423460", "IPI00430844",
                            "IPI00384952", "IPI00423461", "IPI00473015", "IPI00643867", "IPI00644397", "IPI00646045" },
            0.9967, 50, 1.0, 33));
    _listParams.add(new MS2ScoringParams(_test, "human/Hupo_PPP", "xk_hupo",
            new String[] { "IPI00017601", "IPI00019568", "IPI00019580", "IPI00641179", "IPI00019943", "IPI00020091", "IPI00021841",
                            "IPI00021854", "IPI00021857", "IPI00029717", "IPI00219713", "IPI00022229", "IPI00022371", "IPI00480192",
                            "IPI00022429", "IPI00022431", "IPI00022432", "IPI00022434", "IPI00022463", "IPI00022488", "IPI00513782",
                            "IPI00029863", "IPI00032179", "IPI00032220", "IPI00032291", "IPI00215894", "IPI00386785", "IPI00163207",
                            "IPI00164623", "IPI00166729", "IPI00514475", "IPI00218732", "IPI00291262", "IPI00291866", "IPI00292530",
                            "IPI00298497", "IPI00298828", "IPI00298971", "IPI00304273", "IPI00645038", "IPI00334432", "IPI00477069",
                            "IPI00385252", "IPI00399007", "IPI00418163", "IPI00472610", "IPI00477597", "IPI00478003", "IPI00550991",
                            "IPI00553177", "IPI00555746", "IPI00555812", "TRYP_PIG", "rev_UniRef50_Q22F33", "rev_UniRef50_Q3SKD1", "IPI00339224",
                            "IPI00339319", "IPI00556632", "IPI00022895", "IPI00646799", "IPI00644019", "IPI00030205", "IPI00550996",
                            "IPI00430808", "IPI00550315", "IPI00556287", "IPI00641270", "IPI00168728", "IPI00550640", "IPI00448800",
                            "IPI00382606", "IPI00383732", "IPI00645627", "IPI00423464", "IPI00642129", "IPI00426069", "IPI00441196",
                            "IPI00442911", "IPI00470798", "IPI00549304", "IPI00549440", "IPI00549462", "IPI00549576", "IPI00551005",
                            "IPI00640198", "IPI00642082", "IPI00642193", "IPI00642632", "IPI00642967", "IPI00645071", "IPI00645569",
                            "IPI00645822", "IPI00645844", "IPI00654769", "IPI00216773", "IPI00384697", "IPI00218192", "IPI00294193",
                            "IPI00556036", "IPI00385264", "IPI00479169", "IPI00477090", "IPI00645352", "IPI00647394", "IPI00641737",
                            "IPI00431645", "IPI00478493", "IPI00646736", "IPI00061977", "IPI00646280", "IPI00423460", "IPI00430844",
                            "IPI00384952", "IPI00423461", "IPI00473015", "IPI00643867", "IPI00644397", "IPI00646045",
                    // k-score only
                            "IPI00021727", "IPI00550438", "IPI00641354", "IPI00553132"
            },
            0.995, 48, 1.0, 23));
}

public void addTestsISBMix()
{
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/FT", "xt_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/FT", "xk_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/LCQ", "xt_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/LCQ", "xk_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/LTQ", "xt_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/LTQ", "xk_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/QSTAR", "xt_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/QSTAR", "xk_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/QTOF", "xt_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
    _listParams.add(new MS2ScoringParams(_test, "mix/ISB_18Mix/QTOF", "xk_isbmix",
            new String[] {  },
            0.0, 0, 0.0, 0));
}

public void addTestsScoringMix()
{
    _listParams.add(new MS2ScoringParams(_test, "mix/Keller_omics", "xt_komics",
            new String[] { "ACTA_BOVIN", "ALBU_BOVIN", "AMY_BACLI", "BGAL_ECOLI", "CAH2_BOVIN", "CASB_BOVIN",
                            "CATA_BOVIN", "CYC_BOVIN", "G3P_RABIT", "LACB_BOVIN", "LALBA_BOVIN", "PHS2_RABIT",
                            "PPB_ECOLI", "TRFE_BOVIN" },
            0.9977, 36, 0.96, 7));
    _listParams.add(new MS2ScoringParams(_test, "mix/Keller_omics", "xc_komics",
            new String[] { "ACTA_BOVIN", "ALBU_BOVIN", "AMY_BACLI", "BGAL_ECOLI", "CAH2_BOVIN", "CASB_BOVIN",
                            "CATA_BOVIN", "CYC_BOVIN", "G3P_RABIT", "LACB_BOVIN", "LALBA_BOVIN", "PHS2_RABIT",
                            "PPB_ECOLI", "TRFE_BOVIN" },
            0.9901, 106, 0.99, 33));
    _listParams.add(new MS2ScoringParams(_test, "mix/Keller_omics", "xk_komics",
            new String[] { "ACTA_BOVIN", "ALBU_BOVIN", "AMY_BACLI", "BGAL_ECOLI", "CAH2_BOVIN", "CASB_BOVIN",
                            "CATA_BOVIN", "CYC_BOVIN", "G3P_RABIT", "LACB_BOVIN", "LALBA_BOVIN", "PHS2_RABIT",
                            "PPB_ECOLI", "TRFE_BOVIN", "rev_RBL2_RHOCA", "rev_RR5_CYACA", "rev_RRF_PHOLL", "rev_YKOW_BACSU" },
            0.9926, 152, 1.0, 58));
}

public void addTestsQuant()
{
    // Q3 quantitation
    MS2QuantParams qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "BSA_10_1");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(12.86, 5.6));
    qp.addRatio("UPSP:APOH_BOVIN", new MS2QuantRatio(4.39, 0.68));
    qp.addRatio("UPSP:CO3_BOVIN", new MS2QuantRatio(16.21, 5.31));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(7.77, 0.38));
    qp.addRatio("UPSP:TRFE_BOVIN", new MS2QuantRatio(16.79, 6.5));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(13.91, 6.2));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "BSA_5_1");
    qp.addRatio("TRYP_PIG", new MS2QuantRatio(5.15, 0.0));
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(6.07, 1.35));
    qp.addRatio("UPSP:ANT3_BOVIN", new MS2QuantRatio(4.31, 0.2));
    qp.addRatio("UPSP:APOH_BOVIN", new MS2QuantRatio(4.4, 0.27));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(6.48, 0.79));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(5.27, 0.19));
    qp.addRatio("UPSP:TRFE_BOVIN", new MS2QuantRatio(4.24, 0.41));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(6.55, 1.35));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_3-1");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(3.76, 0.81));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(3.24, 0.41));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(2.81, 0.01));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(3.88, 0.99));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_1-1");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(1.09, 0.07));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(0.96, 0.03));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(0.89, 0.01));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(1.11, 0.06));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_1-3");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(0.34, 0.02));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(0.36, 0.02));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(0.33, 0.02));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(0.35, 0.02));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_1-5");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(0.2, 0.02));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(0.16, 0.06));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(0.21, 0.01));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(0.21, 0.02));
    qp.addRatio("UPSP:APOH_BOVIN", new MS2QuantRatio(0.13, 0.0));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_q3_75", "L_04_BSA_D0-D3_1-10");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(0.09, 0.01));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(0.1, 0.02));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(0.13, 0.03));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(0.09, 0.01));
    _listParams.add(qp);

    // Xpress quantitation
    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "BSA_10_1");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(3.33, 2.27));
    qp.addRatio("UPSP:ANT3_BOVIN", new MS2QuantRatio(4.35, 0.6));
    qp.addRatio("UPSP:APOH_BOVIN", new MS2QuantRatio(2.15, 0.43));
    qp.addRatio("UPSP:CO3_BOVIN", new MS2QuantRatio(8.11, 2.28));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(7.83, 3.07));
    qp.addRatio("UPSP:TRFE_BOVIN", new MS2QuantRatio(8.97, 0.79));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(2.36, 1.1));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "BSA_5_1");
    qp.addRatio("TRYP_PIG", new MS2QuantRatio(4.54, 0.0));
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(2.16, 1.35));
    qp.addRatio("UPSP:ANT3_BOVIN", new MS2QuantRatio(4.79, 0.04));
    qp.addRatio("UPSP:APOH_BOVIN", new MS2QuantRatio(2.22, 0.3));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(5.5, 0.56));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(3.93, 1.28));
    qp.addRatio("UPSP:TRFE_BOVIN", new MS2QuantRatio(4.85, 0.64));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(3.2, 1.4));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_3-1");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(2.3, 1.78));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(2.41, 0.26));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(2.86, 0.23));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(10.29, 11.92));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_1-1");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(1.06, 0.18));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(1.48, 0.17));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(0.8, 0.07));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(1.01, 0.05));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_1-3");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(0.24, 0.11));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(0.71, 0.2));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(0.33, 0.06));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(0.28, 0.01));
    qp.addRatio("UPSP:APOH_BOVIN", new MS2QuantRatio(4.52, 3.35));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_1-5");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(0.21, 0.05));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(0.23, 0.02));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(0.56, 0.28));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(0.22, 0.03));
    qp.addRatio("UPSP:APOH_BOVIN", new MS2QuantRatio(0.86, 0.0));
    _listParams.add(qp);

    qp = new MS2QuantParams(_test, "quant/acrylamide", "xc_bov_qc_5", "L_04_BSA_D0-D3_1-10");
    qp.addRatio("UPSP:ALBU_BOVIN", new MS2QuantRatio(0.09, 0.05));
    qp.addRatio("UPTR:O97941_BOVIN", new MS2QuantRatio(0.14, 0.04));
    qp.addRatio("UPSP:CO4_BOVIN", new MS2QuantRatio(0.2, 0.04));
    qp.addRatio("UPTR:Q3SZR2_BOVIN", new MS2QuantRatio(0.17, 0.04));
    _listParams.add(qp);
    //*/
}
}
