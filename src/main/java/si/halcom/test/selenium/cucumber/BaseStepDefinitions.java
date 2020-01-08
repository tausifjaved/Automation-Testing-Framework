package si.halcom.test.selenium.cucumber;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import si.halcom.test.selenium.CucumberTest;
import si.halcom.test.selenium.SeleniumDriverHandler;
import si.halcom.test.selenium.Settings;
import si.halcom.test.ssl.SSLUtilities;

/**
 * The main Cucumber Step (Command) Definition Class. Here's where the test DSL
 * is defined.
 *
 * @author kris
 */
@ContextConfiguration(classes = CucumberTest.class)
public class BaseStepDefinitions {

    @Autowired
    BaseStepsInterface BaseStepsInterface;

    //keep record of failed scenarios
    protected boolean quitTesting = false;

    // the current Cucumber Scenario
    protected Scenario scenario;

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    /**
     * if server is not available, any further testing does not make sense because all tests will fail
     */
    protected void doServerHealthCheck() {
        //reset global exit variable
        quitTesting = false;
        //prepare local error variable
        String lastError = null;
        //don't run healtcheck if HEALTH_CHECK_TIMEOUT_IN_MINUTES is empty or 0
        if (Settings.isHealthCheck()) {
            //repeat HC every 30 seconds until X minutes from settings pass
            int repeatHealthCheck = Settings.getHealthCheckTimeout() * 2;
            //get server status URL
            String urlString = Settings.getServerHealthckeckUrl();

            System.out.println("Doing system healthcheck for " + urlString);
            System.out.println("Configured repetition is " + repeatHealthCheck + " minutes.");
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e1) {
                Assert.fail("Invalid server healthchek url: " + urlString);
            }
            //repeat healthcheck each 30s until healthcheck returns OK or timeouts
            int repetitions = repeatHealthCheck * 2;
            int repetition = 1;
            while (repeatHealthCheck >= 0) {

                System.out.println("Doing healtcheck ping " + repetition + "/" + repetitions);
                repetition++;

                try {
                    //don't evaluate certificates, just check if server is up
                    if ("https".equals(url.getProtocol())) {
                        SSLUtilities.trustAllHostnames();
                        SSLUtilities.trustAllHttpsCertificates();
                    }
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    String data = SSLUtilities.readInputStreamToString(connection);
                    int code = connection.getResponseCode();
                    //if server page is not up and running
                    if (code != 200) {
                        //remember last error
                        lastError = "Server URI healthcheck fail. Server response: " + code + " (" + urlString + ")";

                        System.out.println("Healthcheck failed. System responded with code " + code);

                    }
                    //server page is up and running
                    else {
                        //check exact server response for /status URL, it must be exactly "OK"
                        if (data != null && "OK".equals(data.trim())) {
                            //=============== EXIT HERE ================
                            return;//code ok, break out of loop and function
                            //=============== EXIT HERE ================
                        } else {
                            //server reported something else
                            lastError = "Server URI healthcheck fail. Response text from status URL is not OK: (" + urlString + ")";
                        }
                    }
                } catch (ConnectException e) {
                    //if server is not responding at all
                    lastError = "Server URI healthcheck fail. Server not reachable. (" + urlString + ")";
                    System.out.println("Healthcheck failed. ConnectException.");
                } catch (Exception e) {
                    //any other exceptions
                    lastError = e.getMessage();
                    System.out.println("Healthcheck failed. " + e.getMessage());
                }
                //if we come to this point, wait for half a minute, then repeat
                try {
                    Thread.sleep(Settings.HEALTH_CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    System.exit(1);
                }
                //countdown to 0
                repeatHealthCheck--;
            }
            quitTesting = true;
            Assert.fail(lastError);
        } else {
            System.out.println("Running test without system healthcheck");
        }
    }

    /**
     * @param message text to write to scenario output
     */
    protected void logDebug(String message) {
        if (Settings.isVerboseLogs() && scenario != null) {
            scenario.write(message);
        }
    }

