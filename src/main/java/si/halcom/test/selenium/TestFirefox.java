package si.halcom.test.selenium;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import si.halcom.test.selenium.cucumber.BaseStepDefinitions;

@Ignore("This is just for testing if browser is working as expected!")
public class TestFirefox {

	@BeforeClass
	public static void setupProfile() {
		System.setProperty("settings.profile", "haabsi.dev.chrome");
	}
	
	BaseStepDefinitions steps = new BaseStepDefinitions();
	
	@Test
	public void test() throws Exception {
		steps.launchBrowser(null);		
		steps.loginWithMobileToken("selenium7248", "000000");
		steps.takeScreenshot();
		Thread.sleep(20000);
	}
	
    /**
     * In case we need to clean up stuff after the tests are run.
     */
    @AfterClass
    public static void teardown() {
        SeleniumDriverHandler.shutDown();
    }

}
