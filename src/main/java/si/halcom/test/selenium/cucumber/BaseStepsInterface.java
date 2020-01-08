package si.halcom.test.selenium.cucumber;

import cucumber.api.DataTable;
import cucumber.api.Scenario;

import java.io.IOException;

public interface BaseStepsInterface {

    String LANDING_PAGE_LOGIN = "login";

    void addEinvoiceItem(String itemValues) throws Exception;

    void addLicence(String account, String ignored, Integer trusteeId) throws Throwable;

    void addPermissionForDocumentTypeOnAccount(String permission, String documentType, String account) throws Throwable;

    void areTabsPresent(String tabs);

    void clickOn(String indexStr, String buttonOrLinkText) throws Exception;

    void clickOnActionInTableRow(String row, String action);

    void clickOnHomeOrBankIcon(String home);

    void clickOnMailOrLanguageOrUserIcon(String icon);

    void clickShortListAction(String row, String action);

    void checkActionButtonsInTableRow(String rowNumber, String actionsCSV);

    void checkBreadcrumbs(String breadcrumbs);

    void checkButtonLongTable(String table, String input);

    void checkButtonsDetailsPage(String input);

    void checkColumnsSortingEnabled(String columnsCSV);

    void checkDataModel(String row, String title, String data);

    void checkDataModel(String section, String row, String title, String data);

    void checkForFormErrorMessage(String message, String label);

    void checkIsSpecificPage(String notOperator, String page);

    void checkFilterFields(String fields);

    void checkLinksInSection(String row, String fields);

    void checkLinksInSection(String section, String row, String fields);

    void checkMessage(String message);

    void checkModalWindowTitle(String title);

    void checkNoDataAvailable();

    void checkOuterButtonsLongTable(String table, String input);

    void checkPageTitle(String title);

    void checkSectionCurrencyFieldValue(String section, String field, String expectedValue);

    void checkSectionFieldsContainsValue(String section, String fieldsCSV);

    void checkSectionFieldValue(String section, String field, String expectedValue);

    void checkSectionFieldValueWithIndex(String section, String index, String field, String expectedValue);

    void checkSectionSubsectionFieldValue(String section, String subsection, String field, String expectedValue);

    void checkShortListValue(String row, String label, String value);

    void checkShortListValue(String section, String row, String label, String value);

    void checkTableContentsByColumnName(String row, String columnName, String text);

    void checkVisibleColumnsInTable(String columnsCSV);

    void createClient(String userType, String name, String accountId) throws Exception;

    void createDocument(String document, String account, DataTable dataTable) throws Throwable;

    void enterAutocompleteField(String index, String label, String text, String entryIndex) throws Exception;

    void enterPin() throws Exception;

    void enterPin2() throws Exception;

    void enterSMSOTP(String otp);

    void enterTextInputField(String index, String label, String text, String pressEnterKey) throws Exception;

    void enterTextInputFieldWithName(String index, String name, String text, String pressEnterKey) throws Exception;

    void getClient(String clientId) throws Exception;

    void goToUrl(String url) throws Exception;

    void isCheckboxSelected(String label, String action);

    void isRadioButtonSelected(String label, String action);

    void isRadioButtonSelected(String label, String action, String modal);

    void loginWithMobileToken(String user, String pass);

    void loginWithPassword(String pass);

    void loginWithSMSOTP(String user, String pass);

    void loginWithUsernamePassword(String user, String pass);

    void Logout();

    void launchBrowser(String nexusPersonal) throws Exception;

    void removeClient(String contractId) throws Exception;

    void removePermissionForDocumentTypeOnAccount(String permission, String documentType, String account) throws Throwable;

    void selectAllTableRows();

    void selectAccount(String account);

    void selectCheckbox(String action, String label);

    void selectClient(String client);

    void selectBox(String label, String selection) throws Exception;

    void selectRadioButton(String label);

    void selectTableRows(String rowsCSV) throws Exception;

    void signDocument();

    void signWithOTP(String otp, String enter);

    void setCustomDateRange(String dateFrom, String dateTo) throws Exception;

    void takeScreenshot() throws Exception;

    void toggleSection(String section);

    void uploadFile(String relativePath) throws Exception;

    void waitMilliseconds(long millis);

    byte[] createScreenshot(Scenario scenario) throws Exception;

    void setScenario(Scenario scenario);

    void checkModalWindowAlert(String message);

    void checkModalwindowMessage(String message);

    void checkGraphExists();

    void clickPageNumber(String pageNumber);

    void selectFilterValue(String filterValue);

    void checkSearchFields(String searchFields);

    void checkButtonsOnPage(String input);

    void checkNumberOfReturnedResults(String resultsNumber);

    void checkNumberOfReturnPages(String pageNumberResult);

    void checkDropdownFields(String label, String input);

    void checkSectionRowColumn(String section, String row, String column, String expectedValue);

    void checkLinkButtonInSection(String section, String fields);

    void checkHompageButtonLink(String buttonName, String type);

    void setCustomDate(String dateFrom) throws Exception;

    void closeAlert(String type);

    void switchDriverWindowFocus(String windowType);

    void matchSubMenu(String subMenuOptions);

    void mustVisible(String text);

    void checkTile(String name);

    void checkTableColumns();

    public void selectDropDownVisible(String id);

    public void checkUncheckedBox(String name);

    public void selectByLabel(String selectName, String label);

    void launch_android_app();

    public void clickAndroidElement(String locator);

    public void updateCredentials(String user);

    public void loginWithOrWithoutCredentials(String pin, String user);

    public void back();

    public void textPresent(String text);

    public void logoutFromApp();

    public void pressRowItem(int index);

    public void pressIfPresent(String text);

    public void androidScreenshot();

	public void typeIntoEditText(String inputText, String labelText);

	public void inputIntoEditText(String inputText, String labelText);
}