    // THE START OF GIVEN FUNCTIONS
    @Before
    public void before(Scenario scenario) {
        this.setScenario(scenario);
        BaseStepsInterface.setScenario(scenario);

        //do healthcheck with server URI
        doServerHealthCheck();
    }

    @After
    public void after(Scenario scenario) {
        //look for global variable, see @Before scenario
        if (quitTesting) {
            //force quit testing
            System.exit(1);
        }

        if (scenario.isFailed()) {
            try {
                byte[] image = BaseStepsInterface.createScreenshot(scenario);
                scenario.embed(image, "image/png");
            } catch (Exception e) {
                throw new IllegalStateException("Could not create a screenshot of the failed scenario: " + e.getMessage(), e);
            }
        }
        if (!Settings.isBrowserOpenBetweenTests()) {
            SeleniumDriverHandler.shutDown();
        }
    }

    @Given("Login with mobile token using username \"(.*)\" and password \"(.*)\"")
    public void loginWithMobileToken(String user, String pass) {
        BaseStepsInterface.loginWithMobileToken(user, pass);
    }

    @Given("Take screenshot")
    public void takeScreenshot() throws Exception {
        BaseStepsInterface.takeScreenshot();
    }

    @Given("Wait ([1-9][0-9]*)")
    public void waitMilliseconds(long millis) {
        BaseStepsInterface.waitMilliseconds(millis);
    }

    @Given("^Launch browser( and choose the default certificate in Nexus Personal| without certificate)?")
    public void launchBrowser(String nexusPersonal) throws Exception {
        BaseStepsInterface.launchBrowser(nexusPersonal);
    }

    @Given("Go to (.*) with GET method")
    public void goToUrl(String url) throws Exception {
        BaseStepsInterface.goToUrl(url);
    }

    @Given("Sign with default certificate in Nexus Personal")
    public void enterPin() throws Exception {
        BaseStepsInterface.enterPin();
    }

    @Given("Sign with OTP \"(\\d+)\"( and press ENTER)?")
    public void signWithOTP(String otp, String enter) {
        BaseStepsInterface.signWithOTP(otp, enter);
    }

    @Given("Sign document")
    public void signDocument() {
        BaseStepsInterface.signDocument();
    }

    @Given("Enter PIN and press the ENTER key")
    public void enterPin2() throws Exception {
        BaseStepsInterface.enterPin2();
    }

    @Given("Login with certificate and password \"(.*)\"")
    public void loginWithPassword(String pass) {
        BaseStepsInterface.loginWithPassword(pass);
    }

    @Given("Login with username \"(.*)\" and password \"(.*)\"")
    public void loginWithUsernamePassword(String user, String pass) {
        BaseStepsInterface.loginWithUsernamePassword(user, pass);
    }

    @Given("Login with SMS OTP using username \"(.*)\" and password \"(.*)\"")
    public void loginWithSMSOTP(String user, String pass) {
        BaseStepsInterface.loginWithSMSOTP(user, pass);
    }

    @Given("Enter SMS OTP \"(.*)\"")
    public void enterSMSOTP(String otp) {
        BaseStepsInterface.enterSMSOTP(otp);
    }

    @Given("Click on (first |second |third )?\"(.*)\"")
    public void clickOn(String indexStr, String buttonOrLinkText) throws Exception {
        BaseStepsInterface.clickOn(indexStr, buttonOrLinkText);
    }

    @Given("Tile \"(.*)\" must be selected")
    public void checkTile(String name) throws Exception {
        BaseStepsInterface.checkTile(name);
    }

    @Given("Table Columns must be visible")
    public void checkTableColumns() throws Exception {
        BaseStepsInterface.checkTableColumns();
    }

    @Given("If checkbox \"(.*)\" is not checked, check it")
    public void checkUncheckedBox(String name) {
        BaseStepsInterface.checkUncheckedBox(name);
    }


