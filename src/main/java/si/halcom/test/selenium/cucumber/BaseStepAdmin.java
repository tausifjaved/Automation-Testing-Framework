package si.halcom.test.selenium.cucumber;

import static si.halcom.test.selenium.SeleniumDriverHandler.DRIVER;
import static si.halcom.test.selenium.SeleniumDriverHandler.WAIT;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import si.halcom.test.selenium.SeleniumDriverHandler;
import si.halcom.test.selenium.Settings;
import si.halcom.test.selenium.cucumber.RobotThread.CertUsage;

public class BaseStepAdmin extends BaseSteps {

	@Override
	protected WebElement getPasswordInputFieldElement() {
		By expression = By.id("inputPassword");
		WebElement element = findVisibleElement(expression);
		if (element == null) {
			By expression1 = By.id("inputPassword");
			element = findVisibleElement(expression1);
			Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + " or " + expression1 + ".", element);
		}
		element.clear();
		return element;
	}
	
	@Override
	public void loginWithPassword(String pass) {
        logStopWatchDebug("loginWithPassword");

        if(isLoginSkip()) {
        	return;
		}

        WebElement element = getPasswordInputFieldElement();
        element.sendKeys(pass);
        By expression = By.id("loginButton");
        WebElement button_submit = findVisibleElement(expression);
        Assert.assertNotNull("Could not find any _visible_ element: " + expression.toString() + ".", button_submit);
        button_submit.click();

        logStopWatchDebug("loginWithPassword",true);

        afterLogin();
	}
	
	@Override
	protected boolean isLoginSkip(){
        // check if user is already logged in, when doing tests in series
        // constant close browser/open browser/login is not neccessary
        // see setting KEEP_BROWSER_OPEN_BETWEEN_TESTS
        try {
            SeleniumDriverHandler.setTimeout(0, TimeUnit.SECONDS);
            List<WebElement> jPassInput = DRIVER.findElements(By.className("csValidation"));
            boolean isLoginForm = !jPassInput.isEmpty();
            if (!isLoginForm && Settings.isBrowserOpenBetweenTests()) {
                return true;
            }
        } finally {
            SeleniumDriverHandler.restoreDefaultTimeout();
        }
        return false;
    }
	
	@Override
	public void checkBreadcrumbs(String breadcrumbs) {
		logStopWatchDebug("checkBreadcrumbs");
		
        int count = 0;
        while (true) {
            try {
                count++;
                breadcrumbs = parseDate(breadcrumbs);
                String[] requiredCrumbs = breadcrumbs.split("\\s+/\\s+");
                String xPath = "//ol[contains(@class, 'breadcrumb')]/li[not(contains(@class, 'hidden-md') or contains(@class, 'hidden-lg'))]";
                waitForPageToLoad();
                
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
                
                logStopWatchDebug("checkBreadcrumbs", true);
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
	
	//Admin has only a bank icon, so the distinction between home or bank (TOO BE CHECKED HOW TO HANDLE THIS)
	//currently default is click on logo, which is the same for either Home or Bank parameter given by the feature test
	@Override
	public void clickOnHomeOrBankIcon(String home) {
		logStopWatchDebug("clickOnHomeOrBankIcon");
		
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                waitForPageToLoad();
                
                String xpath = "//*[@class='logo']/a";
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpath));
                    element.click();
                } catch (Exception e) {
                    Assert.fail("Could not click on home or bank icon. Error: " + e.getMessage() + " \nXPath = [" + xpath + "]");
                }
                
                logStopWatchDebug("clickOnHomeOrBankIcon", true);
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
	
	@Override
	public void clickOn(String indexStr, String buttonOrLinkText) throws Exception {
        logStopWatchDebug("Click on " + (indexStr != null ? indexStr + " " : "") + buttonOrLinkText);

        int index = 0;
        if (indexStr != null) {
            switch (indexStr.trim()) {
            case "first": index = 0; break;
            case "second": index = 1; break;
            case "third": index = 2; break;
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
                    if(translationsEnabled){
                        //if translations are enabled skip language selection in
                        //deprecated test definitions
                        logStopWatchDebug("Click on",true);
                        return;
                    }else{
                        List<WebElement> element = DRIVER.findElements(By.tagName("html"));
                        if (!element.isEmpty() && "en".equals(element.get(0).getAttribute("lang"))) {
                            logDebug("Page already in English language");
                            logStopWatchDebug("Click on",true);
                            return;
                        }
                    }
                } else if(buttonOrLinkText.equalsIgnoreCase("Pay")) {
                    DRIVER.findElement(By.id("buttonPay")).click();
                    logDebug("Special case for pay button");
                    logStopWatchDebug("Click on",true);
                    return;
                } else{

                    waitForPageToLoad();
                }

                String xpathExpression = "//button[descendant-or-self::*[text()[" + xpathLowercase(buttonOrLinkText) + "=\"" + buttonOrLinkText.toLowerCase() + "\"]]] | " +
                        "//a[descendant-or-self::*[text()[" + xpathLowercase(buttonOrLinkText) + "=\"" + buttonOrLinkText.toLowerCase() + "\"]]] | " +
                        "//th[contains(@class,'sortable')]/div[descendant-or-self::*[text()[" + xpathLowercase(buttonOrLinkText) + "=\"" + buttonOrLinkText.toLowerCase() + "\"]]] | " +
                		"//div[self::*[text()[" + xpathLowercase(buttonOrLinkText) + "=\"" + buttonOrLinkText.toLowerCase() + "\"]]] | " +
                        "//thead/tr/th[text()[" + xpathLowercase(buttonOrLinkText) + "=\"" + buttonOrLinkText.toLowerCase() + "\"]] |" +
                        "//a[descendant-or-self::*[. = '" + buttonOrLinkText + "']] | " +
                        "//a[contains(@title, \"" + buttonOrLinkText + "\")]";

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

                logStopWatchDebug("Click on",true);
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
	
	@Override
	public void clickOnMailOrLanguageOrUserIcon(String icon) {
		logStopWatchDebug("clickOnMailOrLanguageOrUserIcon");
		
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
                    //xpath = "//*[@id='languageSelector']/i";
                	Assert.fail("Admin sets language when loging in.");
                }
                if (isMailIconType) {
                    Assert.fail("Admin currently has no mail icon to click.");
                }
                if (isUserIconType) {
                    xpath = "//*[@class='profile-info']";
                }
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpath));
                    element.click();
                } catch (Exception e) {
                    Assert.fail("Could not click on home or language or bank icon. Error: " + e.getMessage() + " \nXPath = [" + xpath + "]");
                }
                
                logStopWatchDebug("clickOnMailOrLanguageOrUserIcon", true);
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
	
	
	@Override
	public void checkModalWindowTitle(String title) {	
		logStopWatchDebug("checkModalWindowTitle");
		
		//have to change frames because admin uses iFrames
		DRIVER.switchTo().frame(DRIVER.findElement(By.xpath("//iframe[starts-with(@id, 'fancybox-frame')]")));
        checkText(title, "//div[@class='search-bar']/div[@class='pad']/h2");
        
        DRIVER.switchTo().parentFrame();
        logStopWatchDebug("checkModalWindowTitle", true);
    }
	
	@Override
	public void clickOnActionInTableRow(String row, String action) {
		logStopWatchDebug("clickOnActionInTableRow");
		
		int count = 0;
		
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String xpath = "//table[contains(@class, 'items-table')]/tbody/tr[" + row + "]//button[text()[" + xpathLowercase(action) + "=\"" + action.toLowerCase() + "\"]]";
                List<WebElement> actionElement = DRIVER.findElements(By.xpath(xpath));
                
                Assert.assertTrue("Action not found.\nXPath = [" + xpath + "]", actionElement.size() > 0);
                Assert.assertTrue("Multiple action buttons were found, only needed one.\nXPath = [" + xpath + "]", actionElement.size() <= 1);
                actionElement.get(0).click();
                
                logStopWatchDebug("clickOnActionInTableRow", true);
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
	
	@Override
	public void clickShortListAction(String row, String action) {
		logStopWatchDebug("clickShortListAction");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                //so we can also use {TODAY} tag when clicking on an item in table
                action = parseDate(action.trim());

                String xpath = "//table[contains(@class,'items-table')]/tbody/tr[" + row +"]//a[text()[" + xpathLowercase(action) + "=\"" + action.toLowerCase() + "\"]]";

                //take into account only elements that are currently displayed
                List<WebElement> elements = DRIVER.findElements(By.xpath(xpath))
                        .stream()
                        .filter(WebElement::isDisplayed)
                        .collect(Collectors.toList());

                Assert.assertTrue("Action not found.\nXPath = [" + xpath + "]", elements.size() != 0);
                clickOnElementWithScrolling(elements.get(0));
                
                logStopWatchDebug("clickShortListAction",true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("clickShortListAction",true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }
	
	@Override
	public void selectBox(String label, final String selection) throws Exception {
        logStopWatchDebug("selectBox");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String findDropdownXpath = "(//label[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/following::select "
                		+ "| //label[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/following-sibling::select)[1]";
                List<WebElement> elements = DRIVER.findElements(By.xpath(findDropdownXpath));
                Assert.assertTrue("Dropdown with the label [" + label + "] not found!\nXPath = [" + findDropdownXpath + "]", elements.size() != 0);
                for (WebElement curr : elements) {
                    if (curr.isDisplayed() && curr.isEnabled()) {
                        clickOnElementWithScrolling(curr);
                        break;
                    }
                }
                waitForPageToLoad();
                String findDropdownElementXpath = findDropdownXpath + "//option[text()[" + xpathLowercase(selection) + "=\"" + selection.toLowerCase() + "\"]]";
                elements = DRIVER.findElements(By.xpath(findDropdownElementXpath));
                Assert.assertTrue("Dropdown with the label + [" + label + "] does not contain value [" + selection + "]!\nXPath = [" + findDropdownElementXpath + "]", elements.size() != 0);
                for (WebElement curr : elements) {
                    if (curr.isDisplayed() && curr.isEnabled()) {
                        clickOnElementWithScrolling(curr);
                        break;
                    }
                }
                
                logStopWatchDebug("selectBox",true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("selectBox",true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }
	
	
	@Override
	public void isRadioButtonSelected(String label, String action) {
		logStopWatchDebug("isRadioButtonSelected");
		
		int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                boolean isChecked = action.equals("selected");

                String exeScript = "return !($('label:contains(" + label +")').prev().is(\":checked\") == " + isChecked + " )"
                					+ " || !($('label:contains(" + label + ")').children()[0].checked == " + isChecked + " );";
	            
	            //this is used to check wether the radio button is selected or not through jquery
	            //this is done so, because currently there is no class added to radio button when it is checked like in RWD
	            JavascriptExecutor js = (JavascriptExecutor) DRIVER;
	            String rez = js.executeScript(exeScript).toString();

	            if( (rez.equals("false")  && isChecked) | (rez.equals("true") && !isChecked)) {
	            	Assert.fail("The radio button is not in the correct state. Should be: " + isChecked + ", but is " + rez);
	            } else if ( !(rez.equals("false") | rez.equals("true"))){
	            	Assert.fail("There was no radio button found on the page, with the Jquery code: " + exeScript);
	            }
	            
	            logStopWatchDebug("isRadioButtonSelected",true);
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
	
	
	@Override
	public void checkTableContentsByColumnName(String row, String columnName, String text) {
		logStopWatchDebug("checkTableContentsByColumnName");
		
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                List<WebElement> elements = DRIVER.findElements(By.xpath("//table[contains(@class, 'items-table')]/thead/tr/th"));
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
                    Assert.fail("Column name [" + columnName + "] not found! Check if <thead> is present in the table (most common mistake for this).");
                }
                
                logStopWatchDebug("checkTableContentsByColumnName", true);
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
	
	
	@Override
	protected void checkTableContents(String row, String column, String text) {
		logStopWatchDebug("checkTableContents");
		
        waitForPageToLoad();
        WebElement element = getElementInDynamicTable("//table[contains(@class, 'items-table')]/tbody/tr[" + row + "]/td[" + column + "]");
        String contents = element.getText();
        Assert.assertEquals(text.toUpperCase(), contents.trim().toUpperCase());
        
        logStopWatchDebug("checkTableContents", true);
    }
	
	
	@Override
	public void setCustomDateRange(String dateFrom, String dateTo) throws Exception {
		logStopWatchDebug("setCustomDateRange");
		
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                if(dateTo.length() != 7) {
	                dateFrom = parseDate(dateFrom);
	                dateTo = parseDate(dateTo);
	                
	                //there is no apply button since the from and to input fields are used as searching parameters
	                ((JavascriptExecutor) DRIVER).executeScript("$('input[name=dateFrom]').val('" + dateFrom + "');");
	                ((JavascriptExecutor) DRIVER).executeScript("$('input[name=dateTo]').val('" + dateTo + "');");
                } else {
                	dateFrom = parseDate(dateFrom);
                	dateTo = parseDate(dateTo);
                	
                	//there is no apply button since the from and to input fields are used as searching parameters
                	((JavascriptExecutor) DRIVER).executeScript("$('input[name=monthFrom]').val('" + dateFrom + "');");
                	((JavascriptExecutor) DRIVER).executeScript("$('input[name=monthTo]').val('" + dateTo + "');");
                }
                
                logStopWatchDebug("setCustomDateRange", true);
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
	
	
	@Override
	public void checkFilterFields(String fields) {
		logStopWatchDebug("checkFilterFields");
		
        int count = 0;
        while (true) {
            try {
                count++;
                String xpath = "//select[contains(@name, 'table-sort-select')]/option";
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
                
                logStopWatchDebug("checkFilterFields", true);
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
	
	@Override
	public void isCheckboxSelected(String label, String action) {
	    logStopWatchDebug("isCheckboxSelected");
	
	    int count = 0;
	    while (true) {
	        try {
	            count++;
	            waitForPageToLoad();
	            boolean isChecked = action.equals("selected");
	            String exeScript = "return $('label:contains(" + label + ")').next().is(\"input:checked\")";
	            
	            //this is used to check wether the checkbox is checked or not through jquery
	            //this is done so, because currently there is no class added to checkbox when it is checked like in RWD
	            JavascriptExecutor js = (JavascriptExecutor) DRIVER;
	            String rez = js.executeScript(exeScript).toString();

	            if( (rez.equals("false") && isChecked) | (rez.equals("true") && !isChecked)) {
	            	Assert.fail("The checkbox is not in the correct state. Should be: " + isChecked + ", but is " + rez);
	            } else if ( !(rez.equals("false") | rez.equals("true"))){
	            	Assert.fail("There was no checkbox found on the page, with the Jquery code: " + exeScript);
	            }
	            
	            logStopWatchDebug("isCheckboxSelected",true);
	            return;
	        } catch (Throwable e) {
	            if (count >= MAX_RETRIES_ON_ERROR) {
	                logStopWatchDebug("isCheckboxSelected",true);
	                throw e;
	            } else {
	                waitForPageToLoad();
	            }
	        }
	    }
	}
	
	
	public void checkSectionSubsectionFieldValueWithIndex(String section, String index, String subsection, String field, String expectedValue) {
		logStopWatchDebug("checkSectionSubsectionFieldValueIndex " + index);
        
        int count = 0;
        while (true) {
            try {
                count++;
                expectedValue = parseDate(expectedValue.trim());
                waitForPageToLoad();
                section = section.trim();
                field = field.trim();
                String xPath = "(//div[contains(@class, 'pad')]//h1[text()[" + xpathLowercase(section) + "=\"" + section.toLowerCase() + "\"]]/following::table[1]//tr/td[text()[" + xpathLowercase(field) + "=\"" + field.toLowerCase() + "\"]]/following-sibling::td |"
                		+ "//div[contains(@class, 'pad')]//h1[text()[" + xpathLowercase(section) + "=\"" + section.toLowerCase() + "\"]]/following::form//label[text()[" + xpathLowercase(field) + "=\"" + field.toLowerCase() + "\"]]/following-sibling::input[1])[" + index +"]";
                List<WebElement> elements = DRIVER.findElements(By.xpath(xPath));
                
                WebElement visibleElement = findVisibleElementWithIndex(elements, Integer.parseInt(index));

                if (visibleElement == null) {
                    Assert.fail("Could not find element.");
                } else {
                    Assert.assertEquals("The value does not match the expected result:", expectedValue.toUpperCase(), visibleElement.getText().toUpperCase());
                }
                
                logStopWatchDebug("checkSectionSubsectionFieldValueIndex", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkSectionSubsectionFieldValueIndex",true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }
	
	@Override
	public void checkSectionFieldValueWithIndex(String section, String index, String field, String expectedValue) {
		checkSectionSubsectionFieldValueWithIndex(section, index, "", field, expectedValue);
    }
	
	@Override
	public void selectCheckbox(String action, String label) {
        logStopWatchDebug("selectCheckbox");

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                boolean select = action.equals("Select");
                String xpathCheckboxInput = "//label/descendant-or-self::*[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/following::*[@type='checkbox']";
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpathCheckboxInput));
                    if (element.isSelected() && !select || !element.isSelected() && select) {
                        String xpath = "//label/descendant-or-self::*[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/following::*[@type='checkbox']";
                        element = DRIVER.findElement(By.xpath(xpath));
                        clickOnElementWithScrolling(element);
                        waitForPageToLoad();
                    }
                } catch (TimeoutException e) {
                    Assert.fail("Could not find checkbox.\nXPath = [" + xpathCheckboxInput + "]");
                }
                
                logStopWatchDebug("selectCheckbox",true);
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
	
	@Override
	public void selectRadioButton(String label) {
        logStopWatchDebug("Select radio button " + label);

        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String xpath = "//label/descendant-or-self::*[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/preceding-sibling::*[@type='radio']";
                try {
                    WebElement element = DRIVER.findElement(By.xpath(xpath));
                    element.click();
                } catch (TimeoutException e) {
                    Assert.fail("Could not find radio button.\nXPath = [" + xpath + "]");
                }
                logStopWatchDebug("Select radio button " + label,true);
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
	
	@Override
	public void checkActionButtonsInTableRow(String rowNumber, String actionsCSV) {
		logStopWatchDebug("checkActionButtonsInTableRow");
		
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                String actionsXpath = "//table[contains(@class, 'items-table')]/tbody/tr[" + rowNumber + "]//button";
                ArrayList<String> actionList = Lists.newArrayList(SPLITTER_CSV.split(actionsCSV.toUpperCase()));
                
                List<WebElement> elements = DRIVER.findElements(By.xpath(actionsXpath));
                Assert.assertEquals("The number of actions doesn't match!", actionList.size(), elements.size());
                for (WebElement curr : elements) {
                    String action = curr.getText().trim();
                    Assert.assertTrue("Action [" + action + "] not found.", actionList.contains(action.toUpperCase()));
                }
                elements.get(0).sendKeys(Keys.ESCAPE);
                
                logStopWatchDebug("checkActionButtonsInTableRow", true);
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
	
	@Override
	public void checkVisibleColumnsInTable(String columnsCSV) {
		logStopWatchDebug("checkVisibleColumnsInTable");
		
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                ArrayList<String> columns = Lists.newArrayList(SPLITTER_CSV_KEEP_EMPTY_STRING.split(columnsCSV.toUpperCase()));
                // selects all divs that, when trimmed For whitespace, contain some text, so we get the column names
                // if col has hidden="true" in tableConfiguration.xml, the th element will have the class 'hide' present: ignore it
                String xpath = "//table[contains(@class, 'items-table')]//th[not(contains(@hidden, 'hidden'))]";
                List<WebElement> elementCols = DRIVER.findElements(By.xpath(xpath));
                Assert.assertEquals("The number of columns in the table don't match!", columns.size(), elementCols.size());
                int i = 0;
                for (WebElement curr : elementCols) {
                    Assert.assertEquals(columns.get(i), curr.getText().toUpperCase().trim());
                    i++;
                }
                
                logStopWatchDebug("checkVisibleColumnsInTable", true);
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
	
	@Override
	public void checkMessage(String message) {
		logStopWatchDebug("checkMessage");
		
		waitForPageToLoad();
        String xpathQuerry = "//div[contains(@id, 'msgs') and not(contains(@id, 'msgs_log'))]/div[text()[" + xpathLowercase(message) + "=\"" + message.toLowerCase() + "\"]]";
        
        try {
            WAIT.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathQuerry)));
            logStopWatchDebug("checkMessage", true);
        } catch (Exception e) {
            Assert.fail("Message not displayed.");
        }
	}
	
	
	@Override
	public void areTabsPresent(String tabs) {
		logStopWatchDebug("areTabsPresent");
		
        int count = 0;
        while (true) {
            try {
                count++;
                waitForPageToLoad();
                ArrayList<String> expectedTabs = Lists.newArrayList(SPLITTER_CSV.split(tabs));
                Assert.assertTrue("There must be more than 1 tab present, or the tabs will not be shown.", expectedTabs.size() > 1);

                String tabXpath = "//div[contains(@class, 'nav-bar')]/a";
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
                
                logStopWatchDebug("areTabsPresent", true);
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
	
	
	@Override
	public void checkButtonsDetailsPage(String input) {
		logStopWatchDebug("checkButtonsDetailsPage");
		
        int count = 0;
        while (true) {
            try {
                count++;
                String xpath = "//div[contains(@class, 'detail')]/div[contains(@class, 'search-bar')]//a";
                checkButtonsText(input, xpath, "detail_buttons");
                
                logStopWatchDebug("checkButtonsDetailsPage", true);
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
	
	
	@Override
	public void checkIsSpecificPage(String notOperator, String page) {
        int count = 0;
        boolean shouldBe = notOperator == null || "".equalsIgnoreCase(notOperator);
        while (true) {
            try {
                count++;
                // login page check
                if (LANDING_PAGE_LOGIN.equals(page)) {
                    // login page has login_form
                    List<WebElement> jPassInput = DRIVER.findElements(By.xpath("//button[contains(@id, 'loginButton')]"));
                    boolean isLoginForm = !jPassInput.isEmpty();
                    if (isLoginForm && !shouldBe) {
                        Assert.fail("Login page found.");
                    }
                    if (!isLoginForm && shouldBe) {
                        Assert.fail("Login page not found.");
                    }
                } else if (LANDING_PAGE_HOME.equals(page)) {
                    List<WebElement> homeBodyTag = DRIVER.findElements(By.xpath("//h2[contains(@id,'main-title')]"));
                    boolean isHomePage = !homeBodyTag.isEmpty();
                    if (isHomePage && !shouldBe) {
                        Assert.fail("Home page found.");
                    }
                    if (!isHomePage && shouldBe) {
                        Assert.fail("Home page not found.");
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
	
	@Override
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
                String xPath = "(//div[contains(@class, 'pad')]//h1[text()[" + xpathLowercase(section) + "=\"" + section.toLowerCase() + "\"]]/following::table//tr/td[text()[" + xpathLowercase(field) + "=\"" + field.toLowerCase() + "\"]]/following-sibling::td)[1]";
                List<WebElement> elements = DRIVER.findElements(By.xpath(xPath));

                Assert.assertTrue("There is no element with the \\nXPath = [" + xPath + "]", elements.size() > 0);
                Assert.assertTrue("There is more than one field with the same name.", elements.size() == 1);
                Assert.assertEquals("The value does not match the expected result:", expectedValue.toUpperCase(), elements.get(0).getText().toUpperCase());
                
                logStopWatchDebug("checkSectionSubsectionFieldValue", true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkSectionSubsectionFieldValue",true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }
	
	
	//this is used only for buttons containing <img> elements inside the anchor html tag
	protected void checkButtonsText(String input, String xpath, String type) {
		logStopWatchDebug("checkButtonsText");
		
        waitForPageToLoad();
        List<String> requestedButtons = new ArrayList<>();
        for (String s : input.split(",")) {
        	String trimmed = s.trim().toUpperCase();
        	if(!"".equals(trimmed)) {
				requestedButtons.add(s.trim().toUpperCase());
			}else if(type.equals("dropdown")) {
				requestedButtons.add("");
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
        
        
        if(type.equals("detail_buttons")) {
            List<String> foundTextButtons = new ArrayList<>();
            for(String element : foundButtons) {
            	foundTextButtons.add(element.split(" ")[element.split(" ").length - 1]);
            }
            
            foundButtons = foundTextButtons;
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
       
        logStopWatchDebug("checkButtonsText", true);
    }
	
	
	@Override
	public void checkDropdownFields(String label, String input) {
		logStopWatchDebug("checkDropdownFields");
		waitForPageToLoad();
		
		int count = 0;
		while (true) {
            try {
                count++;
                String xPath = "//label[text()[" + xpathLowercase(label) + "=\"" + label.toLowerCase() + "\"]]/following::select[1]/option";
                //List<WebElement> elements = DRIVER.findElements(By.xpath(xPath));
                
                checkButtonsText(input, xPath, "dropdown");
                
                logStopWatchDebug("checkDropdownFields", true);
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
	
	@Override
	public void checkSectionRowColumn(String section, String row, String column, String expectedValue) {
		logStopWatchDebug("checkSectionRowColumn");
		
		int count = 0;
		while (true) {
			try {
				count++;
				waitForPageToLoad();
				
				List<WebElement> elements = DRIVER.findElements(By.xpath("//div[contains(@class, 'pad')]/h1[text()[" + xpathLowercase(section) + "=\"" + section.toLowerCase() + "\"]]" +
								"/following::table[contains(@class, 'items-table')][1]/thead/tr/th"));
                // find the column index
                int i = 1;
                boolean found = false;
                for (WebElement curr : elements) {
                	String bla = curr.getText();
                    if (curr.getText().toLowerCase().equals(column.toLowerCase())) {
                        found = true;
                        break;
                    }
                    i++;
                }
                if (found) {
                	String xPath = "//div[contains(@class, 'pad')]/h1[text()[" + xpathLowercase(section) + "=\"" + section.toLowerCase() + "\"]]/following::table[contains(@class, 'items-table')][1]/tbody/tr[" + row + "]/td[" + i + "]";
                	
                	List<WebElement> element =  DRIVER.findElements(By.xpath(xPath));
                    String contents = element.get(0).getText();
                    Assert.assertEquals(expectedValue.toUpperCase(), contents.trim().toUpperCase());
                } else {
                    Assert.fail("Column name [" + column + "] not found! Check if <thead> is present in the table (most common mistake for this).");
                }
                
                logStopWatchDebug("checkSectionRowColumn", true);
                return;
			}catch (Throwable e) {
				if (count >= MAX_RETRIES_ON_ERROR) {
					throw e;
				}else {
					waitForPageToLoad();
				}					
			}
		}
	}
	
	@Override
	public void checkPageTitle(String title) {
		logStopWatchDebug("checkPageTitle");
        checkText(title, "//div[contains(@class, 'search-bar')]//h2");
        
        logStopWatchDebug("checkPageTitle", true);
    }
	
	@Override
	public void checkLinkButtonInSection(String section, String fields) {
		logStopWatchDebug("checkLinkButtonsInSection");

        int count = 0;
        while (true) {
            try {
                count++;
                List<String> requestedLinks = new ArrayList<>();
                for (String link : fields.split(",")) {
                    requestedLinks.add(link.trim().toLowerCase());
                }


                String xpath = "//div[contains(@class, 'detail')]//div[contains(@class, 'link-container')]/a";

                waitForPageToLoad();

                //take into account only links that are currently displayed
                List<WebElement> foundLinks = DRIVER.findElements(By.xpath(xpath))
                        .stream()
                        .filter(WebElement::isDisplayed)
                        .collect(Collectors.toList());

                String foundLinksReport = foundLinks
                        .stream()
                        .map(l->l.getAttribute("innerHTML").trim())
                        .filter(s->s!=null&&!"".equals(s))
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
                logStopWatchDebug("checkLinkButtonsInSection",true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkLinkButtonsInSection",true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
	}
	
	@Override
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
        WebElement upload = DRIVER.findElement(By.xpath("//input[contains(@type, 'file')]"));
        upload.sendKeys(absolutePath);
        WAIT.until(ExpectedConditions.invisibilityOfElementLocated(By.className("blockPage")));
        waitForPageToLoad();
        // TODO: not needed?
        // DRIVER.findElement(By.id("file-input-button")).click();
    }
	
	@Override 
	public void checkHompageButtonLink(String buttonName, String type) {
        int count = 0;
        
        while (true) {
        	try {
        		count++;
        		
        		logStopWatchDebug("checkHompageButtonLink");
        		
        		String xpath;
        		if(type == null) {
        			xpath = "//a[contains(@class, 'module') and not(contains(@class, 'disabled'))]/span[text()[" + xpathLowercase(buttonName) + "=\"" + buttonName.toLowerCase() + "\"]]";
        		} else {
        			xpath = "//a[contains(@class, 'module') and contains(@class, 'disabled')]/span[text()[" + xpathLowercase(buttonName) + "=\"" + buttonName.toLowerCase() + "\"]]";
        		}
        		WebElement homepageButtonExists = DRIVER.findElement(By.xpath("//a[contains(@class, 'module')]/span[text()[" + xpathLowercase(buttonName) + "=\"" + buttonName.toLowerCase() + "\"]]"));
        		WebElement homepageButton = DRIVER.findElement(By.xpath(xpath));
        		
        		Assert.assertFalse("No element was found on the page, with the following xpath: " + xpath, homepageButtonExists == null);
        		Assert.assertFalse("The button found does not match the required type of being enabled or disabled. Xpath: " + xpath, homepageButton == null);
        		
        		logStopWatchDebug("checkHompageButtonLink",true);
        		return;
        	} catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("checkHompageButtonLink",true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
		
	}
	
	@Override
	public void setCustomDate(String dateFrom) throws Exception {
		logStopWatchDebug("setCustomDateRange");
		waitForPageToLoad();
		
        int count = 0;
        while (true) {
            try {
                count++;
                dateFrom = parseDate(dateFrom);
            	
            	//there is no apply button since the from and to input fields are used as searching parameters
            	((JavascriptExecutor) DRIVER).executeScript("$('input[name=monthFrom]').val('" + dateFrom + "');");
                
                logStopWatchDebug("setCustomDateRange", true);
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
	
	@Override
	public void checkShortListValue(String row, String label, String value) {
		logStopWatchDebug("checkShortListValue");
		
		int count = 0;
        while (true) {
            try {
                count++;
        		waitForPageToLoad();
                
        		value = parseDate(value);
        		String xpathColumn = "//table[contains(@class, 'items-table')]/thead/tr/th";
        		List<WebElement> columnNames = DRIVER.findElements(By.xpath(xpathColumn));
        		
        		Assert.assertFalse("There was no column found with the name: \"" + label + "\" on the page", columnNames == null);
        		
        		int columnIndex = 1;
        		for(WebElement ele: columnNames) {
        			if(ele.getText().equals(label)) {
        				break;
        			}
        			columnIndex++;
        		}
        		
        		String xpath = "//table[contains(@class, 'items-table')]/tbody/tr[" + row + "]/td[" + columnIndex + "]";
        		WebElement valueFound = DRIVER.findElement(By.xpath(xpath));

        		Assert.assertTrue("The expected value is not right. Expected: " + value + ", but found: " + valueFound.getText(), value.equals(valueFound.getText()));
                logStopWatchDebug("checkShortListValue", true);
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
	
	@Override
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
                        break;
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
                logStopWatchDebug("enterTextInputField",true);
                return;
            } catch (Throwable e) {
                if (count >= MAX_RETRIES_ON_ERROR) {
                    logStopWatchDebug("enterTextInputField",true);
                    throw e;
                } else {
                    waitForPageToLoad();
                }
            }
        }
    }
	
	@Override
	public void launchBrowser(String nexusPersonal) throws Exception {
        logStopWatchDebug("launchBrowser");

        boolean isDriverAlreadySet = false;
        if (SeleniumDriverHandler.isDriverInitialized()) {
            if (!Settings.isBrowserOpenBetweenTests()){
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
            	
            	if(withoutCertificate) {
            		new RobotThread(CertUsage.WITHOUT_CERT);
            	}else {
	                // set up the robot for entering pin in Nexus Personal in a new thread...
	                new RobotThread(CertUsage.WITH_CERT);
            	}
            }
        }

        logStopWatchDebug("launchBrowser",true);
		
        //always go to bank default page
        try {
			logStopWatchDebug("Open URL: "+Settings.getServerUri());
            SeleniumDriverHandler.gotoDefaultServerURI();
            closeAlert("accepting");
			logStopWatchDebug("Open URL: "+Settings.getServerUri(),true);
        } catch (Exception e) {
            createScreenshot(null);
            throw new IllegalStateException("Failed to load the page [" + Settings.getServerUri() + "]: " + e.getMessage());
        }
        waitForPageToLoad();


    }
	
	//this is to handle an exception if a popup appears, it blocks all the test that are run after the alert opens if we do not close it
	@Override
	public void closeAlert(String type) {
		try {
			Alert alert = DRIVER.switchTo().alert();
			String alertText = alert.getText();
			
			if (type.toLowerCase().equals("accepting")) {
				alert.accept();	
			}else {
				alert.dismiss();
			}
			DRIVER.switchTo().defaultContent();
			
			logDebug("There was a alert popup with the message: " + alertText + " and was accepted, to contineu the front-end testing uninterupted.");
		}catch(NoAlertPresentException ex){
			logDebug("There was no alert popup opend at that time. Continuing as intended.");
		}
	}
	
	
	//to handle opening and closing modal windows inside of admin, we have to switch the focuse of the DRIVER
	//this is because admin uses iframes for the modal windows
	@Override
	public void switchDriverWindowFocus(String windowType) {
		waitForPageToLoad();
		
		if(windowType.toLowerCase().equals("default")) {
			DRIVER.switchTo().defaultContent();
			logDebug("Switching DRIVER to default window.");
		} else {
			DRIVER.switchTo().frame(DRIVER.findElement(By.className("fancybox-iframe")));
			logDebug("Switching DRIVER to current active window.");
		}
	}
}
