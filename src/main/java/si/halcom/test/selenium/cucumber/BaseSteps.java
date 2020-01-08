package si.halcom.test.selenium.cucumber;

import static si.halcom.test.selenium.SeleniumDriverHandler.DRIVER;
import static si.halcom.test.selenium.SeleniumDriverHandler.WAIT;

import java.io.*;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

//import com.sun.xml.internal.bind.v2.model.impl.ModelBuilderI;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.bys.builder.AppiumByBuilder;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.appium.java_client.touch.offset.PointOption;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.halcom.ebank.entities.ClientData;
import com.halcom.ebank.entities.ContractData;
import com.halcom.ebank.entities.DataProviderType;
import com.halcom.ebank.entities.DocumentData;
import com.halcom.ebank.entities.DocumentType;
import com.halcom.ebank.entities.LicenceData;
import com.halcom.ebank.model.PutUserRequest;
import com.halcom.ebank.wrapper.DocSpecificDataWrapper;
import com.halcom.ng.dictionary.DicContractOwnerType;
import com.halcom.ng.dictionary.DicContractType;
import com.halcom.ng.dictionary.DicLicenseType;
import com.halcom.ng.dictionary.DicPermission;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import cucumber.api.DataTable;
import cucumber.api.Scenario;
import generated.DocSpecificData;
import gherkin.formatter.model.DataTableRow;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;
import ru.yandex.qatools.ashot.shooting.SimpleShootingStrategy;
import ru.yandex.qatools.ashot.shooting.ViewportPastingDecorator;
import si.halcom.test.selenium.SeleniumDriverHandler;
import si.halcom.test.selenium.Settings;
import si.halcom.test.selenium.cucumber.RobotThread.CertUsage;

@Component
public abstract class BaseSteps implements BaseStepsInterface {

    // the guava csv splitter
    protected final Splitter SPLITTER_CSV = Splitter.on(',').trimResults().omitEmptyStrings();
    // the guava csv splitter that keeps the empty strings also
    protected final Splitter SPLITTER_CSV_KEEP_EMPTY_STRING = Splitter.on(',').trimResults();
    // click delay for test development
    private final int CLICK_DELAY;
    // max retries on error
    protected final int MAX_RETRIES_ON_ERROR;
    // landing page of type home
    protected static final String LANDING_PAGE_HOME = "home";
    // landing page of type multibank home
    protected static final String LANDING_PAGE_MULTIBANK_HOME = "multibank home";

    protected boolean translationsEnabled;

    protected String systemId;
    protected Scenario scenario;
    protected ObjectMapper objectMapper;
    protected DocSpecificDataWrapper docSpecificDataWrapper;

    public static final Map<String, String> credentials = new HashMap<>();


    public BaseSteps() {
        Settings.initSettings();

        CLICK_DELAY = Settings.getClickDelay();
        MAX_RETRIES_ON_ERROR = 3;
        translationsEnabled = Settings.areTranslationsEnabled();
        systemId = null;
        objectMapper = new ObjectMapper();
        readDocSpecificData();
        
        
    }

    /**
     * @param message text to write to scenario output
     */
    protected void logDebug(String message) {
        if (Settings.isVerboseLogs() && scenario != null) {
            scenario.write(message);
        }
    }

    public Scenario getScenario() {
        return scenario;
    }