    @Given("Select \"(.*)\" should be visible")
    public void selectDropDownVisible(String id) throws Exception {
        BaseStepsInterface.selectDropDownVisible(id);
    }

    @Given("Logout")
    public void Logout() {
        BaseStepsInterface.Logout();
    }

    @Given("Click on (mail|language|user) icon")
    public void clickOnMailOrLanguageOrUserIcon(String icon) {
        BaseStepsInterface.clickOnMailOrLanguageOrUserIcon(icon);
    }

    @Given("Click on (home|bank) icon")
    public void clickOnHomeOrBankIcon(String home) {
        BaseStepsInterface.clickOnHomeOrBankIcon(home);
    }

    @Given("No data must be available")
    public void checkNoDataAvailable() {
        BaseStepsInterface.checkNoDataAvailable();
    }

    @Given("In the (\\d\\w+ )?input field with the label \"(.*)\" enter text \"(.*)\"( and press the ENTER key)?")
    public void enterTextInputField(String index, String label, String text, String pressEnterKey) throws Exception {
        BaseStepsInterface.enterTextInputField(index, label, text, pressEnterKey);
    }

    @Given("In the (\\d\\w+ )?autocomplete field with the label \"(.*)\" enter text \"(.*)\" and select (\\d\\w+ )?entry")
    public void enterAutocompleteField(String index, String label, String text, String entryIndex) throws Exception {
        BaseStepsInterface.enterAutocompleteField(index, label, text, entryIndex);
    }

    @Given("In the (\\d\\w+ )?input field with name \"(.*)\" enter text \"(.*)\"( and press the ENTER key)?")
    public void enterTextInputFieldWithName(String index, String name, String text, String pressEnterKey) throws Exception {
        BaseStepsInterface.enterTextInputFieldWithName(index, name, text, pressEnterKey);
    }

    @Given("In the dropdown with the label \"(.*)\" select value \"(.*)\"")
    public void selectBox(String label, final String selection) throws Exception {
        BaseStepsInterface.selectBox(label, selection);
    }


    @Given("In the dropdown \"(.*)\" select text \"(.*)\"")
    public void selectByLabel(String selectName, String label) {
    BaseStepsInterface.selectByLabel(selectName, label);
    }

    //EB-18473 don't just change this regex, there are 7 functions like this (search for @Given("In the section)
    //if there's a problem a real soulution must be found
    @Given("In the section \"([^\"]*)\" the following fields must be present and contain a value: \"(.*)\"")
    public void checkSectionFieldsContainsValue(String section, String fieldsCSV) {
        BaseStepsInterface.checkSectionFieldsContainsValue(section, fieldsCSV);
    }

    //EB-18473 don't just change this regex, there are 7 functions like this (search for @Given("In the section)
    //if there's a problem a real soulution must be found
    @Given("In the section \"([^\"]*)\", subsection \"([^\"]*)\" the value of \"([^\"]*)\" must be \"(.*)\"")
    public void checkSectionSubsectionFieldValue(String section, String subsection, String field, String expectedValue) {
        BaseStepsInterface.checkSectionSubsectionFieldValue(section, subsection, field, expectedValue);
    }

    //EB-18473 don't just change this regex, there are 7 functions like this (search for @Given("In the section)
    //if there's a problem a real soulution must be found
    @Given("In the section \"([^\"]*)\" the value of \"([^\"]*)\" must be \"(.*)\"")
    public void checkSectionFieldValue(String section, String field, String expectedValue) {
        BaseStepsInterface.checkSectionFieldValue(section, field, expectedValue);
    }

    @Given("In the section \"([^\"]*)\" the (\\d+) value of \"([^\"]*)\" must be \"(.*)\"")
    public void checkSectionFieldValueWithIndex(String section, String index, String field, String expectedValue) {
        BaseStepsInterface.checkSectionFieldValueWithIndex(section, index, field, expectedValue);
    }

