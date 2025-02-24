/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.pages.signaldata;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Arrays;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;
import static org.labkey.test.components.ext4.Window.Window;

public class SignalDataUploadPage
{
    private static final String CLEAR_BUTTON = "Clear Run";

    private BaseWebDriverTest _test;

    public SignalDataUploadPage(BaseWebDriverTest test)
    {
        _test = test;
        waitForInitialState();
    }

    public void uploadMetadataFile(File file)
    {
        _test.setFormElement(SignalDataUploadPage.Locators.metadataFileInput, file);
        _test.click(Locators.uploadMetadataButton);
        _test.waitForElement(Locators.runIdentifier);
    }

    public void uploadFile(File... file)
    {
        sleep(1_000);
        WebElement dropFileInputEl = Locators.dropFileInput.findElement(_test.getDriver());
        _test.setInput(dropFileInputEl, Arrays.asList(file));
    }

    public void waitForProgressBars(int count)
    {
        WebDriverWrapper.waitFor(() ->
                Locator.byClass("x4-progress-bar").withAttribute("style", "width:100%")
                        .findElements(_test.getDriver()).size() == count,
                "Upload didn't finish.", 5_000);
        sleep(1_000);
    }

    public void uploadIncorrectFile(File file)
    {
        uploadFile(file);
        WebElement msgBox = Locators.fileNotUploadedMsgBox(file.getName()).waitForElement(_test.getDriver(), WAIT_FOR_JAVASCRIPT);
        msgBox.sendKeys(Keys.ESCAPE);
        _test.shortWait().until(ExpectedConditions.invisibilityOf(msgBox));
        sleep(1_000);
    }

    public void deleteFile(String fileName)
    {
        _test.click(SignalDataUploadPage.Locators.fileLogDeleteCell(fileName));
        _test.waitForElementToDisappear(SignalDataUploadPage.Locators.fileLogCellwithText(fileName));
    }

    public void setRunIDField(String runName)
    {
        WebElement runIdField = Locators.runIdentifier.findElement(_test.getDriver());
        _test.setFormElement(runIdField, runName);
        _test.waitForFormElementToEqual(runIdField, runName);
        _test.fireEvent(runIdField, WebDriverWrapper.SeleniumEvent.blur);
        WebDriverWrapper.waitFor(() -> !runIdField.getAttribute("class").contains("invalid"), 1000);
    }

    private void waitForInitialState()
    {
        _test.waitForElement(SignalDataUploadPage.Locators.metadataFileInput.notHidden(), 5000);
    }

    public void clearRun()
    {
        _test.clickButton(CLEAR_BUTTON, 0);
        final Window clearConfirm = Window(_test.getDriver()).withTitle("Clear Run").waitFor();
        clearConfirm.clickButton("Yes", true);
        _test.waitForElementToDisappear(Ext4Helper.Locators.getGridRow()); //Check grid is cleared
        waitForInitialState();
    }

    public void saveRun()
    {
        WebElement saveButton = Locators.saveButton.findElement(_test.getDriver());
        WebDriverWrapper.waitFor(() -> !saveButton.getAttribute("class").contains("disabled"), "Unable to save, button is disabled", 1000);
        _test.clickAndWait(Locators.saveButton);
    }

    private static class Locators
    {
        static final Locator.XPathLocator runIdentifier = Locator.input("RunIdentifier").notHidden();
        static final Locator.XPathLocator metadataFileInput = Locator.tagWithAttribute("input", "type", "file").withoutAttribute("multiple", "multiple");
        static final Locator.XPathLocator dropFileInput = Locator.tagWithAttribute("input", "type", "file").withAttribute("multiple", "multiple");
        static final Locator.XPathLocator uploadMetadataButton = Locator.tagWithAttribute("a", "role", "button").withDescendant(Locator.tag("span").withText("next"));
        static final Locator saveButton = Ext4Helper.Locators.ext4Button("Save Run");

        static Locator.XPathLocator fileLogCellwithText(String text)
        {
            return Locator.xpath("//td[@role='gridcell']//*[text()='" + text + "']");
        }

        static Locator.XPathLocator fileNotUploadedMsgBox(String fileName)
        {
            return Locator.tagWithClass("div", "x4-message-box").notHidden()
                    .withDescendant(Locator.tagWithAttribute("div", "role", "input").withText(fileName + " not uploaded"));
        }

        /**
         * @param filename of row to find
         */
        static Locator.XPathLocator fileLogDeleteCell(String filename) {
            return Locator.tagWithAttribute("tr","role", "row").withDescendant(Locator.xpath("//*[text()='" + filename + "']"))
                    .append("//img[contains(@class, 'iconDelete')]");
        }
    }
}