    @Override
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
        getSystemIdFromScenario();
    }

    private void getSystemIdFromScenario() {
        systemId = scenario.getSourceTagNames().stream()
                .filter(tag -> tag.startsWith("@SystemId_"))
                .map(tag -> tag.split("_")[1])
                .findFirst().orElse(null);
    }

    private long startTime;

    protected void logStopWatchDebug(String message) {
        logStopWatchDebug(message, false);
    }

    /**
     * @param message text to write to scenario output
     */
    protected void logStopWatchDebug(String message, boolean end) {
        long now = System.currentTimeMillis();
        long diff = now - startTime;
        BigDecimal sec = BigDecimal.valueOf(diff).divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
        if (!end) {
            if (startTime != 0) {
                logDebug("Last operation took " + sec + " s");
            }
            logDebug("START " + message);
            startTime = now;
        } else {
            startTime = 0;
            logDebug("STOP " + message);
            logDebug("Last operation took " + sec + " s");
        }
    }


    public byte[] createScreenshot(Scenario scenario) throws Exception {
        byte[] screenshotBytes;
        if (!Settings.isScreenshotWithAShot()) {
            // original way how to take screenshot
            File screenshotFile = ((TakesScreenshot) SeleniumDriverHandler.DRIVER).getScreenshotAs(OutputType.FILE);
            screenshotBytes = Files.readAllBytes(screenshotFile.toPath());
        } else {
            // alternative way for screenshots
            ShootingStrategy strategy = new ViewportPastingDecorator(new SimpleShootingStrategy()) {

                private static final long serialVersionUID = 1L;

                private boolean skipFirst = true;

                protected void scrollVertically(JavascriptExecutor js, int scrollY) {

                    super.scrollVertically(js, scrollY);

                    try {
                        // remove header
                        if (!skipFirst) {
                            js.executeScript("document.getElementById('header_navbar').remove();");
                        }
                    } catch (Exception e) {
                        // ignore error if header not found
                    }

                    skipFirst = false;
                }

            }.withScrollTimeout(100);

            Screenshot screenshot;
            if(SeleniumDriverHandler.DRIVER instanceof InternetExplorerDriver) {
            	//IE11 driver has some issues with shootingStratgy and returns a nullpointer exception
            	//when we upgrade IE11 driver it should be checked if the problem persists
            	screenshot = new AShot()
                        .takeScreenshot(SeleniumDriverHandler.DRIVER);
            } else {
            	screenshot = new AShot()
                        .shootingStrategy(strategy /*ShootingStrategies.viewportPasting(100)*/)
                        .takeScreenshot(SeleniumDriverHandler.DRIVER);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenshot.getImage(), "PNG", baos);
            screenshotBytes = baos.toByteArray();
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        URL resource = getClass().getClassLoader().getResource("settings.properties");
        if (resource == null) {
            return screenshotBytes;
        }
        String resourcePath = resource.getPath();
        String filename = df.format(new Date());
        filename += "_ScreenShot.png";
        // For Windows OS, skip the first slash
        int beginIdx = 0;
        if (isWindowsOS()) {
            beginIdx = 1;
        }
        String absolutePath = resourcePath.substring(beginIdx, resourcePath.lastIndexOf("/") + 1) + filename;
        // copy file to output
        File f = new File(absolutePath);
        Files.write(f.toPath(), screenshotBytes);
        if (scenario != null) {
            scenario.write("Screenshot of the failed scenario [" + scenario.getName() + "] saved in file: [" + absolutePath + "]");
            // output screenshot name
            if (System.getenv("BUILD_URL") != null) {
                scenario.write("Taking screenshot: " + System.getenv("BUILD_URL") + "artifact/target/test-classes/" + filename);
            }
        }

        return screenshotBytes;
    }


    protected WebElement getUsernameInputFieldElement() {
        By expression = By.id("visiblej_username");
        WebElement element = findVisibleElement(expression);
        if (element == null) {
            By expression1 = By.id("j_username");
            element = findVisibleElement(expression1);
            Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + " or " + expression1 + ".", element);
        }
        element.clear();
        return element;
    }

    protected WebElement getPasswordInputFieldElement() {
        By expression = By.id("visiblej_password");
        WebElement element = findVisibleElement(expression);
        if (element == null) {
            By expression1 = By.id("j_password");
            element = findVisibleElement(expression1);
            Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + " or " + expression1 + ".", element);
        }
        element.clear();
        return element;
    }


    protected boolean isLoginSkip() {
        // check if user is already logged in, when doing tests in series
        // constant close browser/open browser/login is not neccessary
        // see setting KEEP_BROWSER_OPEN_BETWEEN_TESTS
        try {
            SeleniumDriverHandler.setTimeout(0, TimeUnit.SECONDS);
            List<WebElement> jPassInput = DRIVER.findElements(By.className("login_form"));
            boolean isLoginForm = !jPassInput.isEmpty();
            if (!isLoginForm && Settings.isBrowserOpenBetweenTests()) {
                return true;
            }
        } finally {
            SeleniumDriverHandler.restoreDefaultTimeout();
        }
        return false;
    }

    /**
     * Tries to find _visible_ element with given expression, returns null if not found
     *
     * @return the found visible element, null if not found
     */
    protected WebElement findVisibleElement(By expression) {
        List<WebElement> elements = DRIVER.findElements(expression);
        WebElement visibleElement = null;
        for (WebElement curr : elements) {
            if (curr.isDisplayed() && curr.isEnabled()) {
                visibleElement = curr;
                break;
            }
        }
        return visibleElement;
    }

    protected WebElement findVisibleElementWithIndex(List<WebElement> elements, int index) {
        WebElement visibleElement = null;
        int i = 0;
        for (WebElement curr : elements) {
            if (curr.isDisplayed() && curr.isEnabled()) {
                visibleElement = curr;
                if (i == index) {
                    break;
                }
                i++;
            }
        }
        return visibleElement;
    }


    protected void checkSectionFieldsContainsValue(String section, Iterable<String> expectedFields) {

        logStopWatchDebug("checkSectionFieldsContainsValue");


        for (String field : expectedFields) {
            String findInputFieldsXpath =
                    "//div[contains(@class,'form_section')]/descendant::*[(contains(@id,'section_title') or contains(@class,'panel-title')) and contains(" + xpathLowercase(section) + ",\"" + section.toLowerCase() + "\")]" +
                            "/following::label[text()[" + xpathLowercase(field) + " = \"" + field.toLowerCase() + "\"]]/parent::*/following-sibling::*//*[self::input or self::textarea]";
            List<WebElement> elements = DRIVER.findElements(By.xpath(findInputFieldsXpath));
            Assert.assertTrue("No value found for input field [" + field + "].\nXPath = [" + findInputFieldsXpath + "]", elements.size() > 0);
            for (WebElement el : elements) {
                String value = el.getAttribute("value").trim();
                logStopWatchDebug("checkSectionFieldsContainsValue", true);
                Assert.assertFalse("No value found for input field [" + field + "]", Strings.isNullOrEmpty(value));
            }
        }
    }

    protected void checkDetailSectionFieldsContainsValue(String section, Iterable<String> expectedFields) {

        logStopWatchDebug("checkDetailSectionFieldsContainsValue");


        for (String field : expectedFields) {
            // Xpath is searching for text values first, then it checks for input value (nickname) and lastly it checks if checkboxes are present.
            String findFieldsXpath = "//div[contains(@class,'column_half_container')]/div[contains(@class, 'detail-section')]/h3[contains(" + xpathLowercase(section) + ", \"" + section.toLowerCase() + "\")]/following-sibling::*"
                    + "//div[contains(@class, 'label_wrapper') and contains(" + xpathLowercase(field) + ","
                    + " \"" + field.toLowerCase() + "\")]/parent::*//div[contains(@class, 'text_value_wrapper')] | "
                    + "//div[contains(@class,'column_half_container')]/div[contains(@class, 'detail-section')]/h3[contains(" + xpathLowercase(section) + ", \"" + section.toLowerCase() + "\")]"
                    + "/following-sibling::*//div[contains(@class, 'label_wrapper') and contains(" + xpathLowercase(field) + ", \"" + field.toLowerCase() + "\")]"
                    + "/parent::*//div[contains(@class, 'input_element_wrapper')]/input[@type!='hidden'] | "
                    + "//div[contains(@class,'column_half_container')]/div[contains(@class, 'detail-section')]/h3[contains(" + xpathLowercase(section) + ", \"" + section.toLowerCase() + "\")]"
                    + "/following-sibling::*//div[contains(@class, 'label_wrapper') and contains(" + xpathLowercase(field) + ", \"" + field.toLowerCase() + "\")]"
                    + "/ancestor::div[contains(@class, 'form-group')]/div[contains(@class, 'checkbox_wrapper')]/label";
            List<WebElement> elements = DRIVER.findElements(By.xpath(findFieldsXpath));
            logStopWatchDebug("checkDetailSectionFieldsContainsValue", true);
            Assert.assertTrue("Field [" + field + "] not found.\nXPath = [" + findFieldsXpath + "]", elements.size() > 0);
        }
    }


    protected void checkDetailsSectionFieldValue(String section, String field, String expectedValue, String subsection) {
        field = field.trim();
        expectedValue = parseDate(expectedValue.trim());
        // Value of text must match || input value must match (for checkboxes use "Is checkbox XYZ checked")
        String sectionPart = "";
        if (!section.isEmpty()) {
            sectionPart = "[contains(@class, 'detail-section') and h3[" + xpathLowercase(section) + " = \"" + section.toLowerCase() + "\"]]";
        }
        String findInputFieldXpath;
        if (subsection.isEmpty()) {
            findInputFieldXpath = "//div" + sectionPart
                    + "//label[" + xpathLowercase(field) + " = \"" + field.toLowerCase() + "\"]/ancestor::div[contains(@class,'form-group')]"
                    + "/div[contains(@class,'wrapper')]/p | "
                    + "//div" + sectionPart
                    + "//label[" + xpathLowercase(field) + " = \"" + field.toLowerCase() + "\"]/ancestor::div[contains(@class,'form-group')]"
                    + "/div[contains(@class,'wrapper')]/input";
        } else {
            findInputFieldXpath = "//div[contains(@class,'detail-subsection') and child::div[" + xpathLowercase(subsection) + " = \"" + subsection.toLowerCase() + "\"]]"
                    + "/div[div/label[" + xpathLowercase(field) + " = \"" + field.toLowerCase() + "\"]]"
                    + "/div[contains(@class,'wrapper')]/p | "
                    + "//div[contains(@class,'detail-subsection') and child::div[" + xpathLowercase(subsection) + " = \"" + subsection.toLowerCase() + "\"]]"
                    + "/div[div/label[" + xpathLowercase(field) + " = \"" + field.toLowerCase() + "\"]]"
                    + "/div[contains(@class,'wrapper')]/input";
        }

        List<WebElement> elements = DRIVER.findElements(By.xpath(findInputFieldXpath));

        Assert.assertTrue("Field [" + field + "] not found.\nXPath = [" + findInputFieldXpath + "]", elements.size() > 0);
        Assert.assertTrue("More than one field with name [" + field + "] was found.\n"
                + "XPath = [" + findInputFieldXpath + "]", elements.size() == 1);

        WebElement element = elements.get(0);

        String value = element.getAttribute("value") != null ? element.getAttribute("value") : element.getText();
        if (value == null) {
            Assert.fail("Field [" + field + "] has no value.");
        }
        Assert.assertEquals(expectedValue, value);
    }

    protected void checkNormalSectionFieldValue(String section, String field, String expectedValue) {
        // TODO: does not work for account numbers (cause they are in div text)
        String findInputFieldXpath =
                "//div[contains(@class,'form_section')]/descendant::*[(contains(@id,'section_title') or contains(@class,'panel-title')) and contains(" + xpathLowercase(section) + " , \"" + section.toLowerCase() + "\")]" +
                        "/following::label[text()[" + xpathLowercase(field) + " = \"" + field.toLowerCase() + "\"]]/parent::*/following-sibling::*//input[not(contains(@class,'tt-hint'))]";
        try {
            WebElement element = DRIVER.findElement(By.xpath(findInputFieldXpath));
            Assert.assertEquals(expectedValue, element.getAttribute("value"));
        } catch (TimeoutException e) {
            Assert.fail("Field not found.\nXPath = [" + findInputFieldXpath + "]");
        }
    }


    protected void fillEinvoiceItemValue(WebElement row, String field, String value) {
        final WebElement element = row.findElement(By.className(field)).findElement(By.tagName("input"));
        deleteElementValue(element);
        element.sendKeys(value);
    }

    protected void selectEinvoiceItemDropdownValue(final WebElement row, final String dropdownName, final String value) {
        List<WebElement> dropdowns = row.findElements(By.className(dropdownName));
        if (dropdowns.size() != 1) {
            String message = "There has to be exactly one dropdown with name [" + dropdownName + "].\n"
                    + "Number of found dropdowns: [" + dropdowns.size() + "].\n";
            Assert.fail(message);
        }

        clickOnElementWithScrolling(dropdowns.get(0));
        waitForPageToLoad();
        List<WebElement> elements = DRIVER.findElements(
                By.xpath("//span/ul[contains(@class, 'select2-results__options')]" + "/li[text()[" + xpathLowercase(value) + "=\"" + value + "\"]]"));

        if (elements.size() != 1) {
            String message =
                    "There has to be exactly one element with name [" + value + "] in dropdown [" + dropdownName + "].\n"
                            + "Number of found elements: [" + elements.size() + "].\n";
            Assert.fail(message);
        }

        WebElement element = elements.get(0);
        if (element.isDisplayed() && element.isEnabled()) {
            clickOnElementWithScrolling(element);
        }
    }


    /**
     * Modal window is kind of slow to (dis)appear (animation...)
     * Invoke this method if you want to wait for it to completely (dis)appear.
     * If there is no modal in process of closing/opening, this functions will do nothing
     */
    protected void waitForModalIfExists() {
        boolean backdropExists;
        boolean modalVisible;
        do {
            backdropExists = (boolean) ((JavascriptExecutor) DRIVER).executeScript("return window.jQuery != undefined && $('div.modal-backdrop').length > 0");
            modalVisible = (boolean) ((JavascriptExecutor) DRIVER).executeScript("return window.jQuery != undefined && $('div.modal.fade.in').length > 0");
        } while (backdropExists && !modalVisible);
    }


    protected void checkTableContents(String row, String column, String text) {
        waitForPageToLoad();
        WebElement element = getElementInDynamicTable("//tbody/tr[" + row + "]/td[" + column + "]");
        String contents = element.getText();
        Assert.assertEquals(text.toUpperCase(), contents.trim().toUpperCase());
    }


    /**
     * Since we have dynamic tables, we must scroll a couple of times until we find an element...
     */
    protected WebElement getElementInDynamicTable(String xpath) {
        //TODO: make this configurable in settings?
        // or maybe, check if all rows are loaded...
        int repeat = 10;
        while (true) {
            try {
                // set the timeout for element not found to 1 second for faster execution...
                SeleniumDriverHandler.setTimeout(1, TimeUnit.SECONDS);
                return DRIVER.findElement(By.xpath(xpath));
            } catch (Exception e) {
                if (--repeat <= 0) {
                    throw e;
                }
                // find last element and scroll to it, so more elements load
                int size = DRIVER.findElements(By.tagName("tr")).size();
                if (size <= 0) {
                    Assert.fail("There are no rows in the table!");
                }
                int lastDisplayedRow = size - 1;
                Actions actions = new Actions(DRIVER);
                actions.moveToElement(DRIVER.findElement(By.xpath("//tr[" + lastDisplayedRow + "]"))).perform();
            } finally {
                // set the timeout back to the predefined one
                SeleniumDriverHandler.restoreDefaultTimeout();
            }
        }

    }

    /**
     * When click target might need scroll to get to it, use this method for clicking
     * <p>
     * The problem is that when Selenium scrolls to the element, it puts it on top,
     * where the "sticky header" covers it. This method tries the click and if it cannot click,
     * it scrolls the window down a bit and tries again...
     */
    protected void clickOnElementWithScrolling(WebElement element) {

        List<String> bounds = getBoundedRectangleOfElement(element);
        Long totalInnerPageHeight = getViewPortHeight();
        JavascriptExecutor je = (JavascriptExecutor) DRIVER;
        je.executeScript("window.scrollTo(0, " + (Integer.parseInt(bounds.get(1)) - (totalInnerPageHeight / 2)) + ");");

        try {
            element.click();
        } catch (Exception e) {
            if (e.getMessage().contains("is not clickable")) {
                JavascriptExecutor js = (JavascriptExecutor) DRIVER;
                // if the element is on top (the floating header covers it)
                js.executeScript("window.scrollBy(0,-100)");
                element.click();
            } else {
                throw e;
            }
        }
    }


    protected void checkButtons(String input, String xpath) {
        waitForPageToLoad();
        List<String> requestedButtons = new ArrayList<>();
        for (String s : input.split(",")) {
            String trimmed = s.trim().toUpperCase();
            if (!"".equals(trimmed)) {
                requestedButtons.add(s.trim().toUpperCase());
            }
        }

        List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));

        List<String> foundButtons = new ArrayList<>();
        for (WebElement element : elements) {
            WebElement parent = element.findElement(By.xpath(".."));
            if (parent != null && parent.isDisplayed()) {
                foundButtons.add(element.getAttribute("innerHTML").trim().toUpperCase());
            }
        }

        StringBuilder requestedButtonsReport = new StringBuilder();
        for (String label : requestedButtons) {
            if (requestedButtonsReport.length() > 0) {
                requestedButtonsReport.append(", ");
            }
            requestedButtonsReport.append(label);
        }
        StringBuilder foundButtonsReport = new StringBuilder();
        for (String label : foundButtons) {
            if (foundButtonsReport.length() > 0) {
                foundButtonsReport.append(", ");
            }
            foundButtonsReport.append(label);
        }

        Assert.assertTrue("Number of requested buttons does not match number of found buttons!\n"
                        + "Requested: [" + requestedButtonsReport + "]\n"
                        + "Found: [" + foundButtonsReport + "]\n"
                        + "XPath = [" + xpath + "]",
                requestedButtons.size() == foundButtons.size());

        Assert.assertTrue("Requested buttons do not match found buttons!\n"
                        + "Requested: [" + requestedButtonsReport + "]\n"
                        + "Found: [" + foundButtonsReport + "]\n"
                        + "XPath = [" + xpath + "]",
                isArrayTheSame(requestedButtons, foundButtons));
    }

    protected boolean isArrayTheSame(List<String> requestedButtons, List<String> foundButtons) {
        return requestedButtons.containsAll(foundButtons)
                && foundButtons.containsAll(requestedButtons);
    }

    protected void waitForPageToLoad() {

        // wait for jQuery Async to finish
        ExpectedCondition<Boolean> jQueryLoad = (WebDriver driverLambda) -> {
            final JavascriptExecutor driverLambdaJSExecutor = (JavascriptExecutor) driverLambda;
            if (driverLambdaJSExecutor == null) {
                return false;
            }
            return (Boolean) driverLambdaJSExecutor.executeScript("return (window.jQuery != null) && (jQuery.active === 0);");
        };

        // wait for Javascript/DOM to load
        ExpectedCondition<Boolean> jsLoad = driverLambda -> {
            final JavascriptExecutor driverLambdaJSExecutor = (JavascriptExecutor) driverLambda;
            if (driverLambdaJSExecutor == null) {
                return false;
            }
            String domStatus = (String) driverLambdaJSExecutor.executeScript("return document.readyState");
            return "complete".equals(domStatus);
        };

        try {
            @SuppressWarnings("unused")
            boolean b = WAIT.until(jQueryLoad) && WAIT.until(jsLoad);
        } catch (Exception e) {
            Assert.fail("Failed to execute javascript in browser: " + e.getMessage());
        }

        if (CLICK_DELAY > 0) {
            try {
                Thread.sleep(CLICK_DELAY);
            } catch (InterruptedException e) {
                throw new RuntimeException("There was a problem with click delay! Thread.sleep() returned InterruptedException.");
            }
        }
    }

    /**
     * Checks if text contains {TODAY+DAYS} or {TODAY-DAYS} and returns the calculated date string.
     * If not, returns unmodified text.
     *
     * @param text the string to parse
     * @return the calculated date (if token present) or unmodified string
     */
    protected String parseDate(String text) {
        if (text.contains("{TODAY")) {
            DateTime dt = null;
            DateTimeFormatter fmt = null;
            int endIndex = text.indexOf("}");
            int strtIndex = text.indexOf("{TODAY");
            try {
                int offset = 0;
                if (endIndex > (strtIndex + 6)) {
                    offset = Integer.parseInt(text.substring(strtIndex + 6, endIndex));
                }
                dt = new DateTime().withFieldAdded(DurationFieldType.days(), offset);
                fmt = DateTimeFormat.forPattern(Settings.getDateFormatPattern());
            } catch (NumberFormatException e) {
                Assert.fail("Problem parsing the date macro [" + text + "]: " + e.getMessage());
            }
            return text.substring(0, strtIndex) + fmt.print(dt) + text.substring(endIndex + 1);
        } else {
            return text;
        }
    }

    protected WebElement findVisibleElement(List<WebElement> elementList) throws Exception {
        for (WebElement curr : elementList) {
            if (curr.getSize().getHeight() > 0) {
                return curr;
            }
        }
        return null;
    }


    protected void verifyHttpStatus(HttpResponse<JsonNode> jsonResponse) throws Exception {
        if (HttpStatus.SC_OK != jsonResponse.getStatus()) {

            // ugly http code description - we want more if possible :)
            String message = jsonResponse.getStatusText();

            // if service gave us some more descriptive error message, just show it
            if (StringUtils.isNotEmpty(jsonResponse.getBody().getObject().getString("message"))) {
                message = jsonResponse.getBody().getObject().getString("message");
            }

            throw new Exception(message);
        }
    }


    private void readDocSpecificData() {
        try {
            final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("documentSpecificData.xml");
            final JAXBContext jaxbContext = JAXBContext.newInstance(DocSpecificData.class);
            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            final DocSpecificData docSpecificData = (DocSpecificData) jaxbUnmarshaller.unmarshal(
                    inputStream
            );
            docSpecificDataWrapper = new DocSpecificDataWrapper(docSpecificData);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    protected Pair<List<String>, List<Map<String, String>>> convertDataTableToListOfMaps(final DataTable dataTable) {
        if (dataTable == null || dataTable.getGherkinRows().size() < 2) {
            throw new IllegalArgumentException("DataTable does not have items present. 2 rows are required! (headerRow, dataRow)");
        }
        final List<Map<String, String>> listOfMaps = new ArrayList<>();
        final List<String> headerCells = new ArrayList<>();
        for (DataTableRow row : dataTable.getGherkinRows()) {
            if (CollectionUtils.isEmpty(headerCells)) {
                headerCells.addAll(row.getCells());
                continue;
            }
            final List<String> cells = row.getCells();
            final Map<String, String> rowMap = new HashMap<>();
            for (int i = 0; i < cells.size(); i++) {
                rowMap.put(headerCells.get(i), cells.get(i));
            }
            listOfMaps.add(rowMap);
        }

        return MutablePair.of(headerCells, listOfMaps);
    }

    protected void deleteElementValue(WebElement element) {
        // clear the field if not empty...
        while (!Strings.isNullOrEmpty(element.getAttribute("value"))) {
            // "\u0008" - is backspace char
            element.sendKeys("\u0008");
        }
    }

    /**
     * Checks if a JQuery modal window/dialog is displayed
     */
    protected boolean isModalDialogOpen() {
        waitForModalIfExists();
        return (Boolean) ((JavascriptExecutor) DRIVER).executeScript("return $('.modal.in').length > 0");
    }

    /**
     * Returns the XPath translate to lower case (for case insensitivity) and normalization (space trimming)
     * (a) Using XPATH translate to lower case in order to enable case-insensitive comparison
     * (b) Using XPATH normalize-space in order to trim whitespaces
     */
    protected String xpathLowercase(String string) {
        return String.format("normalize-space(translate(., \"%s\", \"%s\"))", string.toUpperCase(), string.toLowerCase());
    }

    protected boolean isWindowsOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    /**
     * @return bounding rectangle of web element
     */
    private List<String> getBoundedRectangleOfElement(WebElement we) {
        JavascriptExecutor je = (JavascriptExecutor) SeleniumDriverHandler.DRIVER;
        List<String> bounds = (ArrayList<String>) je.executeScript(
                "var rect = arguments[0].getBoundingClientRect();" +
                        "return [ '' + parseInt(rect.left), '' + parseInt(rect.top), '' + parseInt(rect.width), '' + parseInt(rect.height) ]", we);
        return bounds;
    }

    /**
     * @return webpage viewport height
     */
    private Long getViewPortHeight() {
        JavascriptExecutor je = (JavascriptExecutor) SeleniumDriverHandler.DRIVER;
        return (Long) je.executeScript("return window.innerHeight;");
    }

    /**
     * method to execute after each login
     * takes care of e.g.:
     * -language selection
     */
    protected void afterLogin() {

        //handle automatic language selection based on settings
        if (translationsEnabled) {
            String langToTest = Settings.getTranslationLanguage();

            //check if appropriate language is already selected
            List<WebElement> element = DRIVER.findElements(By.tagName("html"));
            if (!element.isEmpty() && langToTest.equals(element.get(0).getAttribute("lang"))) {
                logDebug("Page already in " + langToTest + " language");
            } else {
                try {
                    //pass selected language in url
                    //this should get ebank-ctx-root/home?lang=something and cause
                    //language change to desired language in browser
                    goToUrl(Settings.getServerUri() + "?lang=" + langToTest);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    public void takeScreenshot() throws Exception {
        createScreenshot(scenario);
    }


    public void waitMilliseconds(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("There was a problem with time delay! Thread.sleep() returned InterruptedException.");
        }
    }


    public void launchBrowser(String nexusPersonal) throws Exception {
        logStopWatchDebug("launchBrowser");


        boolean isDriverAlreadySet = false;
        if (SeleniumDriverHandler.isDriverInitialized()) {
            if (!Settings.isBrowserOpenBetweenTests()) {
                SeleniumDriverHandler.initializeDriver();
            } else {
                isDriverAlreadySet = true;
            }
        } else {
            SeleniumDriverHandler.initializeDriver();
        }

        if (!isDriverAlreadySet) {
            if (!Strings.isNullOrEmpty(nexusPersonal)) {
                boolean withoutCertificate = nexusPersonal.trim().equals("without certificate");

                if (withoutCertificate) {
                    new RobotThread(CertUsage.WITHOUT_CERT);
                } else {
                    // set up the robot for entering pin in Nexus Personal in a new thread...
                    new RobotThread(CertUsage.WITH_CERT);
                }
            }
        }

        logStopWatchDebug("launchBrowser", true);

        //always go to bank default page
        try {
            logStopWatchDebug("Open URL: " + Settings.getServerUri());
            SeleniumDriverHandler.gotoDefaultServerURI();
            logStopWatchDebug("Open URL: " + Settings.getServerUri(), true);
        } catch (Exception e) {
            createScreenshot(null);
            throw new IllegalStateException("Failed to load the page [" + Settings.getServerUri() + "]: " + e.getMessage());
        }
        waitForPageToLoad();


    }


    public void goToUrl(String url) throws Exception {
        try {
            waitForPageToLoad();
            if (url != null && !"".equals(url)) {
                if (url.startsWith("{MB_")) {
                    url = Settings.getMultiBankUri(url);
                }
                SeleniumDriverHandler.gotoURI(url);
            } else {
                throw new IllegalStateException("Not a valid url [" + url + "]: ");
            }
        } catch (Exception e) {
            createScreenshot(getScenario());
            throw new IllegalStateException("Failed to load the page [" + Settings.getServerUri() + "]: " + e.getMessage());
        }
        waitForPageToLoad();
    }


    public void enterPin() throws Exception {
        RobotThread.robotSelectPlugoutAndEnterPin(new Robot());
    }


    public void signWithOTP(String otp, String enter) {
        waitForPageToLoad();
        waitForModalIfExists();

        By expression = By.xpath("//input[@id='signatureToken']");
        WebElement element = findVisibleElement(expression);
        if (element == null) {
            expression = By.xpath("//input[@name='signatureToken']");
            element = findVisibleElement(expression);
        }
        Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + ".", element);
        element.sendKeys(otp);

        if (!enter.isEmpty()) {
            element.sendKeys(Keys.ENTER);
        }
    }


    public void signDocument() {
        waitForPageToLoad();
        //waitForModalIfExists();


        // try to detect sign method
        // check modal window or nexus plugout window each 500ms for 20sec

        // this is for modal window
        By expression = By.xpath("//input[@id='signatureToken']");

        for (int i = 0; i < 40; i++) {

            WebElement element = findVisibleElement(expression);
            // is it a signaure token sign
            if (element != null) {
                element.sendKeys(credentials.get("password").toString());
                element.sendKeys(Keys.ENTER);
                break;
            } else {
                // check for nexus plugout pin entry window
                if (NexusPersonalHandler.isPersonalPlugoutWinOpen()) {
                    //enter pin
                    try {
                        Robot robot = new Robot();
                        String certPIN = "123456";
                        for (char c : certPIN.toCharArray()) {
                            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
                            robot.keyPress(keyCode);
                            robot.keyRelease(c);
                        }

                        robot.keyPress(KeyEvent.VK_ENTER);
                        robot.keyRelease(KeyEvent.VK_ENTER);

                    } catch (AWTException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;

                }


            }

            // if not modal or nexus plugout window 

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Error while checking for sign method");
            }


        }


        // going once
        /*
        WebElement element = findVisibleElement(expression);
        if (element == null) {
            
            // going twice
            expression = By.xpath("//input[@name='signatureToken']");
            element = findVisibleElement(expression);
       }
       
        
       // is it a signaure token sign
       if(element != null) {
           element.sendKeys(credentials.get("password").toString());
           element.sendKeys(Keys.ENTER);
       }else {           
           Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + ".", element);
       }
       */


        waitForPageToLoad();

    }


    public void enterPin2() throws Exception {
        RobotThread.robotEnterPin(new Robot());
    }


    public void loginWithPassword(String pass) {

        logStopWatchDebug("loginWithPassword");


        if (isLoginSkip()) {
            return;
        }

        WebElement element = DRIVER.findElement(By.xpath("//*[@data-target='#certificate']"));
        element.click();


        element = getPasswordInputFieldElement();
        element.sendKeys(pass);
        By expression = By.className("button_submit");
        WebElement button_submit = findVisibleElement(expression);
        Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + ".", button_submit);
        button_submit.click();

        logStopWatchDebug("loginWithPassword", true);

        afterLogin();
    }


    public void loginWithUsernamePassword(String user, String pass) {

        logStopWatchDebug("loginWithUsernamePassword");

        if (isLoginSkip()) return;

        //waitForPageToLoad();
        WebElement element = DRIVER.findElement(By.xpath("//*[@data-target='#password']"));
        element.click();

        //waitForPageToLoad();

        element = getUsernameInputFieldElement();
        element.sendKeys(user);
        element = getPasswordInputFieldElement();
        element.sendKeys(pass);
        By expression = By.className("button_submit");
        WebElement button_submit = findVisibleElement(expression);
        Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + ".", button_submit);
        button_submit.click();

        logStopWatchDebug("loginWithUsernamePassword", true);
    }


    public void loginWithSMSOTP(String user, String pass) {

        logStopWatchDebug("loginWithSMSOTP");
        if (isLoginSkip()) return;

        //waitForPageToLoad();

        WebElement element = DRIVER.findElement(By.xpath("//*[@data-target='#smsotp']"));
        element.click();

        //waitForPageToLoad();

        element = getUsernameInputFieldElement();
        element.sendKeys(user);
        element = getPasswordInputFieldElement();
        element.sendKeys(pass);
        By expression = By.className("button_submit");
        WebElement button_submit = findVisibleElement(expression);
        Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + ".", button_submit);
        button_submit.click();

        logStopWatchDebug("loginWithSMSOTP", true);
    }


    public void enterSMSOTP(String otp) {
        waitForPageToLoad();
        By expression = By.id("visiblej_otp");
        WebElement element = findVisibleElement(expression);
        if (element == null) {
            By expression1 = By.id("j_otp");
            element = findVisibleElement(expression1);
            Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + " or " + expression1 + ".", element);
        }
        element.sendKeys(otp);
        expression = By.className("button_submit");
        WebElement button_submit = findVisibleElement(expression);
        Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + ".", button_submit);
        button_submit.click();
    }


    public void clickOn(String indexStr, String buttonOrLinkText) throws Exception {
        logStopWatchDebug("Click on " + (indexStr != null ? indexStr + " " : "") + buttonOrLinkText);

        int index = 0;
        if (indexStr != null) {
            switch (indexStr.trim()) {
                case "first":
                    index = 0;
                    break;
                case "second":
                    index = 1;
                    break;
                case "third":
                    index = 2;
                    break;
            }
        }
        int count = 0;
        while (true) {
            try {
                count++;

                // exceptional cases

                // check if english is already selected
                // prevent click if it is
                // optimization of test execution to skip unnecessary page reloads
                if (buttonOrLinkText.equalsIgnoreCase("english")) {
                    if (translationsEnabled) {
                        //if translations are enabled skip language selection in
                        //deprecated test definitions
                        logStopWatchDebug("Click on", true);
                        return;
                    } else {
                        List<WebElement> element = DRIVER.findElements(By.tagName("html"));
                        if (!element.isEmpty() && "en".equals(element.get(0).getAttribute("lang"))) {
                            logDebug("Page already in English language");
                            logStopWatchDebug("Click on", true);
                            return;
                        }
                    }
                } else if (buttonOrLinkText.equalsIgnoreCase("Pay")) {
                    DRIVER.findElement(By.id("buttonPay")).click();
                    logDebug("Special case for pay button");
                    logStopWatchDebug("Click on", true);
                    return;
                } else {

                    waitForPageToLoad();
                }


                // if modal dialog open, start searching from div that contains
                String startXPath = "//";
                if (isModalDialogOpen()) {
                    startXPath = "//div[contains(@class,'modal-content')]//";
                }
                String xpathExpression = startXPath + "button[descendant-or-self::*[text()[" + xpathLowercase(buttonOrLinkText) + "=\"" + buttonOrLinkText.toLowerCase() + "\"]]] | " +
                        startXPath + "a[descendant-or-self::*[text()[" + xpathLowercase(buttonOrLinkText) + "=\"" + buttonOrLinkText.toLowerCase() + "\"]]] | " +
                        startXPath + "th[contains(@class,'sortable')]/div[descendant-or-self::*[text()[" + xpathLowercase(buttonOrLinkText) + "=\"" + buttonOrLinkText.toLowerCase() + "\"]]] | " +
                        startXPath + "div[self::*[text()[" + xpathLowercase(buttonOrLinkText) + "=\"" + buttonOrLinkText.toLowerCase() + "\"]]]";

                waitForElementPresent(By.xpath(xpathExpression), 15);
                try {
                    WAIT.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath(xpathExpression)));
                } catch (TimeoutException e) {
                    Assert.fail("Could not find button or link containing text [" + buttonOrLinkText + "].\nXPath=[" + xpathExpression + "]");
                }
                List<WebElement> elements = DRIVER.findElements(By.xpath(xpathExpression));
                // find the currently visible element
                WebElement visibleElement = findVisibleElementWithIndex(elements, index);

                if (visibleElement == null) {
                    Assert.fail("Could not find visible button or link containing text [" + buttonOrLinkText + "].\nXPath=[" + xpathExpression + "]");
                }

                // a fix for unresponsive top menu (main menu) see JIRA EB-10811 for more info...
                try {
                    SeleniumDriverHandler.setTimeout(0, TimeUnit.SECONDS);
                    DRIVER.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
                    List<WebElement> elementList = visibleElement.findElements(By.xpath("ancestor::ul[@id='header-menu']"));
                    if (elementList.size() > 0) {
                        Thread.sleep(500);
                    }
                } finally {
                    SeleniumDriverHandler.restoreDefaultTimeout();
                }


                clickOnElementWithScrolling(visibleElement);
                waitForModalIfExists();

                logStopWatchDebug("Click on", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void Logout() {
        clickOnMailOrLanguageOrUserIcon("user");

        String xpath = "//*[@class='logoutLink']";
        try {
            WebElement element = DRIVER.findElement(By.xpath(xpath));
            element.click();
        } catch (Exception e) {
            Assert.fail("Could not logout  by clicking on \"" + xpath + "\". Error: " + e.getMessage());
        }
    }


    public void clickOnMailOrLanguageOrUserIcon(String icon) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                boolean isMailIconType = icon.equals("mail");
                boolean isLanguageIconType = icon.equals("language");
                boolean isUserIconType = icon.equals("user");
                // default 2 icons in main page
                int max_size = 2;

                // icon on Login or icons on Main page
                String xpath = "//ul[@id='loginNavbar']/li/a | //ul[@id='header-row-right-menu']/li/a";
                // count icons
                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                // if there are 3 icons in main page
                if (elements.size() == 3) {
                    max_size = 3;
                }
                Assert.assertTrue("Mail or (language) or user icon not present on page.\nXPath = [" + xpath + "]", 1 <= elements.size() && elements.size() <= max_size);
                // on login is only language icon, on main page is mail icon first, (language) if exist second icon, user third icon

                if (isLanguageIconType) {
                    xpath = "//*[@id='languageSelector']/i";
                }
                if (isMailIconType) {
                    xpath = "//*[@id='header-row-right-menu']//*[@class='fa fa-envelope']";
                }
                if (isUserIconType) {
                    xpath = "//*[@id='header-row-right-menu']//*[@class='fa fa-user']";
                }
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpath));
                    element.click();
                } catch (Exception e) {
                    Assert.fail("Could not click on home or language or bank icon. Error: " + e.getMessage() + " \nXPath = [" + xpath + "]");
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void clickOnHomeOrBankIcon(String home) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                waitForPageToLoad();
                boolean isHomeIconType = home.equals("home");
                String xpath;
                if (isHomeIconType) {
                    xpath = "//*[@id='common_label_home']/i";
                } else {
                    xpath = "//*[@class='navbar-header']/a";
                }
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpath));
                    element.click();
                } catch (Exception e) {
                    Assert.fail("Could not click on home or bank icon. Error: " + e.getMessage() + " \nXPath = [" + xpath + "]");
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkNoDataAvailable() {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String xpath = "//*[@id='table_container']/div[@class = 'no_data']";

                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                Assert.assertTrue("Text: \"No data available...\" was not found!\nxPath = [" + xpath + "]",
                        elements.size() > 0);

                String text = elements.get(0).getText().trim();
                Assert.assertTrue("Wrong text: [" + text + "]! It must be \"No data available...\" \nxPath = [" + xpath + "]",
                        text.equals("No data available..."));
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void enterTextInputField(String index, String label, String text, String pressEnterKey) throws Exception {

        logStopWatchDebug("enterTextInputField");

        int count = 0;
        while (true) {
            try {
                count++;

                waitForPageToLoad();


                text = parseDate(text);
                int idx = 1;
                if (!Strings.isNullOrEmpty(index)) {
                    idx = Integer.parseInt(index.substring(0, 1));
                }
                // Using XPATH translate in order to simulate case insensitive search
                // Using XPATH normalize-space in order to trim whitespaces
                String xpath = "//label/descendant-or-self::*[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]" +
                        "/following::*[(self::input or self::textarea) and @name and not(parent::*[contains(@class,'hide')])][" + idx + "]";

                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                WebElement element = null;
                for (WebElement curr : elements) {
                    if (curr.isDisplayed() && curr.isEnabled() && curr.getSize().getHeight() > 0) {
                        element = curr;
                    }
                }
                Assert.assertNotNull("Could not find element with the label [" + label + "]. \nXPath = [" + xpath + "]", element);

                int i = 1;
                // clear the field if not empty...
                while (!Strings.isNullOrEmpty(element.getAttribute("value"))) {
                    // "\u0008" - is backspace char
                    element.sendKeys("\u0008");
                    if (i++ > 200) {
                        Assert.fail("Could not clear the element with the label [" + label + "]. \nXPath = [" + xpath + "]. Text still present: " + element.getAttribute("value"));
                    }
                }

                element.sendKeys(text);

                if (!Strings.isNullOrEmpty(pressEnterKey)) {
                    element.sendKeys(Keys.ENTER);
                }
                logStopWatchDebug("enterTextInputField", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("enterTextInputField", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void enterAutocompleteField(String index, String label, String text, String entryIndex) throws Exception {

        logStopWatchDebug("enterAutocompleteField");

        int count = 0;
        while (true) {
            try {
                count++;

                waitForPageToLoad();


                text = parseDate(text);
                int idx = 1;
                if (!Strings.isNullOrEmpty(index)) {
                    idx = Integer.parseInt(index.substring(0, 1));
                }
                // Using XPATH translate in order to simulate case insensitive search
                // Using XPATH normalize-space in order to trim whitespaces
                String xpath = "//label/descendant-or-self::*[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]" +
                        "/following::*[(self::input or self::textarea) and @name and not(parent::*[contains(@class,'hide')])][" + idx + "]";

                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                WebElement element = null;
                for (WebElement curr : elements) {
                    if (curr.isDisplayed() && curr.isEnabled() && curr.getSize().getHeight() > 0) {
                        element = curr;
                    }
                }
                Assert.assertNotNull("Could not find element with the label [" + label + "]. \nXPath = [" + xpath + "]", element);

                int i = 1;
                // clear the field if not empty...
                while (!Strings.isNullOrEmpty(element.getAttribute("value"))) {
                    // "\u0008" - is backspace char
                    element.sendKeys("\u0008");
                    if (i++ > 200) {
                        Assert.fail("Could not clear the element with the label [" + label + "]. \nXPath = [" + xpath + "]. Text still present: " + element.getAttribute("value"));
                    }
                }

                // send keys very slowly for autocomplete to pick up
                String fastTextPart = "";
                String slowTextPart = "";
                if (text.length() > 3) {
                    fastTextPart = text.substring(0, text.length() - 3);
                    slowTextPart = text.substring(text.length() - 3, text.length());
                } else {
                    fastTextPart = text;
                }

                element.sendKeys(fastTextPart);
                for (String s : slowTextPart.split("")) {
                    element.sendKeys(s);
                    Thread.sleep(600);
                }


                int entryIdx = 1;
                if (!Strings.isNullOrEmpty(entryIndex)) {
                    entryIdx = Integer.parseInt(entryIndex.substring(0, 1));
                }


                // press down key entryIdx times
                for (int numOfKeypressDown = 0; numOfKeypressDown < entryIdx; numOfKeypressDown++) {
                    element.sendKeys(Keys.ARROW_DOWN);
                    numOfKeypressDown++;
                }

                // press enter
                element.sendKeys(Keys.ENTER);


                logStopWatchDebug("enterAutocompleteField", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("enterAutocompleteField", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void enterTextInputFieldWithName(String index, String name, String text, String pressEnterKey) throws Exception {

        logStopWatchDebug("enterTextInputField");

        int count = 0;
        while (true) {
            try {
                count++;

                waitForPageToLoad();


                text = parseDate(text);
//                int idx = 1;
//                if (!Strings.isNullOrEmpty(index)) {
//                    idx = Integer.parseInt(index.substring(0, 1));
//                }
                // Using XPATH translate in order to simulate case insensitive search
                // Using XPATH normalize-space in order to trim whitespaces
                String xpath = "//input[@name" + "='" + name + "']";

                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                WebElement element = null;
                for (WebElement curr : elements) {
                    if (curr.isDisplayed() && curr.isEnabled() && curr.getSize().getHeight() > 0) {
                        element = curr;
                    }
                }
                Assert.assertNotNull("Could not find element with name [" + name + "]. \nXPath = [" + xpath + "]", element);

                int i = 1;
                // clear the field if not empty...
                while (!Strings.isNullOrEmpty(element.getAttribute("value"))) {
                    // "\u0008" - is backspace char
                    element.sendKeys("\u0008");
                    if (i++ > 200) {
                        Assert.fail("Could not clear the element with name [" + name + "]. \nXPath = [" + xpath + "]. Text still present: " + element.getAttribute("value"));
                    }
                }

                element.sendKeys(text);

                if (!Strings.isNullOrEmpty(pressEnterKey)) {
                    element.sendKeys(Keys.ENTER);
                }
                logStopWatchDebug("enterTextInputField", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("enterTextInputField", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void selectBox(String label, final String selection) throws Exception {

        logStopWatchDebug("selectBox");


        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String findDropdownXpath = "//label[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/following::*[contains(@class,'select_element_wrapper') or contains(@class,'account_selector_container')][1]";
                List<WebElement> elements = DRIVER.findElements(By.xpath(findDropdownXpath));
                Assert.assertTrue("Dropdown with the label [" + label + "] not found!\nXPath = [" + findDropdownXpath + "]", elements.size() != 0);
                for (WebElement curr : elements) {
                    if (curr.isDisplayed() && curr.isEnabled()) {
                        clickOnElementWithScrolling(curr);
                    }
                }
                waitForPageToLoad();
                String selectionNew = parseDate(selection.trim());
                String findDropdownElementXpath = "//li/descendant-or-self::*[" + xpathLowercase(selectionNew) + "=\"" + selectionNew.toLowerCase() + "\"" +
                        " and contains(concat(' ', @class, ' '), 'select2-results__option')]";
                elements = DRIVER.findElements(By.xpath(findDropdownElementXpath));
                Assert.assertTrue("Dropdown with the label + [" + label + "] does not contain value [" + selectionNew + "]!\nXPath = [" + findDropdownElementXpath + "]", elements.size() != 0);
                for (WebElement curr : elements) {
                    if (curr.isDisplayed() && curr.isEnabled()) {
                        clickOnElementWithScrolling(curr);
                    }
                }
                logStopWatchDebug("selectBox", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("selectBox", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkSectionFieldsContainsValue(String section, String fieldsCSV) {
        logStopWatchDebug("checkSectionFieldsContainsValue");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                Iterable<String> expectedFields = SPLITTER_CSV.split(fieldsCSV);
                // Check if we are on the "Details" screen or some "read-only" form screen...
                List<WebElement> elements = DRIVER.findElements(By.className("details-container"));
                section = section.trim();
                if (elements.size() > 0) {
                    checkDetailSectionFieldsContainsValue(section, expectedFields);
                } else {
                    checkSectionFieldsContainsValue(section, expectedFields);
                }
                logStopWatchDebug("checkSectionFieldsContainsValue", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkSectionFieldsContainsValue", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkSectionSubsectionFieldValue(String section, String subsection, String field, String expectedValue) {
        logStopWatchDebug("checkSectionSubsectionFieldValue");

        int count = 0;
        while (true) {
            try {
                count++;
                expectedValue = parseDate(expectedValue.trim());
                waitForPageToLoad();
                section = section.trim();
                field = field.trim();
                List<WebElement> elements = DRIVER.findElements(By.className("details-container"));
                if (elements.size() > 0) {
                    checkDetailsSectionFieldValue(section, field, expectedValue, subsection.trim());
                } else {
                    checkNormalSectionFieldValue(section, field, expectedValue);
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkSectionSubsectionFieldValue", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkSectionFieldValue(String section, String field, String expectedValue) {
        logStopWatchDebug("checkSectionFieldValue");

        int count = 0;
        while (true) {
            try {
                count++;
                checkSectionSubsectionFieldValue(section, "", field, expectedValue);
                logStopWatchDebug("checkSectionFieldValue", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkSectionFieldValue", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkSectionCurrencyFieldValue(String section, String field, String expectedValue) {
        logStopWatchDebug("checkSectionCurrencyFieldValue");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String findInputFieldXpath =
                        "//div[contains(@class,'form_section')]/child::*[contains(@id,'section_title') and contains(.,\"" + section.trim() + "\")]" +
                                "/following::label[contains(.,\"" + field.trim() + "\")]/parent::*/following-sibling::*//span[contains(@id,'currency') and string-length(normalize-space(text())) > 0]";
                WebElement element = DRIVER.findElement(By.xpath(findInputFieldXpath));
                try {
                    Assert.assertEquals(expectedValue, element.getText());
                } catch (TimeoutException e) {
                    Assert.fail("Field not found.\nXPath = [" + findInputFieldXpath + "]");
                }
                logStopWatchDebug("checkSectionCurrencyFieldValue", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkSectionCurrencyFieldValue", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkLinksInSection(String row, String fields) {
        checkLinksInSection("", row, fields);
    }


    public void checkLinksInSection(String section, String row, String fields) {
        logStopWatchDebug("checkLinksInSection");

        int count = 0;
        while (true) {
            try {
                count++;
                List<String> requestedLinks = new ArrayList<>();
                for (String link : fields.split(",")) {
                    requestedLinks.add(link.trim().toLowerCase());
                }

                String sectionString = "";
                if (!Strings.isNullOrEmpty(section)) {
                    sectionString = " and child::div[contains(@class, 'short_table_group_title_container') and "
                            + "text()[" + xpathLowercase(section) + "=\"" + section.toLowerCase() + "\"]]";
                }

                String xpath = "//div[contains(@class,'short_table_group_container')" + sectionString + "]"
                        + "/div[contains(@class, 'short_table_section_container')]["
                        + row + "]//div[contains(@class, 'short_table_link_container')]//a";

                waitForPageToLoad();

                //take into account only links that are currently displayed
                List<WebElement> foundLinks = DRIVER.findElements(By.xpath(xpath))
                        .stream()
                        .filter(WebElement::isDisplayed)
                        .collect(Collectors.toList());

                String foundLinksReport = foundLinks
                        .stream()
                        .map(l -> l.getAttribute("innerHTML").trim())
                        .filter(s -> s != null && !"".equals(s))
                        .collect(Collectors.joining(", "));


                Assert.assertTrue("Number of requested links does not match actual state.\n"
                        + "Requested: [" + fields + "]\n"
                        + "Found: [" + foundLinksReport + "]\n"
                        + "XPath = [" + xpath + "]", foundLinks.size() == requestedLinks.size());

                for (WebElement found : foundLinks) {
                    if (!requestedLinks.contains(found.getAttribute("innerHTML").trim().toLowerCase())) {
                        Assert.fail("Required and found links do not match!\n"
                                + "Requested: [" + fields + "]\n"
                                + "Found: [" + foundLinksReport + "]\n"
                                + "XPath = [" + xpath + "]");
                    }
                }
                logStopWatchDebug("checkLinksInSection", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkLinksInSection", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void toggleSection(String section) {
        logStopWatchDebug("toggleSection");

        int count = 0;
        while (true) {
            try {
                section = parseDate(section);
                count++;
                waitForPageToLoad();
                String xpath = "//a[@data-toggle='collapse']/descendant-or-self::*[" +
                        "text()[normalize-space(translate(.,\"" + section.toUpperCase() + "\",\"" + section.toLowerCase() + "\"))=\"" +
                        section.toLowerCase() + "\"]]";
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpath));
                    element.click();
                } catch (Exception e) {
                    Assert.fail("Could not extend section. Error: " + e.getMessage() + " \nXPath = [" + xpath + "]");
                }
                logStopWatchDebug("toggleSection", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("toggleSection", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void addEinvoiceItem(String itemValues) throws Exception {
        final List<String> separateItemValues = Arrays.stream(itemValues.split("\\|")).map(s -> s.trim().toLowerCase()).collect(Collectors.toList());
        if (separateItemValues.size() != 9) {
            Assert.fail("Not enough parameters were sent in. Remember that all non-empty values must be set (including zeroes)!");
        }
        waitForPageToLoad();
        final WebElement row = DRIVER.findElement(By.className("add_new"));

        fillEinvoiceItemValue(row, "code", separateItemValues.get(0));
        fillEinvoiceItemValue(row, "description", separateItemValues.get(1));
        fillEinvoiceItemValue(row, "deliveryNote", separateItemValues.get(2));
        fillEinvoiceItemValue(row, "purchaseOrder", separateItemValues.get(3));
        fillEinvoiceItemValue(row, "quantity", separateItemValues.get(4));
        selectEinvoiceItemDropdownValue(row, "unitOfMeasure", separateItemValues.get(5));
        fillEinvoiceItemValue(row, "netAmount", separateItemValues.get(6));
        fillEinvoiceItemValue(row, "discount", separateItemValues.get(7));
        selectEinvoiceItemDropdownValue(row, "vat", separateItemValues.get(8));

        clickOn("first", "Add");
    }


    public void selectCheckbox(String action, String label) {
        logStopWatchDebug("selectCheckbox");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                boolean select = action.equals("Select");
                String xpathCheckboxInput = "//label/descendant-or-self::*[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/ancestor::div[contains(@class,'form-group')][1]/descendant::input[@type='checkbox']";
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpathCheckboxInput));
                    if (element.isSelected() && !select || !element.isSelected() && select) {
                        String xpath = "//label/descendant-or-self::*[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/ancestor::div[contains(@class,'form-group')][1]/descendant::div[contains(@class,'btn-checkbox')]";
                        element = DRIVER.findElement(By.xpath(xpath));
                        clickOnElementWithScrolling(element);
                        waitForPageToLoad();
                    }
                } catch (TimeoutException e) {
                    Assert.fail("Could not find checkbox.\nXPath = [" + xpathCheckboxInput + "]");
                }
                logStopWatchDebug("selectCheckbox", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("selectCheckbox", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void isCheckboxSelected(String label, String action) {
        logStopWatchDebug("isCheckboxSelected");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                boolean isChecked = action.equals("selected");
                String xpathCheckboxInput = "//label/descendant-or-self::*[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]" +
                        "/ancestor::div[contains(@class, 'form-group')]" +
                        "/child::div[contains(@class, 'checkbox_wrapper')]/div[contains(@class, 'btn-checkbox')]";
                List<WebElement> elements = DRIVER.findElements(By.xpath(xpathCheckboxInput));
                Assert.assertTrue("Checkbox [" + label + "] not found.\nXPath = [" + xpathCheckboxInput + "]", elements.size() > 0);
                Assert.assertEquals("The checkbox is not in the correct state.", isChecked, elements.get(0).getAttribute("class").contains("active"));
                logStopWatchDebug("isCheckboxSelected", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("isCheckboxSelected", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }

    // Short table (list) commands


    public void checkShortListValue(String row, String label, String value) {
        checkShortListValue("", row, label, value);
    }


    public void checkShortListValue(String section, String row, String label, String value) {
        logStopWatchDebug("checkShortListValue");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String sectionString = "";

                String xpath;
                // short tables have group_container > section_container, retail only has section_container
                String groupTitleContainerXpath = "(//div[contains(@class,'short_table_group_container')])//div[contains(@class, 'short_table_group_title_container')]";
                List<WebElement> containerElements = DRIVER.findElements(By.xpath(groupTitleContainerXpath));
                if (containerElements.size() > 0) {
                    if (!Strings.isNullOrEmpty(section)) {
                        sectionString = "and child::div[contains(@class, 'short_table_group_title_container') and "
                                + "text()[" + xpathLowercase(section) + "=\"" + section.toLowerCase() + "\"]]";
                    }
                    xpath = "(//div[contains(@class,'short_table_group_container')" + sectionString + "]/div[contains(@class, 'short_table_section_container')])["
                            + row + "]//div[contains(@class, 'short_table_info') and text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]";
                } else {
                    xpath = "(//div[contains(@class, 'short_table_section_container')])[" + row + "]//div[contains(@class, 'short_table_info') and text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]";
                }

                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                Assert.assertTrue("Label not found.\nXPath = [" + xpath + "]", elements.size() > 0);
                xpath = "./following-sibling::div[contains(@class,'short_table_element')]";
                List<WebElement> valueElements = elements.get(0).findElements(By.xpath(xpath));
                Assert.assertTrue("Value for label '" + label + "' not found.\nXPath = [" + xpath + "]", valueElements.size() > 0);
                Assert.assertEquals(value, valueElements.get(0).getText());
                logStopWatchDebug("checkShortListValue", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkShortListValue", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkDataModel(String row, String title, String data) {
        checkDataModel("", row, title, data);
    }


    public void checkDataModel(String section, String row, String title, String data) {
        logStopWatchDebug("checkDataModel");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String sectionString = "";
                if (!Strings.isNullOrEmpty(section)) {
                    sectionString = " and child::div[contains(@class, 'short_table_group_title_container') and "
                            + "text()[" + xpathLowercase(section) + "=\"" + section.toLowerCase() + "\"]]";
                }

                String xpath = "(//div[contains(@class,'short_table_group_container')" + sectionString + "]/div[contains(@class, 'short_table_section_container')])["
                        + row + "]";
                WebElement element = DRIVER.findElement(By.xpath(xpath));
                Assert.assertFalse("Specified row was not found!\nXPath = [" + xpath + "]", element == null);
                if (!title.isEmpty()) {
                    String xpathTitle = "./div/div[contains(@class, 'short_table_section_title')]";
                    WebElement titleElement = element.findElement(By.xpath(xpathTitle));
                    if (titleElement == null || !titleElement.isDisplayed() || !titleElement.getText().equalsIgnoreCase(title)) {
                        Assert.fail("Title element was not found or does not match required string.\n"
                                + "XPath = [" + xpathTitle + "]");
                    }
                }

                if (!data.isEmpty()) {
                    String xpathData = "./div[contains(@class, 'short_table_left_group')]/div/div[contains(@class, 'short_table_element')]";
                    List<WebElement> elements = element.findElements(By.xpath(xpathData));
                    List<String> requestedData = new ArrayList<>();
                    for (String line : data.split(",")) {
                        requestedData.add(line.trim().toLowerCase());
                    }
                    List<String> foundData = new ArrayList<>();
                    for (WebElement elt : elements) {
                        foundData.add(elt.getAttribute("innerHTML").trim().toLowerCase());
                    }

                    StringBuilder requestedReport = new StringBuilder();
                    StringBuilder foundReport = new StringBuilder();
                    for (String found : foundData) {
                        if (foundReport.length() > 0) {
                            foundReport.append(", ");
                        }
                        foundReport.append(found);
                    }
                    for (String req : requestedData) {
                        if (requestedReport.length() > 0) {
                            requestedReport.append(", ");
                        }
                        requestedReport.append(req);
                    }

                    Assert.assertTrue("Amount of required lines does not match amount of found lines!\n"
                            + "Requested: [" + requestedReport + "]\n"
                            + "Found: [" + foundReport + "]\n"
                            + "XPath = [" + xpathData + "]", requestedData.size() == foundData.size());

                    boolean everythingOK = true;
                    for (String found : foundData) {
                        if (!requestedData.contains(found)) {
                            everythingOK = false;
                            break;
                        }
                    }

                    Assert.assertTrue("Requested data does not match actual state\n"
                            + "Requested: [" + requestedReport + "]\n"
                            + "Found: [" + foundReport + "]\n"
                            + "XPath = [" + xpathData + "]", everythingOK);
                }
                logStopWatchDebug("checkDataModel", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkDataModel", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void clickShortListAction(String row, String action) {
        logStopWatchDebug("clickShortListAction");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String xpath = "(//div[contains(@class,'short_table_group_container')]/div[contains(@class,'short_table_section_container')])[" + row +
                        "]//div[contains(@class,'short_table_link_container')]//a[text()[" + xpathLowercase(action) + "=\"" + action.toLowerCase() + "\"]]";

                //take into account only elements that are currently displayed
                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath))
                        .stream()
                        .filter(WebElement::isDisplayed)
                        .collect(Collectors.toList());

                Assert.assertTrue("Action not found.\nXPath = [" + xpath + "]", elements.size() > 0);
                clickOnElementWithScrolling(elements.get(0));
                logStopWatchDebug("clickShortListAction", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("clickShortListAction", true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void selectRadioButton(String label) {

        logStopWatchDebug("Select radio button " + label);

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String xpath = "//label/descendant-or-self::*[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]";
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpath));
                    element.click();
                } catch (TimeoutException e) {
                    Assert.fail("Could not find radio button.\nXPath = [" + xpath + "]");
                }
                logStopWatchDebug("Select radio button " + label, true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("Select radio button " + label, true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }


    }


    public void isRadioButtonSelected(String label, String action) {
        isRadioButtonSelected(label, action, "");
    }

    // TODO remove this command and use findVisibleElement in isRadioButtonSelected(String label, String action)
    public void isRadioButtonSelected(String label, String action, String modal) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                boolean isChecked = action.equals("selected");
                String modalXpath = "";
                if (!modal.isEmpty()) {
                    modalXpath = "(//div[contains(@class, 'modal') and div[contains(@class, 'modal-header')]"
                            + "/h4[text()[" + xpathLowercase(modal) + "=\"" + modal.toLowerCase() + "\"]]])"
                            + "/div[contains(@class, 'modal-body')]";
                }
                String active = "";
                if (isChecked) {
                    active = "contains(@class, 'active') and ";
                }

                String xpath = modalXpath + "//label[" + active + "span/text()[" + xpathLowercase(label) + " = \"" + label.toLowerCase() + "\"]]/input";
                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                Assert.assertTrue("Radio button [" + label + "] with argument " + action + " was not found.\nxPath = [" + xpath + "]", elements.size() > 0);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void selectAccount(String account) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String xpath = "//a[@id='main-selector'] | //div[contains(@class,'account_selector_container')]";
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpath));
                    element.click();
                } catch (TimeoutException e) {
                    Assert.fail("Could not find the account selector.\nXPath = [" + xpath + "]");
                }
                waitForPageToLoad();
                xpath = "//ul/li[contains(@class,'account')]//div | //div[contains(@class,'account_wrapper_elements')]//div";
                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                boolean found = false;
                for (WebElement el : elements) {
                    if (el.isDisplayed() && el.getText().toUpperCase().equals(account.toUpperCase())) {
                        found = true;
                        el.click();
                        break;
                    }
                }
                Assert.assertTrue("Account [" + account + "] not found.\nXPath = [" + xpath + "]", found);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void selectClient(String client) {
        //try to skip this step if proper client is already selected
        try {
            //don't wait for element
            SeleniumDriverHandler.setTimeout(0, TimeUnit.SECONDS);

            //check if current selected client matches requested and exit method if it does
            String xpath = "(//div[contains(@class, 'clientIdSelector_wrapper')])[1]//span[contains(@class, 'select2-selection__rendered')]";
            List<WebElement> element = DRIVER.findElements(By.xpath(xpath));
            if (element != null && !element.isEmpty()) {
                if (element.get(0).getText().equalsIgnoreCase(client)) {
                    logDebug("Client already selected, skipping step");
                    return;
                }
            }
        } finally {
            //restore general element wait time
            SeleniumDriverHandler.restoreDefaultTimeout();
        }


        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String xpath = "(//div[contains(@class, 'clientIdSelector_wrapper')])[1]";
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpath));
                    element.click();
                } catch (TimeoutException e) {
                    Assert.fail("Could not find the client selector.\nXPath = [" + xpath + "]");
                }

                //EB-18639 without a hard delay clicking the client was too fast for the page, it seems that drop down was not populated with clients in time
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    logDebug(e.getMessage());
                    throw new RuntimeException("There was a problem with click delay! Thread.sleep() returned InterruptedException.");
                }

                //FIXME this obviously does not wait in all cases, see EB-18639 above
                waitForPageToLoad();
                xpath = "//span[contains(@class,'clientSelector')]//li[@role='treeitem']";
                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                boolean found = false;
                StringBuilder foundElementsReport = new StringBuilder();
                for (WebElement elt : elements) {
                    if (foundElementsReport.length() > 0) {
                        foundElementsReport.append(", ");
                    }
                    foundElementsReport.append(elt.getAttribute("innerHTML"));
                    String innerHTML = elt.getAttribute("innerHTML").replaceAll("\\<[^>]*>", "");
                    if (elt.isDisplayed() && innerHTML.equalsIgnoreCase(client)) {
                        found = true;
                        elt.click();
                        break;
                    }
                }

                Assert.assertTrue("Client [" + client + "] not found.\n"
                        + "Found elements: [" + foundElementsReport + "]\n"
                        + "XPath = [" + xpath + "]", found);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void areTabsPresent(String tabs) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                ArrayList<String> expectedTabs = Lists.newArrayList(SPLITTER_CSV.split(tabs));
                Assert.assertTrue("There must be more than 1 tab present, or the tabs will not be shown.", expectedTabs.size() > 1);

                String tabXpath = "//div[contains(@class, 'tabbable')]/ul/li[contains(@class, 'tab')]/a/div";
                List<WebElement> elements = DRIVER.findElements(By.xpath(tabXpath));

                List<String> requestedTabs = new ArrayList<>();
                for (String s : tabs.split(",")) {
                    requestedTabs.add(s.trim().toUpperCase());
                }

                List<String> foundTabs = new ArrayList<>();
                for (WebElement element : elements) {
                    foundTabs.add(element.getAttribute("innerHTML").trim().toUpperCase());
                }

                StringBuilder requestedTabsReport = new StringBuilder();
                for (String label : requestedTabs) {
                    if (requestedTabsReport.length() > 0) {
                        requestedTabsReport.append(", ");
                    }
                    requestedTabsReport.append(label);
                }

                StringBuilder foundTabsReport = new StringBuilder();
                for (String label : foundTabs) {
                    if (foundTabsReport.length() > 0) {
                        foundTabsReport.append(", ");
                    }
                    foundTabsReport.append(label);
                }

                Assert.assertTrue("Number of requested buttons does not match number of found buttons!\n"
                                + "Requested: [" + requestedTabsReport + "]\n"
                                + "Found: [" + foundTabsReport + "]\n"
                                + "XPath = [" + tabXpath + "]",
                        requestedTabs.size() == foundTabs.size());

                for (String tab : foundTabs) {
                    if (!requestedTabs.contains(tab)) {
                        Assert.fail("Requested buttons do not match found buttons!\n"
                                + "Requested: [" + requestedTabsReport + "]\n"
                                + "Found: [" + foundTabsReport + "]\n"
                                + "XPath = [" + tabXpath + "]");
                    }
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkActionButtonsInTableRow(String rowNumber, String actionsCSV) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                // first, click on the first action button so that actions become visible to selenium...
                WebElement actionElement = getElementInDynamicTable("//table/tbody/tr[" + rowNumber + "]//a[@class='dropdown-toggle']");
                clickOnElementWithScrolling(actionElement);
                ArrayList<String> actionList = Lists.newArrayList(SPLITTER_CSV.split(actionsCSV.toUpperCase()));
                String actionsXpath = "//table//tr[" + rowNumber + "]//ul[@class='dropdown-menu']//a";
                List<WebElement> elements = DRIVER.findElements(By.xpath(actionsXpath));
                Assert.assertEquals("The number of actions doesn't match!", actionList.size(), elements.size());
                for (WebElement curr : elements) {
                    String action = curr.getText().trim();
                    Assert.assertTrue("Action [" + action + "] not found.", actionList.contains(action.toUpperCase()));
                }
                elements.get(0).sendKeys(Keys.ESCAPE);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkFilterFields(String fields) {
        int count = 0;
        while (true) {
            try {
                count++;
                String xpath = "//div[@id = 'filter_accordion']//div[contains(@class, 'label_wrapper')]/label";
                List<String> requestedFields = new ArrayList<>();
                for (String field : fields.split(",")) {
                    requestedFields.add(field.trim().toUpperCase());
                }

                waitForPageToLoad();

                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath));
                List<String> foundFields = new ArrayList<>();
                for (WebElement element : elements) {
                    foundFields.add(element.getText().trim().toUpperCase());
                }

                StringBuilder requestedFieldsReport = new StringBuilder();
                for (String label : requestedFields) {
                    if (requestedFieldsReport.length() > 0) {
                        requestedFieldsReport.append(", ");
                    }
                    requestedFieldsReport.append(label);
                }
                StringBuilder foundFieldsReport = new StringBuilder();
                for (String label : foundFields) {
                    if (foundFieldsReport.length() > 0) {
                        foundFieldsReport.append(", ");
                    }
                    foundFieldsReport.append(label);
                }

                Assert.assertTrue("Number of requested fields does not match number of found fields!\n"
                                + "Requested: [" + requestedFieldsReport + "]\n"
                                + "Found: [" + foundFieldsReport + "]\n"
                                + "XPath = [" + xpath + "]",
                        requestedFields.size() == foundFields.size());

                Assert.assertTrue("Requested fields do not match found fields!\n"
                                + "Requested: [" + requestedFieldsReport + "]\n"
                                + "Found: [" + foundFieldsReport + "]\n"
                                + "XPath = [" + xpath + "]",
                        requestedFields.containsAll(foundFields));
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkMessage(String message) {
        waitForPageToLoad();
        String xpathQuerry = "//h1/descendant-or-self::*[text()[" + xpathLowercase(message) + "=\"" + message.toLowerCase() + "\"]] "
                + " | //div[contains(@class,'fixed_messages') or contains(@data-role,'message_container')]/descendant::*[text()[contains(" + xpathLowercase(message) + ",\"" + message.toLowerCase() + "\")]]";
        try {
            WAIT.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathQuerry)));
        } catch (Exception e) {
            try {
                xpathQuerry = "//*[contains(text(),'" + message + "')]";
                WAIT.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathQuerry)));
            } catch (Exception ex) {
                Assert.fail("Message not displayed.");
            }
        }
    }


    public void checkTableContentsByColumnName(String row, String columnName, String text) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                List<WebElement> elements = DRIVER.findElements(By.xpath("//thead/tr/th"));
                // find the column index
                int i = 1;
                boolean found = false;
                for (WebElement curr : elements) {
                    if (curr.getText().toLowerCase().equals(columnName.toLowerCase())) {
                        found = true;
                        break;
                    }
                    i++;
                }
                if (found) {
                    checkTableContents(row, i + "", parseDate(text.trim()));
                } else {
                    Assert.fail("Column name [" + columnName + "] not found!");
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void clickOnActionInTableRow(String row, String action) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                WebElement actionElement = getElementInDynamicTable("//table/tbody/tr[" + row + "]//a[@class='dropdown-toggle']");
                clickOnElementWithScrolling(actionElement);
                List<WebElement> actions = actionElement.findElements(By.xpath("../ul/li/a/descendant-or-self::*[text()[" + xpathLowercase(action) + "=\"" + action.toLowerCase() + "\"]]"));
                //actions.get(0).click();
                clickOnElementWithScrolling(actions.get(0));
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void selectTableRows(String rowsCSV) throws Exception {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                Iterable<String> rowsIter = SPLITTER_CSV.split(rowsCSV);
                for (String curr : rowsIter) {
                    String xpath = "//tbody/tr[" + curr + "]/td[1]/div/div/div";
                    WebElement element = getElementInDynamicTable(xpath);
                    clickOnElementWithScrolling(element);
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void selectAllTableRows() {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                WebElement selectedAll = DRIVER.findElement(By.xpath("//th[1]/div"));
                selectedAll.click();
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkVisibleColumnsInTable(String columnsCSV) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                ArrayList<String> columns = Lists.newArrayList(SPLITTER_CSV.split(columnsCSV.toUpperCase()));
                // selects all divs that, when trimmed For whitespace, contain some text, so we get the column names
                // if col has hidden="true" in tableConfiguration.xml, the th element will have the class 'hide' present: ignore it
                String xpath = "//table/thead/tr/th[not(contains(@class, 'hide'))]//div[string-length(normalize-space(text())) > 1]";
                List<WebElement> elementCols = DRIVER.findElements(By.xpath(xpath));
                Assert.assertEquals("The number of columns in the table don't match!", columns.size(), elementCols.size());
                int i = 0;
                for (WebElement curr : elementCols) {
                    Assert.assertEquals(columns.get(i), curr.getText().toUpperCase().trim());
                    i++;
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkColumnsSortingEnabled(String columnsCSV) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                Iterable<String> columns = SPLITTER_CSV.split(columnsCSV);
                for (String curr : columns) {
                    String xpath = "//th[contains(@class,'sortable')]/descendant::*[text()[" + xpathLowercase(curr) + "=\"" + curr.toLowerCase() + "\"]]";
                    try {
                        DRIVER.findElement(By.xpath(xpath));
                    } catch (Exception e) {
                        Assert.fail("The column [" + curr + "] is not sortable or not found! Error: " + e.getMessage());
                    }
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkForFormErrorMessage(String message, String label) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String xpath = "//label[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/following::span[contains(@class,'validation_errors')]";
                WebElement element = DRIVER.findElement(By.xpath(xpath));
                Assert.assertTrue("The error message is not visible!\nXPath = [" + xpath + "]", element.isDisplayed());
                String contents = element.getText().trim();
                Assert.assertEquals(message, contents);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkOuterButtonsLongTable(String table, String input) {
        String xPath;
        if (table.equalsIgnoreCase("short")) {
            xPath = "//div[contains(@class, 'short_table_container')]//div[contains(@class, 'upper_buttons_container')]/a/span";
        } else {
            xPath = "//div[contains(@class, 'long_table_container')]/div[contains(@class, 'upper_buttons_container')]/a/span";
        }

        int count = 0;
        while (true) {
            try {
                count++;
                checkButtons(input, xPath);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkButtonLongTable(String table, String input) {
        int count = 0;
        while (true) {
            try {
                count++;
                String xpathTable;
                if (table.equalsIgnoreCase("short")) {
                    xpathTable = "a/span";
                } else {
                    xpathTable = "a[not(boolean(@id)) or @id!='dropdownMenuLink']/span";
                }
                String xpath = "//div[contains(@class, 'filter_actions_row')]//" + xpathTable;

                checkButtons(input, xpath);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void checkButtonsDetailsPage(String input) {
        int count = 0;
        while (true) {
            try {
                count++;
                String xpath = "//div[contains(@class, 'upper_buttons_container')]/*[self::a or self::button]/span";
                checkButtons(input, xpath);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }

    public void uploadFile(String relativePath) throws Exception {
        waitForPageToLoad();
        URL resource = getClass().getResource("/uploadfiles/" + relativePath);
        String absolutePath = resource.getPath();
        if (isWindowsOS()) {
            absolutePath = absolutePath.replaceAll("/", "\\\\").replaceFirst("\\\\", "");
        }
        //Thread.sleep(1000);
        // first, we must make the file input element visible...
        JavascriptExecutor js = (JavascriptExecutor) DRIVER;
        js.executeScript("$('#file-input').removeClass('hidden')");
        // and now, we can proceed with the normal selenium file upload
        WebElement upload = DRIVER.findElement(By.id("file-input"));
        upload.sendKeys(absolutePath);
        WAIT.until(ExpectedConditions.invisibilityOfElementLocated(By.className("blockPage")));
        waitForPageToLoad();
        // TODO: not needed?
        // DRIVER.findElement(By.id("file-input-button")).click();
    }

    void waitForElementPresent(By locator, int timeoutInSeconds) {
        WebDriverWait wait = new WebDriverWait(DRIVER, timeoutInSeconds);
        boolean passed1 = false;
        try {
            WAIT.until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
        } catch (Exception e) {
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                passed1 = true;
            } catch (Exception ignored) {
                if (passed1) {
                    try {
                        wait.until(ExpectedConditions.elementToBeClickable(locator));
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    public boolean isElementPresent(By locator) {
        return DRIVER.findElements(locator).size() > 0;
    }

    String stringLowerCase(String text) {
        return "translate(" + text + ",'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')";
    }

    public void matchSubMenu(String subMenuOptions) {
        waitForPageToLoad();
        String subMenuXPath = "//ul[@class='dropdown-menu inline-menu' and @style]";

        for (String option : subMenuOptions.split(",")
        ) {
            if (!isElementPresent(By.xpath(subMenuXPath + "//span[contains(" + stringLowerCase("text()") + ",'" + option.trim().toLowerCase() + "')]"))) {
                Assert.fail("Sub menu Item: " + option.trim() + " not found");
            }
        }
        logStopWatchDebug("matchSubMenu", true);
        return;

    }

    public void checkBreadcrumbs(String breadcrumbs) {
        int count = 0;

        while (true) {
            try {
                count++;
                String[] requiredCrumbs = breadcrumbs.split("/");
                String xPath = "//ul[contains(@class, 'breadcrumb')]/li[not(contains(@class, 'hidden-md') or contains(@class, 'hidden-lg'))]";
                waitForPageToLoad();
                waitForElementPresent(By.xpath(xPath), 15);
                List<WebElement> elts = DRIVER.findElements(By.xpath(xPath));
                String[] foundElements = new String[elts.size()];
                for (int i = 0; i < elts.size(); i++) {
                    foundElements[i] = elts.get(i).getText().trim();
                    requiredCrumbs[i] = requiredCrumbs[i].trim();
                }

                String reportXpath = ("[" + xPath + "]").toUpperCase();
                String reportRequired = ("[" + String.join(", ", requiredCrumbs) + "]").toUpperCase();
                String reportFound = ("[" + String.join(", ", foundElements) + "]").toUpperCase();
                Assert.assertArrayEquals(
                        "Required breadcrumbs do not match actual state: "
                                + "\nrequired\t: " + reportRequired
                                + "\nfound\t\t: " + reportFound
                                + "\nxPath\t\t: " + reportXpath,
                        requiredCrumbs, foundElements);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void selectByLabel(String selectName, String label) {
        String xpath = "//select[contains(" + stringLowerCase("@name") + ",'" + selectName + "')]";
        Select dropDown = new Select(DRIVER.findElement(By.xpath(xpath)));
        dropDown.selectByVisibleText(label);
        //for now until we find out why this causes problems
        //dropDown.selectByValue(label);
    }

    /**
     * This is used for checking various texts in the page:<br>
     * 1. title of window and modal window<br>
     * 2. message in modal window<br>
     * 3. alert message in modal window<br>
     *
     * @param title
     * @param xPath
     */

    protected boolean isMessageTextPresent(String text) {
        if (DRIVER.findElements(By.xpath("//div[contains(@class,'alert') and contains(.,'" + text + "')]")).size() > 0) {
            return true;
        } else if (DRIVER.findElements(By.xpath("//label[@class='validation_errors' and text()='" + text + "']")).size() > 0) {
            return true;
        } else if (DRIVER.findElements(By.xpath("//*[contains(text(),'" + text + "')]")).size() > 0) {
            return true;
        }
        return false;
    }

    public void checkTableColumns() {
        String xpath = "//div[@data-name='PARTNER_DETAILS']";

        if (isElementPresent(By.xpath(xpath)) && DRIVER.findElement(By.xpath(xpath)).isDisplayed()) {
            logStopWatchDebug("Columns Visible", true);
        } else {
            Assert.fail("Columns Not Visible");
        }
    }

    public boolean isElementVisible(By locator) {
        return DRIVER.findElement(locator).isDisplayed();
    }

    public boolean isElementPresentAndVisible(By locator) {
        return isElementPresent(locator) && isElementVisible(locator);
    }

    public void selectDropDownVisible(String id) {

        String xpath = "//span[contains(@class,'selection') and contains(@id,'" + id + "') and contains(@id,'xt')]";
        if (isElementPresent(By.xpath(xpath)) && isElementVisible(By.xpath(xpath))) {
            logStopWatchDebug("Select " + id + " Visible", true);
        } else {
            Assert.fail("Select " + id + " Not Visible");
        }
    }

    protected void click(By locator) {
        DRIVER.findElement(locator).click();
    }

    public void checkUncheckedBox(String name) {
        String xpath = "//div[not(contains(@class,'active'))]//input[contains(" + stringLowerCase("@name") + ",'" + name + "')]";
        String xpath2 = "//span[contains(" + stringLowerCase("text()") + ",'" + name.toLowerCase() + "')]/preceding-sibling::input[@type='checkbox']";
        if (isElementPresentAndVisible(By.xpath(xpath))) {
            click(By.xpath(xpath));
        } else if (isElementPresentAndVisible(By.xpath(xpath2))) {
            click(By.xpath(xpath2));
        }
    }

    protected void checkText(String title, String xPath) {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                WebElement element = DRIVER.findElement(By.xpath(xPath));
                String reportXpath = "[" + xPath + "]";
                String reportRequired = "[" + title.toUpperCase() + "]";
                String reportFound = "[" + element.getText().toUpperCase() + "]";

                Assert.assertEquals("Required title does not match found one: "
                        + "\nxPath\t\t: " + reportXpath
                        + "\nrequired\t: " + reportRequired
                        + "\nfound\t\t: " + reportFound, title.toLowerCase(), element.getText().toLowerCase().trim());
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }

    public void checkPageTitle(String title) {
        checkText(title, "//div[contains(@class, 'container_breadcrumbs')]/h1");
    }


    public void checkModalWindowTitle(String title) {
        checkText(title, "//div[contains(@class, 'modal') and contains(@class, 'fade') and contains(@style, 'display: block')]//h4[contains(@class, 'modal-title')]");
    }

    public void checkModalWindowAlert(String message) {
        checkText(message, "//div[contains(@class, 'modal') and contains(@class, 'fade') and contains(@style, 'display: block')]//div[contains(@class, 'alert')]");
    }

    public void checkModalwindowMessage(String message) {

        if (isMessageTextPresent(message)) {
            logStopWatchDebug("checkModalwindowMessage", true);
            return;
        } else {
            checkText(message, "//div[contains(@class,'alert')]");
        }
    }

    public void mustVisible(String text) {
        String xpath = "//*[contains(" + stringLowerCase("(text)") + ",'" + text.toLowerCase() + "')]";

        if (isElementPresent(By.xpath(xpath)) && DRIVER.findElement(By.xpath(xpath)).isDisplayed()) {
            logStopWatchDebug("must visible: " + text, true);
        } else if (isElementPresent(By.xpath(xpath + "/..")) && DRIVER.findElement(By.xpath(xpath + "/..")).isDisplayed()) {
            logStopWatchDebug("must visible: " + text, true);
        } else {
            Assert.fail(text + " not visible");
        }

    }

    public void checkTile(String name) {
        String xpath = "//li[contains(@class,'open ')]//span[contains(" + stringLowerCase("text()") + ",'" + name.toLowerCase() + "')]";
        if (isElementPresent(By.xpath(xpath))) {
            logStopWatchDebug("check tile " + name, true);
        } else {
            Assert.fail("Tile " + name + " not selected");
        }
    }

    /**
     * @param notOperator use string not or leave blank
     * @param page        supported login|home
     */
    public void checkIsSpecificPage(String notOperator, String page) {
        int count = 0;
        boolean shouldBe = notOperator == null || "".equalsIgnoreCase(notOperator);
        while (true) {
            try {
                count++;
                // login page check
                if (LANDING_PAGE_LOGIN.equals(page)) {
                    // login page has login_form
                    List<WebElement> jPassInput = DRIVER.findElements(By.className("login_form"));
                    boolean isLoginForm = !jPassInput.isEmpty();
                    if (isLoginForm && !shouldBe) {
                        Assert.fail("Login page found.");
                    }
                    if (!isLoginForm && shouldBe) {
                        Assert.fail("Login page not found.");
                    }
                } else if (LANDING_PAGE_HOME.equals(page)) {
                    List<WebElement> homeBodyTag = DRIVER.findElements(By.xpath("//body[contains(@class,'home')]"));
                    boolean isHomePage = !homeBodyTag.isEmpty();
                    if (isHomePage && !shouldBe) {
                        Assert.fail("Home page found.");
                    }
                    if (!isHomePage && shouldBe) {
                        Assert.fail("Home page not found.");
                    }
                } else if (LANDING_PAGE_MULTIBANK_HOME.equals(page)) {
                    List<WebElement> homeBodyTag = DRIVER.findElements(By.xpath("//body[contains(@class,'home')]"));
                    boolean isHomePage = !homeBodyTag.isEmpty();
                    List<WebElement> specialCssLink = DRIVER.findElements(By.xpath("//link[contains(@href,'redesign_multibank_header.css')]"));
                    boolean isMultiBankCss = !specialCssLink.isEmpty();
                    boolean isMultiBankHome = (shouldBe && isHomePage && isMultiBankCss);
                    if (isMultiBankHome) {
                        Assert.fail("Multibank home page found.");
                    }
                    if (shouldBe) {
                        Assert.fail("Multibank home page not found.");
                    }
                }

                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void setCustomDateRange(String dateFrom, String dateTo) throws Exception {
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                dateFrom = parseDate(dateFrom);
                dateTo = parseDate(dateTo);
                String datePickerXPath = "//li[@id='custom-date-top-container']";
                ((JavascriptExecutor) DRIVER).executeScript("$('input[name=dateFromStr]').val('" + dateFrom + "');");
                ((JavascriptExecutor) DRIVER).executeScript("$('input[name=dateToStr]').val('" + dateTo + "');");
                WebElement datePickerButton = DRIVER.findElement(By.xpath(datePickerXPath + "//a"));
                datePickerButton.click();
                waitForPageToLoad();
                WAIT.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(datePickerXPath + "//div[@id='custom']")));
                WebElement element = findVisibleElement(By.id("custom_filter_apply"));
                element.click();
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    public void getClient(String clientId) throws Exception {
        Validate.notBlank(systemId, "systemId is required!");

        HttpResponse<JsonNode> jsonResponse = Unirest.get(Settings.getDataSetupUri() + "/systems/" + systemId + "/users/" + clientId)
                .header("Content-Type", "application/json")
                .header("certificate", Settings.getCertificate())
                .asJson();

        verifyHttpStatus(jsonResponse);
    }


    public void createClient(String userType, String name, String accountId) throws Exception {
        Validate.notBlank(systemId, "systemId is required!");

        PutUserRequest request = new PutUserRequest();

        // prepare client data
        ClientData clientData = new ClientData();
        clientData.setContractOwnerTypeId(DicContractOwnerType.valueOf(userType).getId());
        clientData.setName(name);
        clientData.setAddress("FE Test address");
        clientData.setCity("FE Test city");
        // CountryId is set on data-setup
        clientData.setCountryId(0);
        clientData.setTaxNumber("12345678");
        clientData.setRegisterNumber("1234567890");
        request.setClientData(clientData);

        // prepare contract data
        ContractData contractData = new ContractData();
        contractData.setAccountId(accountId);
        // Currency is set on data-setup from bank defaultCurrency
        contractData.setCurrencyId(0);
        contractData.setHasEbank(true);
        request.setContractData(contractData);

        // prepare licence data
        LicenceData licenceData = new LicenceData();
        licenceData.setTrusteeId(0);
        licenceData.setLicenceTypeId(DicLicenseType.AUTHORIZER_PP.getId());
        request.setLicenceData(licenceData);

        HttpResponse<JsonNode> jsonResponse = Unirest.put(Settings.getDataSetupUri() + "/systems/" + systemId + "/users")
                .header("Content-Type", "application/json")
                .header("certificate", Settings.getCertificate())
                .body(objectMapper.writeValueAsString(request))
                .asJson();

        verifyHttpStatus(jsonResponse);
    }


    public void removeClient(String contractId) throws Exception {
        Validate.notBlank(systemId, "systemId is required!");

        HttpResponse<JsonNode> jsonResponse = Unirest.delete(Settings.getDataSetupUri() + "/systems/" + systemId + "/contracts/" + contractId + "/users/")
                .header("Content-Type", "application/json")
                .header("certificate", Settings.getCertificate())
                .asJson();

        verifyHttpStatus(jsonResponse);
    }


    public void addLicence(String account, String ignored, Integer trusteeId) throws Throwable {
        Validate.notBlank(systemId, "systemId is required!");

        PutUserRequest request = new PutUserRequest();

        // prepare contract data
        ContractData contractData = new ContractData();
        contractData.setAccountId(account);
        contractData.setContractTypeId(DicContractType.TRANS_ACCOUNT.getId());
        request.setContractData(contractData);

        // prepare licence data
        LicenceData licenceData = new LicenceData();
        licenceData.setTrusteeId(trusteeId == null ? 0 : trusteeId);
        licenceData.setLicenceTypeId(DicLicenseType.AUTHORIZER_PP.getId());
        request.setLicenceData(licenceData);

        final String url = Settings.getDataSetupUri() + "/systems/" + systemId + "/licence/";
        System.out.println("add_account_for_client_with_account: calling url " + url);
        HttpResponse<JsonNode> jsonResponse = Unirest.put(url)
                .header("Content-Type", "application/json")
                .header("certificate", Settings.getCertificate())
                .body(objectMapper.writeValueAsString(request))
                .asJson();

        verifyHttpStatus(jsonResponse);
    }


    public void addPermissionForDocumentTypeOnAccount(String permission, String documentType, String account) throws Throwable {
        Validate.notBlank(systemId, "systemId is required!");

        final DicPermission dicPermission = DicPermission.valueOf(permission);
        final String url = Settings.getDataSetupUri() + "/systems/" + systemId + "/contracts/" + account + "/trustees/0/doc_type/"
                + documentType + "/permissions/" + dicPermission.getId();
        System.out.println("add_permission_for_document_type_on_account: calling url " + url);
        HttpResponse<JsonNode> jsonResponse = Unirest.put(url)
                .header("Content-Type", "application/json")
                .header("certificate", Settings.getCertificate())
                .asJson();

        verifyHttpStatus(jsonResponse);
    }


    public void removePermissionForDocumentTypeOnAccount(String permission, String documentType, String account) throws Throwable {
        Validate.notBlank(systemId, "systemId is required!");

        final DicPermission dicPermission = DicPermission.valueOf(permission);
        final String url = Settings.getDataSetupUri() + "/systems/" + systemId + "/contracts/" + account + "/trustees/0/doc_type/"
                + documentType + "/permissions/" + dicPermission.getId();
        System.out.println("add_permission_for_document_type_on_account: calling url " + url);
        HttpResponse<JsonNode> jsonResponse = Unirest.delete(url)
                .header("Content-Type", "application/json")
                .header("certificate", Settings.getCertificate())
                .asJson();

        verifyHttpStatus(jsonResponse);
    }


    public void createDocument(String document, String account, DataTable dataTable) throws Throwable {
        Validate.notBlank(systemId, "systemId is required!");

        final String url = Settings.getDataSetupUri() + "/systems/" + systemId + "/contracts/" + account + "/docs";
        final DocumentData documentData = new DocumentData();
        final DocumentType documentType = DocumentType.valueOf(document);
        final DataProviderType dataProviderType = docSpecificDataWrapper.getDataProviderType(document);
        final List<String> possibleFields = docSpecificDataWrapper.getDocData(document).stream()
                .map(DocSpecificData.Doc.Data::getName)
                .collect(Collectors.toList());
        final Pair<List<String>, List<Map<String, String>>> pairDocumentData = convertDataTableToListOfMaps(dataTable);

        for (String columnName : pairDocumentData.getLeft()) {
            if (!possibleFields.contains(columnName)) {
                throw new IllegalArgumentException(String.format("Column '%s' not present for document '%s'! Possible columns are: '%s'",
                        columnName, document, StringUtils.join(possibleFields, ", ")
                )
                );
            }
        }

        documentData.setDocumentSpecificData(pairDocumentData.getRight());
        documentData.setDocumentType(documentType);
        documentData.setDataProviderType(dataProviderType);

        HttpResponse<JsonNode> jsonResponse = Unirest.put(url)
                .header("Content-Type", "application/json")
                .header("certificate", Settings.getCertificate())
                .body(objectMapper.writeValueAsString(documentData))
                .asJson();

        verifyHttpStatus(jsonResponse);
    }

    public void loginWithMobileToken(String user, String pass) {

        logStopWatchDebug("loginWithMobileToken");

        if (isLoginSkip()) return;

        //waitForPageToLoad();

        WebElement element = DRIVER.findElement(By.xpath("//*[@data-target='#totp']"));
        element.click();

        //waitForPageToLoad();

        element = getUsernameInputFieldElement();
        element.sendKeys(user);
        element = getPasswordInputFieldElement();
        element.sendKeys(pass);

        // remember login credentials
        credentials.put("username", user);
        credentials.put("password", pass);


        By expression = By.className("button_submit");
        WebElement button_submit = findVisibleElement(expression);
        Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + ".", button_submit);
        button_submit.click();

        afterLogin();

        logStopWatchDebug("loginWithMobileToken", true);

    }

    public void checkGraphExists() {
        waitForPageToLoad();

        //this is for graphs like in PFM
        WebElement graph = findVisibleElement(By.xpath("//div[@id='report_graph_container']/div[contains(@class, 'highcharts-container')]"));

        //if that is not found try other known graphs
        //forex history
        if (graph == null) {
            graph = findVisibleElement(By.xpath("//div[@id='forex-history-chart']"));
        }

        Assert.assertNotNull("Graph does not exist.", graph);
    }


    //FIXME this looks like a good idea, but no xpath that I tried worked with chrome
    //even the ones returned by chrome itself
    //if there's interest to get this to work, here's the scenario with SU on prodsi:
    //	* Click on "personal finance"
    //	* Click on "Spendings & Budget"
    //	* Graph must exist with title "Total spendings"
    //...
    //there's no support for optional string argument arguments, if there were, this would be a part of the above function checkGraphExists()
    //    @Given("Graph must exist with title \"(.*)\"$")
    //    public void checkGraphExistsWithTitle(String title) {
    //    	checkText(title,"\"//div[@id='report_graph_container']/div[contains(@class, 'highcharts-container')]/svg/text[contains(@class, 'highcharts-title')]/tspan[1]\"");
    ////    	checkText(title,"\"//*[@id=\"highcharts-0\"]/svg/text/tspan[1]\"");//this one is as chrome reportedfrom chrome
    //    }


    //this code works only in admin / it is in admin because it is easyer to link
    public void checkDropdownFields(String label, String input) {
        Assert.fail("This functionality is used only in admin front-end testing. For RWD implement it.");
    }


    //this code works only in admin
    public void checkNumberOfReturnPages(String pageNumberResult) {
        waitForPageToLoad();

        int count = 0;
        while (true) {
            try {
                count++;
                String xPath = "//div[contains(@class, 'pager')]/button";
                List<WebElement> elements = DRIVER.findElements(By.xpath(xPath));

                if ("1".equals(pageNumberResult)) {
                    Assert.assertTrue("There are page number displayed although there should not be any.", elements.isEmpty());
                } else {
                    String maxPageNum = elements.get(elements.size() - 1).getText().split("\\(")[1].split("\\)")[0];
                    Assert.assertTrue("The number of returned pages does not match.", pageNumberResult.equals(maxPageNum));
                }

                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    //this code works only in admin
    public void checkNumberOfReturnedResults(String resultsNumber) {
        waitForPageToLoad();

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String xPath = "//div[contains(@class, 'sort-container')]";
                List<WebElement> elements = DRIVER.findElements(By.xpath(xPath));

                String allElements = elements.get(0).getText().substring(elements.get(0).getText().lastIndexOf(":")).trim();
                String realNumberText = allElements.replaceAll("[^0-9]+", "");

                if (realNumberText.equals("")) {
                    Assert.fail("The number of return results is missing on the current page.");
                } else {
                    Assert.assertTrue("There are no return results present on the page.", Integer.parseInt(realNumberText) >= 0);
                    Assert.assertTrue("The number of results does not match the expected number. Expected: " + resultsNumber + ", got: " + realNumberText, resultsNumber.equals(realNumberText));
                }

                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    //this code works only in admin
    public void selectFilterValue(String filterValue) {
        logStopWatchDebug("selectFilterValue");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String findDropdownXpath = "//div[contains(@class, 'sort-container')]/select[contains(@name, 'table-sort-select')]";
                List<WebElement> elements = DRIVER.findElements(By.xpath(findDropdownXpath));
                Assert.assertTrue("Filter not found or is not present on the page.", elements.size() != 0);
                waitForPageToLoad();

                String findDropdownElementXpath = "//option[text()[" + xpathLowercase(filterValue) + "=\"" + filterValue.toLowerCase() + "\"]]";
                elements = DRIVER.findElements(By.xpath(findDropdownElementXpath));
                Assert.assertTrue("Filter does not contain value [" + filterValue + "]!\nXPath = [" + findDropdownElementXpath + "]", elements.size() != 0);
                for (WebElement curr : elements) {
                    if (curr.isDisplayed() && curr.isEnabled()) {
                        clickOnElementWithScrolling(curr);
                    }
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    //this code works only in admin
    public void clickPageNumber(String pageNumber) {
        int count = 0;
        while (true) {
            try {
                count++;

                waitForPageToLoad();

                String startXPath = "//";
                String xpathExpression = startXPath + "button[contains(@class, 'pagerBtn ')]/following::*[text()[" + xpathLowercase(pageNumber) + "=\"" + pageNumber.toLowerCase() + "\"]][1]";

                try {
                    WAIT.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath(xpathExpression)));
                } catch (TimeoutException e) {
                    Assert.fail("Could not find page button containing text [" + pageNumber + "].\nXPath=[" + xpathExpression + "]");
                }

                logStopWatchDebug("Click on", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    //this code works only in admin
    public void checkButtonsOnPage(String input) {
        logStopWatchDebug("checkButtonsOnPage");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                ArrayList<String> columns = Lists.newArrayList(SPLITTER_CSV.split(input.toUpperCase()));
                // selects all divs that, when trimmed For whitespace, contain some text, so we get the column names
                // if col has hidden="true" in tableConfiguration.xml, the th element will have the class 'hide' present: ignore it
                String xpath = "//nav[contains(@aria-label, 'breadcrumb')]/preceding-sibling::div[contains(@class, 'search-bar')]//div[contains(@class, 'toolbox-right')]/a | "
                        + "//nav[contains(@aria-label, 'breadcrumb')]/preceding-sibling::div[contains(@class, 'search-bar')]//div[contains(@class, 'toolbox-left')]/a";
                List<WebElement> elementCols = DRIVER.findElements(By.xpath(xpath));
                Assert.assertEquals("The number of buttons in the top of the page don't match!", columns.size(), elementCols.size());
                int i = 0;
                for (WebElement curr : elementCols) {
                    Assert.assertEquals(columns.get(i), curr.getText().toUpperCase().trim());
                    i++;
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }


    //this code works only in admin
    public void checkSearchFields(String searchFields) {
        logStopWatchDebug("checkSearchFields");
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();

                ArrayList<String> columns = Lists.newArrayList(SPLITTER_CSV.split(searchFields.toUpperCase()));

                //because of different implementations of search fields and forms, we have to check for more then on case
                //there are two cases: label and input are on the same level or input is inside a div/td
                String xPath = "(//div[contains(@class, 'pad')]/form/label | "
                        + "//div[contains(@class, 'pad')]/form/div[contains(@class, 'criterion') and not(contains(@hidden, 'hidden'))]/label | "
                        + "//div[contains(@class, 'pad')]/form/div[not(contains(@hidden, 'hidden'))]/label)";
                String fullXpath = xPath + "/following-sibling::*[self::input[not(contains(@type, 'hidden'))] or self::textarea or self::select][1]/preceding-sibling::label |"
                        + xPath + "/following-sibling::div/input/parent::div[not(contains(@hidden, 'hidden'))][1]/preceding-sibling::label | //div[contains(@class, 'pad')]/form/table/tbody/tr/td/label";
                List<WebElement> elementCols = DRIVER.findElements(By.xpath(fullXpath));


                List<WebElement> foundElements = new ArrayList();
                for (WebElement element : elementCols) {
                    if (!element.getText().trim().equals("")) {
                        foundElements.add(element);
                    }
                }

                Assert.assertEquals("The number of search fields in the table don't match!", columns.size(), foundElements.size());
                int i = 0;
                for (WebElement curr : foundElements) {
                    Assert.assertEquals(columns.get(i), curr.getText().toUpperCase().trim());
                    i++;
                }
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }

    //this is implemented for admin
    public void closeAlert(String type) {
        Assert.fail("This functionality is used only in admin front-end testing. For RWD implement it");
    }

    //this is implemented for admin
    public void checkSectionRowColumn(String section, String row, String column, String expectedValue) {
        Assert.fail("This functionality is used only in admin front-end testing. For RWD implement it.");
    }

    //this is implemented for admin
    public void checkSectionFieldValueWithIndex(String section, String index, String field, String expectedValue) {
        Assert.fail("This functionality is used only in admin front-end testing. For RWD implement it.");
    }

    //this is implemented for admin use only
    public void checkLinkButtonInSection(String section, String fields) {
        Assert.fail("This functionality is used only in admin front-end testing. For RWD implement it.");
    }

    public void checkHompageButtonLink(String buttonName, String type) {
        Assert.fail("This functionality is used only in admin front-end testing. For RWD implment it.");
    }


    public void setCustomDate(String dateFrom) throws Exception {
        Assert.fail("This functionality is used only in admin front-end testing. For RWD implement it.");
    }

    public void switchDriverWindowFocus(String windowType) {
        Assert.fail("This functionality is used only in admin front-end testing. For RWD implement it.");
    }


    //region appium-android

    AndroidDriver<AndroidElement> driver = null;
    AndroidDriver<AndroidElement> webDriver = null;
    DesiredCapabilities dc = new DesiredCapabilities();
    DesiredCapabilities bdc = new DesiredCapabilities();
    String registrationID = null;
    String activationCode = null;
    String timeStamp = "";
    String path = "D://screenshot//";
    //AppiumDriverLocalService appiumService = AppiumDriverLocalService.buildDefaultService();

    public void launch_android_app() {
        setUp();
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        timeStamp = path + timeStamp + "//";
        File folder = new File(timeStamp);
        if (!folder.exists()) {
            try {
                folder.mkdir();
            } catch (Exception e) {
            }
        }
    }

    void waitWhileLoading() {
        while (isElementPresent(By.id("progress"), driver)) {
            Sleep(100);
        }
    }

    public void clickAndroidElement(String locator) {
        List<AndroidElement> elements = driver.findElements(By.id(locator));
        MobileElement mobileElement = null;

        if (locator.equalsIgnoreCase("login")) {
            Sleep(2000);
            //currently, this element is not directly locatable
            (new TouchAction(driver)).tap(PointOption.point(211, 364)).perform();
        } else if (locator.equalsIgnoreCase("+")) {
            (new TouchAction(driver)).tap(PointOption.point(951, 1631)).perform();
        } else {

            if (locator.equalsIgnoreCase("menu")) {
                mobileElement = driver.findElementByXPath("//android.widget.ImageButton[contains(@content-desc,'Navigate up')]");
            } else if (elements.size() > 0) {
                mobileElement = driver.findElementById(locator);
            } else if (driver.findElements(By.xpath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + locator.toLowerCase() + "')]")).size() > 0) {
                if (driver.findElements(By.xpath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + locator.toLowerCase() + "')]")).size() > 1) {
                    mobileElement = driver.findElements(By.xpath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + locator.toLowerCase() + "')]")).get(1);
                } else {
                    mobileElement = driver.findElementByXPath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + locator.toLowerCase() + "')]");
                }
            } else if (locator.equalsIgnoreCase("logo")) {
                mobileElement = driver.findElementById("splash_logo");
            } else {
                mobileElement = driver.findElementByXPath(locator);
            }

            WaitForElement(mobileElement, 20);


            try {
                (new TouchAction(driver)).tap(PointOption.point(mobileElement.getCenter().x, mobileElement.getCenter().y)).perform();
            } catch (Exception e) {
                mobileElement.click();
            }

            waitWhileLoading();
            Sleep(2000);
            androidScreenshot();
        }
    }


    @Override
    public void pressIfPresent(String text) {
        try {
            clickAndroidElement(text);
        } catch (Exception e) {
        }
    }

    void Sleep(int mils) {
        try {
            Thread.sleep(mils);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void WaitForElement(MobileElement mobileElement, int seconds) {
        WebDriverWait wait = new WebDriverWait(driver, seconds);
        wait.until(ExpectedConditions.visibilityOf(mobileElement));
    }

    void initializeDriver() {
        try {
            //startAppiumServer();
            driver = new AndroidDriver<AndroidElement>(new URL("http://127.0.0.1:4723/wd/hub"), dc);
            Sleep(10000);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

//    public void startAppiumServer() {
//        //Set Capabilities
//        DesiredCapabilities cap = new DesiredCapabilities();
//        cap.setCapability("noReset", "true");
//
//        AppiumServiceBuilder builder = new AppiumServiceBuilder();
//        //Build the Appium service
//        builder.withIPAddress("127.0.0.1");
//        builder.usingPort(4723);
//        builder.withCapabilities(cap);
//        builder.withArgument(GeneralServerFlag.SESSION_OVERRIDE);
//        builder.withArgument(GeneralServerFlag.LOG_LEVEL, "error");
//
//        //Start the server with the builder
//        appiumService = AppiumDriverLocalService.buildService(builder);
//        appiumService.start();
//    }
//
//    public void stopAppiumServer() {
//        appiumService.stop();
//    }

    public void updateCredentials(String user) {

        if (isElementPresent(By.id("oath_otp_provision_wizard_next_button"), driver)) {


            browserSetUp();

            webDriver.get("http://epsr.halcom.local:8090/otp_hypo/demo/enroll.html");
            webDriver.findElementById("usernameInput").sendKeys(user);
            webDriver.findElementById("enrollButton").click();


            Sleep(3000);
            registrationID = webDriver.findElementByXPath("//td[@id='registrationId']").getAttribute("innerHTML");
            activationCode = webDriver.findElementById("activationCode").getText().trim();


            webDriver.quit();


            setUp();
            clickAndroidElement("splash_logo");
            clickAndroidElement("menu");
            clickAndroidElement("login");


        }
    }

    public void loginWithOrWithoutCredentials(String pin, String user) {
        Sleep(5000);

        clickAndroidElement("menu");
        clickAndroidElement("login");


        if (isElementPresent(By.id("oath_otp_provision_wizard_next_button"), driver)) {
            updateCredentials(user);

            clickByID("oath_otp_provision_wizard_next_button");


            typeByID("oath_otp_registration_id", registrationID);
            typeByID("oath_otp_activation_code", activationCode);
            clickByID("oath_otp_provision_form_provision_button");

            enterPIN(pin);
            enterPIN(pin);

            System.exit(0);
            //Assert.assertEquals(driver.findElementsById("textinput_error").get(0).getText().trim(), "Invalid Registration ID.");

            back();
            back();
        } else {
            enterPIN(pin);
            //Assert.assertEquals(isTextPresent("You were automatically logged out due to inactivity.", driver), true);
            //System.exit(0);
        }
    }


    void clickDigits(String digits) {
        Sleep(5000);
        for (char c : digits.toCharArray()) {
            clickText(c + "");
        }
        Sleep(5000);

    }

    void enterPIN(String pin) {
        Sleep(5000);
        waitWhileLoading();

        clickDigits(pin);

        Sleep(5000);
        waitWhileLoading();

        //Assert.assertEquals(isTextPresent("You were automatically logged out due to inactivity.", driver), true);
        //System.exit(0);
    }

    public void back() {
        driver.navigate().back();
        waitWhileLoading();
    }

    void setUp() {
        setCapabilities("Nexus_5_API_29", "emulator-5554", "android", "com.halcom.mobile.hybrid.otpvrs22", "com.halcom.mobile.hybrid.activity.SplashActivityOTPVRS22", true);
        initializeDriver();
    }

    void browserSetUp() {
        browserCapabilities("emulator-5554", "android", "Chrome", true);

        try {
            webDriver = new AndroidDriver<AndroidElement>(new URL("http://127.0.0.1:4723/wd/hub"), bdc);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    void setCapabilities(String avd, String deviceName, String platformName, String appPackage, String appActivity,
                         boolean noReset) {
        dc.setCapability(MobileCapabilityType.DEVICE_NAME, deviceName);
        dc.setCapability("platformName", platformName);
        dc.setCapability("appPackage", appPackage);
        dc.setCapability("appActivity", appActivity);
        dc.setCapability("noReset", noReset);
        dc.setCapability("avd", avd);
        dc.setCapability("automationName", "Appium");
    }

    void browserCapabilities(String deviceName, String platformName, String browserName, boolean noReset) {
        bdc.setCapability(MobileCapabilityType.DEVICE_NAME, deviceName);
        bdc.setCapability("platformName", platformName);
        //dc.setCapability("appPackage", appPackage);
        //dc.setCapability("appActivity", appActivity);
        bdc.setCapability("noReset", noReset);
        bdc.setCapability("browserName", browserName);
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\tausifj\\Downloads\\chromedriver_win32\\chromedriver.exe");
    }

    boolean isElementPresent(By locator, AndroidDriver driver) {
        return driver.findElements(locator).size() > 0;
    }

    public boolean isTextPresent(String text, AndroidDriver driver) {
        return driver.findElements(By.xpath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + text + "')]")).size() > 0;
    }


    public void textPresent(String text) {
        Assert.assertEquals(isTextPresent(text, driver), true);
    }

    void clickByID(String id) {
        MobileElement mobileElement = driver.findElementById(id);
        WaitForElement(mobileElement, 20);
        mobileElement.click();
    }

    void clickByAccessibilityID(String id) {
        MobileElement mobileElement = driver.findElementByAccessibilityId(id);
        WaitForElement(mobileElement, 20);
        mobileElement.click();
    }

    void clickByXPath(String xpath) {
        MobileElement mobileElement = driver.findElementByXPath(xpath);
        WaitForElement(mobileElement, 20);
        mobileElement.click();
    }

    void typeByID(String id, String text) {
        driver.findElementById(id).sendKeys(text);
    }

    void clickText(String text) {
        MobileElement mobileElement = driver.findElementByXPath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + text + "')]");
        WaitForElement(mobileElement, 60);
        mobileElement.click();
    }


    public void pressRowItem(int index) {
        index = index + 2;
        clickAndroidElement("//android.widget.ListView/android.view.View[" + index + "]");
    }

    public void logoutFromApp() {
        clickAndroidElement("menu");
        clickAndroidElement("drawer_row_logout");
    }

    public void androidScreenshot() {


        File srcFile = driver.getScreenshotAs(OutputType.FILE);
        //String filename= UUID.randomUUID().toString();
        String filename = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File targetFile = new File(timeStamp + filename + ".jpg");
        try {
            FileHandler.copy(srcFile, targetFile);
        } catch (IOException ioe) {
        }
    }


    public void typeIntoEditText(String inputText, String labelText) {
        MobileElement mobileElement = null;

        try {
            mobileElement = driver.findElementByXPath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + labelText.toLowerCase() + "')]/following-sibling::android.widget.EditText[1]");
            WaitForElement(mobileElement, 20);
        } catch (Exception e) {
            mobileElement = driver.findElementByXPath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + labelText.toLowerCase() + "')]/..//android.widget.EditText[1]");
            WaitForElement(mobileElement, 20);
        }
        mobileElement.sendKeys(inputText);
    }

    public void inputIntoEditText(String inputText, String labelText) {
        MobileElement mobileElement = null;

        try {
            mobileElement = driver.findElementByXPath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + labelText.toLowerCase() + "')]/following-sibling::android.widget.EditText[1]");
            mobileElement.click();
        } catch (Exception e) {
            mobileElement = driver.findElementByXPath("//*[contains(translate(@text,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + labelText.toLowerCase() + "')]/..//android.widget.EditText[1]");
            WaitForElement(mobileElement, 20);
            mobileElement.click();
        }
        clickDigits(inputText);

        clickAndroidElement("apply");
    }

    //endregion

}
	