    //EB-18473 don't just change this regex, there are 7 functions like this (search for @Given("In the section)
    //if there's a problem a real soulution must be found
    @Given("In the section \"([^\"]*)\" the selected currency of \"(.*)\" must be \"(.*)\"")
    public void checkSectionCurrencyFieldValue(String section, String field, String expectedValue) {
        BaseStepsInterface.checkSectionCurrencyFieldValue(section, field, expectedValue);
    }

    @Given("In the row (\\d+), only the following links must be present: \"(.*)\"")
    public void checkLinksInSection(String row, String fields) {
        BaseStepsInterface.checkLinksInSection(row, fields);
    }

    //EB-18473 don't just change this regex, there are 7 functions like this (search for @Given("In the section)
    //if there's a problem a real soulution must be found
    @Given("In the section \"([^\"]*)\", row (\\d+), only the following links must be present: \"(.*)\"")
    public void checkLinksInSection(String section, String row, String fields) {
        BaseStepsInterface.checkLinksInSection(section, row, fields);
    }

    @Given("In the section \"([^\"]*)\", only the following links must be present: \"(.*)\"")
    public void checkLinkButtonInSection(String section, String fields) {
        BaseStepsInterface.checkLinkButtonInSection(section, fields);
    }

    @Given("Extend section \"(.*)\"")
    public void toggleSection(String section) {
        BaseStepsInterface.toggleSection(section);
    }

    @Given("^Add einvoice item with values: \"(.*)\"$")
    public void addEinvoiceItem(String itemValues) throws Exception {
        BaseStepsInterface.addEinvoiceItem(itemValues);
    }

    @Given("(Select|Deselect) checkbox \"(.*)\"")
    public void selectCheckbox(String action, String label) {
        BaseStepsInterface.selectCheckbox(action, label);
    }

    @Given("Is checkbox \"(.*)\" (selected|deselected)")
    public void isCheckboxSelected(String label, String action) {
        BaseStepsInterface.isCheckboxSelected(label, action);
    }

    @Given("In the row (\\d+), the value of \"(.*)\" must be \"(.*)\"")
    public void checkShortListValue(String row, String label, String value) {
        BaseStepsInterface.checkShortListValue(row, label, value);
    }

    //EB-18473 don't just change this regex, there are 7 functions like this (search for @Given("In the section)
    //if there's a problem a real soulution must be found
    @Given("In the section \"([^\"]*)\", row (\\d+), the value of \"(.*)\" must be \"(.*)\"")
    public void checkShortListValue(String section, String row, String label, String value) {
        BaseStepsInterface.checkShortListValue(section, row, label, value);
    }

    @Given("In the row (\\d+), title must be \"(.*)\", and data below: \"(.*)\"")
    public void checkDataModel(String row, String title, String data) {
        BaseStepsInterface.checkDataModel(row, title, data);
    }

    //EB-18473 don't just change this regex, there are 7 functions like this (search for @Given("In the section)
    //if there's a problem a real soulution must be found
    @Given("In the section \"([^\"]*)\", row (\\d+), title must be \"(.*)\", and data below: \"(.*)\"")
    public void checkDataModel(String section, String row, String title, String data) {
        BaseStepsInterface.checkDataModel(section, row, title, data);
    }

    @Given("In the row (\\d+), click on \"(.*)\"")
    public void clickShortListAction(String row, String action) {
        BaseStepsInterface.clickShortListAction(row, action);
    }

    @Given("Select radio button \"(.*)\"")
    public void selectRadioButton(String label) {
        BaseStepsInterface.selectRadioButton(label);
    }

    @Given("^Is radio button \"(.*)\" (selected|deselected)$")
    public void isRadioButtonSelected(String label, String action) {
        BaseStepsInterface.isRadioButtonSelected(label, action);
    }

