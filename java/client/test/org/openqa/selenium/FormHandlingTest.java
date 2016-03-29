// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.openqa.selenium.support.ui.ExpectedConditions.titleIs;
import static org.openqa.selenium.testing.Driver.HTMLUNIT;
import static org.openqa.selenium.testing.Driver.IE;
import static org.openqa.selenium.testing.Driver.MARIONETTE;
import static org.openqa.selenium.testing.Driver.PHANTOMJS;
import static org.openqa.selenium.testing.Driver.SAFARI;
import static org.openqa.selenium.testing.TestUtilities.isIe6;
import static org.openqa.selenium.testing.TestUtilities.isIe7;

import org.junit.Test;
import org.openqa.selenium.testing.Ignore;
import org.openqa.selenium.testing.JUnit4TestBase;
import org.openqa.selenium.testing.TestUtilities;

import java.io.File;
import java.io.IOException;

public class FormHandlingTest extends JUnit4TestBase {

  @Test
  public void testShouldClickOnSubmitInputElements() {
    driver.get(pages.formPage);
    driver.findElement(By.id("submitButton")).click();
    wait.until(titleIs("We Arrive Here"));
    assertThat(driver.getTitle(), equalTo("We Arrive Here"));
  }

  @Test
  public void testClickingOnUnclickableElementsDoesNothing() {
    driver.get(pages.formPage);
    try {
      driver.findElement(By.xpath("//body")).click();
    } catch (Exception e) {
      e.printStackTrace();
      fail("Clicking on the unclickable should be a no-op");
    }
  }

  @Test
  public void testShouldBeAbleToClickImageButtons() {
    driver.get(pages.formPage);
    driver.findElement(By.id("imageButton")).click();
    wait.until(titleIs("We Arrive Here"));
    assertThat(driver.getTitle(), equalTo("We Arrive Here"));
  }

  @Test
  public void testShouldBeAbleToSubmitForms() {
    driver.get(pages.formPage);
    driver.findElement(By.name("login")).submit();
    wait.until(titleIs("We Arrive Here"));
  }

  @Test
  public void testShouldSubmitAFormWhenAnyInputElementWithinThatFormIsSubmitted() {
    driver.get(pages.formPage);
    driver.findElement(By.id("checky")).submit();
    wait.until(titleIs("We Arrive Here"));
  }

  @Test
  public void testShouldSubmitAFormWhenAnyElementWithinThatFormIsSubmitted() {
    driver.get(pages.formPage);
    driver.findElement(By.xpath("//form/p")).submit();
    wait.until(titleIs("We Arrive Here"));
  }

  @Test(expected = NoSuchElementException.class)
  @Ignore(value = {PHANTOMJS, SAFARI})
  public void testShouldNotBeAbleToSubmitAFormThatDoesNotExist() {
    driver.get(pages.formPage);
    driver.findElement(By.name("SearchableText")).submit();
  }

  @Test
  public void testShouldBeAbleToEnterTextIntoATextAreaBySettingItsValue() {
    driver.get(pages.javascriptPage);
    WebElement textarea = driver.findElement(By.id("keyUpArea"));
    String cheesy = "brie and cheddar";
    textarea.sendKeys(cheesy);
    assertThat(textarea.getAttribute("value"), equalTo(cheesy));
  }

  @Test
  public void testSendKeysKeepsCapitalization() {
    driver.get(pages.javascriptPage);
    WebElement textarea = driver.findElement(By
                                                 .id("keyUpArea"));
    String cheesey = "BrIe And CheDdar";
    textarea.sendKeys(cheesey);
    assertThat(textarea.getAttribute("value"), equalTo(cheesey));
  }

  @Test
  @Ignore(MARIONETTE)
  public void testShouldSubmitAFormUsingTheNewlineLiteral() {
    driver.get(pages.formPage);
    WebElement nestedForm = driver.findElement(By.id("nested_form"));
    WebElement input = nestedForm.findElement(By.name("x"));
    input.sendKeys("\n");
    wait.until(titleIs("We Arrive Here"));
    assertTrue(driver.getCurrentUrl().endsWith("?x=name"));
  }

  @Test
  public void testShouldSubmitAFormUsingTheEnterKey() {
    driver.get(pages.formPage);
    WebElement nestedForm = driver.findElement(By.id("nested_form"));
    WebElement input = nestedForm.findElement(By.name("x"));
    input.sendKeys(Keys.ENTER);
    wait.until(titleIs("We Arrive Here"));
    assertTrue(driver.getCurrentUrl().endsWith("?x=name"));
  }

  @Test
  public void testShouldEnterDataIntoFormFields() {
    driver.get(pages.xhtmlTestPage);
    WebElement element = driver.findElement(By.xpath("//form[@name='someForm']/input[@id='username']"));
    String originalValue = element.getAttribute("value");
    assertThat(originalValue, equalTo("change"));

    element.clear();
    element.sendKeys("some text");

    element = driver.findElement(By.xpath("//form[@name='someForm']/input[@id='username']"));
    String newFormValue = element.getAttribute("value");
    assertThat(newFormValue, equalTo("some text"));
  }

  @Ignore(value = {SAFARI, MARIONETTE},
          reason = "Does not yet support file uploads", issues = {4220})
  @Test
  public void testShouldBeAbleToAlterTheContentsOfAFileUploadInputElement() throws IOException {
    driver.get(pages.formPage);
    WebElement uploadElement = driver.findElement(By.id("upload"));
    assertThat(uploadElement.getAttribute("value"), equalTo(""));

    File file = File.createTempFile("test", "txt");
    file.deleteOnExit();

    uploadElement.sendKeys(file.getAbsolutePath());

    String uploadPath = uploadElement.getAttribute("value");
    assertTrue(uploadPath.endsWith(file.getName()));
  }