    // TODO remove this command and use findVisibleElement in isRadioButtonSelected(String label, String action)
    @Given("Is radio button \"(.*)\" (selected|deselected) on the modal \"(.*)\"")
    public void isRadioButtonSelected(String label, String action, String modal) {
        BaseStepsInterface.isRadioButtonSelected(label, action, modal);
    }

    @Given("Select account \"(.*)\"")
    public void selectAccount(String account) {
        BaseStepsInterface.selectAccount(account);
    }

    @Given("Select client \"(.*)\"")
    public void selectClient(String client) {
        BaseStepsInterface.selectClient(client);
    }

    @Given("The following tabs must be present in the current page: \"(.*)\"")
    public void areTabsPresent(String tabs) {
        BaseStepsInterface.areTabsPresent(tabs);
    }

    @Given("In the row (\\d+) only the following row actions must be present: \"(.*)\"")
    public void checkActionButtonsInTableRow(String rowNumber, String actionsCSV) {
        BaseStepsInterface.checkActionButtonsInTableRow(rowNumber, actionsCSV);
    }

    @Given("Only the following fields must be available in filter: \"(.*)\"")
    public void checkFilterFields(String fields) {
        BaseStepsInterface.checkFilterFields(fields);
    }

    @Given("In the dropdown with the label \"(.*)\" the following fields must be available: \"(.*)\"")
    public void checkDropdownFields(String label, final String input) {
        BaseStepsInterface.checkDropdownFields(label, input);
    }

    @Given("The following message must be displayed: \"(.*)\"")
    public void checkMessage(String message) {
        BaseStepsInterface.checkMessage(message);
    }

    @Given("The row (\\d+), column \"(.*)\" of the table must contain \"(.*)\"")
    public void checkTableContentsByColumnName(String row, String columnName, String text) {
        BaseStepsInterface.checkTableContentsByColumnName(row, columnName, text);
    }

    @Given("In the row (\\d+) of the table, select the action \"(.*)\"")
    public void clickOnActionInTableRow(String row, String action) {
        BaseStepsInterface.clickOnActionInTableRow(row, action);
    }

    @Given("Select rows \"(.*)\"")
    public void selectTableRows(String rowsCSV) throws Exception {
        BaseStepsInterface.selectTableRows(rowsCSV);
    }

    @Given("Select all rows")
    public void selectAllTableRows() {
        BaseStepsInterface.selectAllTableRows();
    }

    @Given("Only the following columns must be present in the table: \"(.*)\"")
    public void checkVisibleColumnsInTable(String columnsCSV) {
        BaseStepsInterface.checkVisibleColumnsInTable(columnsCSV);
    }

    @Given("The following table columns must contain sorting functionality: \"(.*)\"")
    public void checkColumnsSortingEnabled(String columnsCSV) {
        BaseStepsInterface.checkColumnsSortingEnabled(columnsCSV);
    }

    @Given("The error message: \"(.*)\" must appear under the input field with the label \"(.*)\"")
    public void checkForFormErrorMessage(String message, String label) {
        BaseStepsInterface.checkForFormErrorMessage(message, label);
    }

    @Given("Only the following outer buttons must be present above the (long|short) table: \"(.*)\"")
    public void checkOuterButtonsLongTable(String table, String input) {
        BaseStepsInterface.checkOuterButtonsLongTable(table, input);
    }

    @Given("Only the following buttons must be present at the top of (long|short) table: \"(.*)\"")
    public void checkButtonLongTable(String table, String input) {
        BaseStepsInterface.checkButtonLongTable(table, input);
    }

    @Given("Only the following buttons must be present at the top of details page: \"(.*)\"")
    public void checkButtonsDetailsPage(String input) {
        BaseStepsInterface.checkButtonsDetailsPage(input);
    }

    @Given("Only the following buttons must be present at the top of the page: \"(.*)\"")
    public void checkButtonsPage(String input) {
        BaseStepsInterface.checkButtonsOnPage(input);
    }

    @Given("Upload file \"(.*)\"")
    public void uploadFile(String relativePath) throws Exception {
        BaseStepsInterface.uploadFile(relativePath);
    }

    @Given("Breadcrumbs must be: \"(.*)\"")
    public void checkBreadcrumbs(String breadcrumbs) {
        BaseStepsInterface.checkBreadcrumbs(breadcrumbs);
    }

    @Given("Submenu must have options: \"(.*)\"")
    public void matchSubMenu(String subMenuOptions) {
        BaseStepsInterface.matchSubMenu(subMenuOptions);
    }

    @Given("Must visible: \"(.*)\"")
    public void mustVisible(String text) {
        BaseStepsInterface.mustVisible(text);
    }


    @Given("Title must be: \"(.*)\"")
    public void checkPageTitle(String title) {
        BaseStepsInterface.checkPageTitle(title);
    }

    @Given("Modal window title must be: \"(.*)\"")
    public void checkModalWindowTitle(String title) {
        BaseStepsInterface.checkModalWindowTitle(title);
    }

    @Given("Modal window alert must be: \"(.*)\"")
    public void checkModalWindowAlert(String message) {
        BaseStepsInterface.checkModalWindowAlert(message);
    }

    @Given("Modal window message must be: \"(.*)\"")
    public void checkModalWindowMessage(String message) {
        BaseStepsInterface.checkModalwindowMessage(message);
    }

    /**
     * @param notOperator use string not or leave blank
     * @param page        supported login|home
     */
    @Given("^Page must (not )?be \"(login|home|multibank home)\"")
    public void checkIsSpecificPage(String notOperator, String page) {
        BaseStepsInterface.checkIsSpecificPage(notOperator, page);
    }

    @Given("Set custom date range: \"(.*)\" to \"(.*)\"")
    public void setCustomDateRange(String dateFrom, String dateTo) throws Exception {
        BaseStepsInterface.setCustomDateRange(dateFrom, dateTo);
    }

    @Given("Set custom date month: \"(.*)\"")
    public void setCustomDate(String dateFrom) throws Exception {
        BaseStepsInterface.setCustomDate(dateFrom);
    }

    @Given("Get client \"(.*)\"")
    public void getClient(String clientId) throws Exception {
        BaseStepsInterface.getClient(clientId);
    }

    @Given("Create client of type \"(SINGLEUSER|MULTIUSER|MULTISIGNER)\" with name \"(.*)\" and account \"(.*)\"")
    public void createClient(String userType, String name, String accountId) throws Exception {
        BaseStepsInterface.createClient(userType, name, accountId);
    }

    @Given("Remove client with account \"(.*)\"")
    public void removeClient(String contractId) throws Exception {
        BaseStepsInterface.removeClient(contractId);
    }

    @Given("Add licence for client with account \"([^\"]*)\"( for trusteeId \"(\\d+)\")?")
    public void addLicence(String account, String ignored, Integer trusteeId) throws Throwable {
        BaseStepsInterface.addLicence(account, ignored, trusteeId);
    }

    @Given("Add permission \"(READ|PREPARE|SIGN)\" for document type \"([^\"]*)\" on account \"([^\"]*)\"")
    public void addPermissionForDocumentTypeOnAccount(String permission, String documentType, String account) throws Throwable {
        BaseStepsInterface.addPermissionForDocumentTypeOnAccount(permission, documentType, account);
    }

    @Given("Remove permission \"(READ|PREPARE|SIGN)\" for document type \"([^\"]*)\" on account \"([^\"]*)\"")
    public void removePermissionForDocumentTypeOnAccount(String permission, String documentType, String account) throws Throwable {
        BaseStepsInterface.removePermissionForDocumentTypeOnAccount(permission, documentType, account);
    }

    @Given("Create doc of type \"(.*)\" for account \"(.*)\"")
    public void createDocument(String document, String account, DataTable dataTable) throws Throwable {
        BaseStepsInterface.createDocument(document, account, dataTable);
    }