  @Ignore(value = {SAFARI, MARIONETTE},
          reason = "Does not yet support file uploads", issues = {4220})
  @Test
  public void testShouldBeAbleToSendKeysToAFileUploadInputElementInAnXhtmlDocument()
      throws IOException {
    assumeFalse("IE before 9 doesn't handle pages served with an XHTML content type,"
                + " and just prompts for to download it",
                TestUtilities.isOldIe(driver));

    driver.get(pages.xhtmlFormPage);
    WebElement uploadElement = driver.findElement(By.id("file"));
    assertThat(uploadElement.getAttribute("value"), equalTo(""));

    File file = File.createTempFile("test", "txt");
    file.deleteOnExit();

    uploadElement.sendKeys(file.getAbsolutePath());

    String uploadPath = uploadElement.getAttribute("value");
    assertTrue(uploadPath.endsWith(file.getName()));
  }

  @Ignore(value = {SAFARI},
          reason = "Does not yet support file uploads", issues = {4220})
  @Test
  public void testShouldBeAbleToUploadTheSameFileTwice() throws IOException {
    File file = File.createTempFile("test", "txt");
    file.deleteOnExit();

    driver.get(pages.formPage);
    WebElement uploadElement = driver.findElement(By.id("upload"));
    assertThat(uploadElement.getAttribute("value"), equalTo(""));

    uploadElement.sendKeys(file.getAbsolutePath());
    uploadElement.submit();

    driver.get(pages.formPage);
    uploadElement = driver.findElement(By.id("upload"));
    assertThat(uploadElement.getAttribute("value"), equalTo(""));

    uploadElement.sendKeys(file.getAbsolutePath());
    uploadElement.submit();

    // If we get this far, then we're all good.
  }

  @Test
  public void testSendingKeyboardEventsShouldAppendTextInInputs() {
    driver.get(pages.formPage);
    WebElement element = driver.findElement(By.id("working"));
    element.sendKeys("some");
    String value = element.getAttribute("value");
    assertThat(value, is("some"));

    element.sendKeys(" text");
    value = element.getAttribute("value");
    assertThat(value, is("some text"));
  }

  @Test
  public void testSendingKeyboardEventsShouldAppendTextInInputsWithExistingValue() {
    driver.get(pages.formPage);
    WebElement element = driver.findElement(By.id("inputWithText"));
    element.sendKeys(". Some text");
    String value = element.getAttribute("value");

    assertThat(value, is("Example text. Some text"));
  }

  @Test
  public void testSendingKeyboardEventsShouldAppendTextInTextAreas() {
    driver.get(pages.formPage);
    WebElement element = driver.findElement(By.id("withText"));

    element.sendKeys(". Some text");
    String value = element.getAttribute("value");

    assertThat(value, is("Example text. Some text"));
  }

  @Test
  public void testEmptyTextBoxesShouldReturnAnEmptyStringNotNull() {
    driver.get(pages.formPage);
    WebElement emptyTextBox = driver.findElement(By.id("working"));
    assertEquals(emptyTextBox.getAttribute("value"), "");
  }

  @Test
  @Ignore(value = {PHANTOMJS, SAFARI, HTMLUNIT, MARIONETTE},
          reason = "HtmlUnit: error; others: untested")
  public void handleFormWithJavascriptAction() {
    String url = appServer.whereIs("form_handling_js_submit.html");
    driver.get(url);
    WebElement element = driver.findElement(By.id("theForm"));
    element.submit();
    Alert alert = driver.switchTo().alert();
    String text = alert.getText();
    alert.accept();

    assertEquals("Tasty cheese", text);
  }

  @Ignore(value = {SAFARI}, reason = "untested")
  @Test
  public void testCanClickOnASubmitButton() {
    checkSubmitButton("internal_explicit_submit");
  }


  @Ignore(value = {SAFARI}, reason = "untested")
  @Test
  public void testCanClickOnASubmitButtonNestedSpan() {
    checkSubmitButton("internal_span_submit");
  }

  @Ignore(value = {SAFARI}, reason = "untested")
  @Test
  public void testCanClickOnAnImplicitSubmitButton() {
    assumeFalse(isIe6(driver) || isIe7(driver) );
    checkSubmitButton("internal_implicit_submit");
  }

  @Ignore(value = {IE, SAFARI},
          reason = "IE: failed; Others: untested")
  @Test
  public void testCanClickOnAnExternalSubmitButton() {
    checkSubmitButton("external_explicit_submit");
  }

  @Ignore(value = {IE, SAFARI},
      reason = "IE: failed; Others: untested")
  @Test
  public void testCanClickOnAnExternalImplicitSubmitButton() {
    checkSubmitButton("external_implicit_submit");
  }

  private void checkSubmitButton(String buttonId) {
    driver.get(appServer.whereIs("click_tests/html5_submit_buttons.html"));
    String name = "Gromit";

    driver.findElement(By.id("name")).sendKeys(name);
    driver.findElement(By.id(buttonId)).click();

    wait.until(titleIs("Submitted Successfully!"));

    assertThat(driver.getCurrentUrl(), containsString("name="+name));
  }
}