    @Given("Graph must exist$")
    public void checkGraphExists() {
        BaseStepsInterface.checkGraphExists();
    }

    @Given("Click page number (\\d+)")
    public void clickPageNumber(String pageNumber) throws Exception {
        BaseStepsInterface.clickPageNumber(pageNumber);
    }

    @Given("Select filter value \"(.*)\"")
    public void selectFilterValue(String filterValue) throws Exception {
        BaseStepsInterface.selectFilterValue(filterValue);
    }

    @Given("Only the following search fields must be present: \"(.*)\"")
    public void checkSearchFields(String searchFields) {
        BaseStepsInterface.checkSearchFields(searchFields);
    }

    @Given("The total number of results must be: (\\d+)")
    public void checkNumberOfReturnedResults(String resultsNumber) {
        BaseStepsInterface.checkNumberOfReturnedResults(resultsNumber);
    }

    @Given("The number of returned pages must be: (\\d+)")
    public void checkNumberOfReturnPages(String pageNumberResult) {
        BaseStepsInterface.checkNumberOfReturnPages(pageNumberResult);
    }

    @Given("In the section \"(.*)\", row (\\d+), column \"(.*)\" must contain \"(.*)\"")
    public void checkSectionRowColumn(String section, String row, String column, String expectedValue) {
        BaseStepsInterface.checkSectionRowColumn(section, row, column, expectedValue);
    }

    @Given("The homepage button \"([^\"]*)\" must (not )?be enabled")
    public void checkHompageButtonLink(String buttonName, String type) {
        BaseStepsInterface.checkHompageButtonLink(buttonName, type);
    }

    @Given("Close alert window by \"(accepting|dismissing)\" it")
    public void closeAlert(String type) {
        BaseStepsInterface.closeAlert(type);
    }

    @Given("Switch to \"(modal|default)\" window")
    public void switchDriverWindowFocus(String windowType) {
        BaseStepsInterface.switchDriverWindowFocus(windowType);
    }


    //region appium-android
    @Given("Launch Android App")
    public void launch_android_app() {
        BaseStepsInterface.launch_android_app();
    }

    @Given("Press \"(.*)\"")
    public void clickAndroidElement(String locator) {
        BaseStepsInterface.clickAndroidElement(locator);
    }

    @Given("If present, Press \"(.*)\"")
    public void pressIfPresent(String locator) {
        BaseStepsInterface.pressIfPresent(locator);
    }

    @Given("Update credentials if needed, User: \\\"(.*)\\\"\"")
    public void updateCredentials(String user) {
        BaseStepsInterface.updateCredentials(user);
    }

    @Given("Login using pin \"(.*)\", using user \"(.*)\"")
    public void loginWithOrWithoutCredentials(String pin, String user) {
        BaseStepsInterface.loginWithOrWithoutCredentials(pin, user);
    }

    @Given("Back")
    public void back() {
        BaseStepsInterface.back();
    }

    @Given("Text \"(.*)\" should be present")
    public void textPresent(String text) {
        BaseStepsInterface.textPresent(text);
    }


    @Given("Logoff")
    public void logoutFromApp() {
        BaseStepsInterface.logoutFromApp();
    }

    @Given("In the list, press item (\\d+)")
    public void pressRowItem(int index) {
        BaseStepsInterface.pressRowItem(index);
    }

    @Given("Take Screenshot")
    public void androidScreenshot() {
        BaseStepsInterface.androidScreenshot();
    }

    @Given("Type \"(.*)\" into inputfield with text \"(.*)\"")
    public void typeIntoEditText(String inputText, String labelText) {
        BaseStepsInterface.typeIntoEditText(inputText, labelText);
    }

    @Given("Input \"(.*)\" into inputfield with text \"(.*)\"")
    public void inputIntoEditText(String inputText, String labelText) {
        BaseStepsInterface.inputIntoEditText(inputText, labelText);
    }


    //endregion

}