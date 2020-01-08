package si.halcom.test.selenium;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.core.io.ClassPathResource;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;

import javax.swing.*;


/**
 * @author kris
 */
public class SeleniumDriverHandler {
    public static WebDriver DRIVER;

    public static FluentWait<WebDriver> WAIT;

    // wait timeout when searching for web elements in seconds
    private static int WAIT_FOR_ELEMENT_TIMEOUT;

    public static void initializeDriver() {
        final Settings.BrowserEnum browser = Settings.getBrowser();
        switch (browser) {
            case Firefox:
                configureFirefoxDriver();
                break;
            case Chrome:
                configureChromeDriver();
                break;
            case IE11:
                configureIEDriver();
                break;
            case Opera:
                configureOperaDriver();
                break;
            case Edge:
                configureEdgeDriver();
                break;
            default:
                throw new IllegalStateException("FATAL: Browser not supported!");
        }
        String EXECUTION_SPEED = Settings.getTestExecutionSpeed();
        if (EXECUTION_SPEED.equalsIgnoreCase("fast")) {
            WAIT_FOR_ELEMENT_TIMEOUT = 5;
        } else if (EXECUTION_SPEED.equalsIgnoreCase("normal")) {
            WAIT_FOR_ELEMENT_TIMEOUT = 10;
        } else if (EXECUTION_SPEED.equalsIgnoreCase("slow")) {
            WAIT_FOR_ELEMENT_TIMEOUT = 20;
        } else {
            throw new IllegalStateException("Wrong test speed defined in settings.properties: [" + EXECUTION_SPEED + "]. Available speed settings are: slow, normal, fast.");
        }
        WAIT = new WebDriverWait(DRIVER, WAIT_FOR_ELEMENT_TIMEOUT, 2000);
    }

    private static void configureFirefoxDriver() {
        //region OldCode
        //        if (Settings.isFFMarionette()) {
//            // use new firefox geckodriver.exe for firefox from 47 upwards
//        	System.setProperty("webdriver.gecko.driver", setWebDriverPath("geckodriver.exe"));
//        } else {
//            // use old firefox webdriver (firefox 45)
//            System.setProperty("webdriver.firefox.marionette", "false");
//        }
//
//        DesiredCapabilities dc = DesiredCapabilities.firefox();
//        dc.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
//
//        FirefoxProfile profile;
//        String ffExe = Settings.getFFExecutable();
//        String ffProfile = Settings.getFFEProfile();
//
//        if (ffExe != null) {
//
//            File firefox = new File(ffExe); // path to portable firefox .exe
//            // file
//            FirefoxBinary ffbin = new FirefoxBinary(firefox);
//            dc.setCapability(FirefoxDriver.BINARY, ffbin);
//
//        }
//        // if profile is passed as sys variable use this profile
//        if (ffProfile != null) {
//            profile = new FirefoxProfile(new File(ffProfile));
//        } else {
//            ProfilesIni allProfiles = new ProfilesIni();
//            profile = allProfiles.getProfile("default");
//            if (profile == null) {
//                throw new IllegalStateException("Firefox does not contain the the needed profile named \"default\". Please fix!");
//            }
//        }
//
//        profile.setEnableNativeEvents(true);
//        if (Settings.isAutoSelectCertificate()) {
//            profile.setPreference("security.default_personal_cert", "Select Automatically");
//        }
//        dc.setCapability(FirefoxDriver.PROFILE, profile);
//        DRIVER = new FirefoxDriver(dc);
        //endregion


        File geckoFolder = new File(System.getProperty("user.home") + "\\.m2\\repository\\webdriver\\geckodriver");
        if (!geckoFolder.exists()) {
            WebDriverManager.firefoxdriver().setup();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            WebDriverManager.firefoxdriver().setup();
        }

        String path = null;
        File[] directories = new File(System.getProperty("user.home") + "\\.m2\\repository\\webdriver\\geckodriver\\win64\\").listFiles(File::isDirectory);
        Arrays.sort(directories);
        path = System.getProperty("user.home") + "\\.m2\\repository\\webdriver\\geckodriver\\win64\\" + directories[directories.length - 1] + "\\geckodriver.exe";


        System.setProperty("webdriver.gecko.driver", path.toString());
        WebDriverManager.firefoxdriver().setup();
        DRIVER = new FirefoxDriver();
        configureCommonDriverProperties();


    }

    private static void configureEdgeDriver() {


        WebDriverManager.edgedriver().setup();
        DesiredCapabilities dc = DesiredCapabilities.edge();
        dc.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
        dc.setCapability(CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION, true);

        DRIVER = new EdgeDriver(dc);
        configureCommonDriverProperties();
    }


    private static void configureChromeDriver() {
        System.setProperty("webdriver.chrome.driver", setWebDriverPath("chromedriver.exe"));

        DesiredCapabilities dc = DesiredCapabilities.chrome();
        //dc.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
        dc.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.DISMISS);
        //dc.setCapability(CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION, true);

        //System.setProperty("HEADLESS", "TRUE");

        ChromeOptions chromeOptions = new ChromeOptions();

        if (Settings.isHeadless()) {
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("disable-gpu");
            chromeOptions.addArguments("window-size=1980,1080");
            dc.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
        }

        Map<String, Object> chromeOptionz = new HashMap<String, Object>();

        if (!Settings.getMobileDevice().equalsIgnoreCase("NULL")) {
            Map<String, String> mobileEmulation = new HashMap<String, String>();

            mobileEmulation.put("deviceName", Settings.getMobileDevice());
            chromeOptionz.put("mobileEmulation", mobileEmulation);
//            if(Settings.isHeadless()) {
//                chromeOptions.addArguments("--no-sandbox");
//                chromeOptions.addArguments("--headless");
//                chromeOptions.addArguments("disable-gpu");
//                chromeOptions.addArguments("window-size=1980,1080");
//                chromeOptions.setExperimentalOption("prefs",chromeOptionz);
//                dc.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
//            }
//            else
//            {
            dc.setCapability(ChromeOptions.CAPABILITY, chromeOptionz);
//            }

        }

        if (!Settings.getUserAgent().equalsIgnoreCase("null")) {

            //Map<String, Object> deviceMetrics = new HashMap<>();
            //deviceMetrics.put("width", 1078);
            //deviceMetrics.put("height", 924);

//            deviceMetrics.put("pixelRatio", 3.0);
//            Map<String, Object> mobileEmulation = new HashMap<>();
//            mobileEmulation.put("deviceName", Settings.getMobileDevice());
//            mobileEmulation.put("deviceMetrics", deviceMetrics);
//            mobileEmulation.put("userAgent",Settings.getUserAgent());
//            chromeOptions.setExperimentalOption("mobileEmulation",mobileEmulation);

            chromeOptions.addArguments("--user-agent=" + "" + Settings.getUserAgent() + "");
            Map<String, String> mobileEmulation = new HashMap<String, String>();
            mobileEmulation.put("deviceName", Settings.getMobileDevice());
            chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
            dc.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
        }
        DRIVER = new ChromeDriver(dc);
        configureCommonDriverProperties();
    }

    private static void configureIEDriver() {
        WebDriverManager.iedriver().setup();
        //System.setProperty("webdriver.ie.driver", setWebDriverPath("IEDriverServer.exe"));
        System.setProperty("webdriver.ie.driver.silent", "true");
        DesiredCapabilities dc = DesiredCapabilities.internetExplorer();
        dc.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
        dc.setCapability(CapabilityType.ForSeleniumServer.ENSURING_CLEAN_SESSION, true);
        dc.setCapability("ignoreZoomSetting", true);
        dc.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);

        InternetExplorerOptions ieOptions = new InternetExplorerOptions();
        ieOptions.disableNativeEvents();
        ieOptions.destructivelyEnsureCleanSession();

        //ieOptions.merge(dc);

        DRIVER = new InternetExplorerDriver();
        configureCommonDriverProperties();
    }

    private static void configureOperaDriver() {
        WebDriverManager.operadriver().setup();
        OperaOptions options = new OperaOptions();
        File[] directories = new File(System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Opera\\").listFiles(File::isDirectory);
        Arrays.sort(directories, Comparator.comparingLong(File::lastModified));
        options.setBinary(directories[0] + "\\opera.exe");

        DRIVER = new OperaDriver(options);
        configureCommonDriverProperties();
    }


    private static String setWebDriverPath(String webDriverName) {
        //in case run from JAR the location of drivers should be on the same level as the JAR in folder /drivers
        if ("true".equals(System.getProperty("isApplicationExeJar"))) {
            File webDriver = new File("drivers" + File.separator + webDriverName);
            return webDriver.getAbsolutePath();
        }

        Path path = null;
        try {
            path = Paths.get(new ClassPathResource(webDriverName).getURI());
        } catch (Exception e) {
            throw new IllegalStateException("FATAL: problem obtaining path to " + webDriverName + " " + e.toString());
        }
        return path.toString();
    }


    private static void configureCommonDriverProperties() {
        DRIVER.manage().timeouts().implicitlyWait(WAIT_FOR_ELEMENT_TIMEOUT, TimeUnit.SECONDS);
        DRIVER.manage().timeouts().setScriptTimeout(WAIT_FOR_ELEMENT_TIMEOUT, TimeUnit.SECONDS);
        // some more settings if needed...
        DRIVER.manage().window().maximize();
        // CucumberTest.driver.manage().timeouts().pageLoadTimeout(1, TimeUnit.SECONDS);
        // Sometimes first one is not working with latest selenium framework
        DRIVER.manage().timeouts().setScriptTimeout(5, TimeUnit.SECONDS);
    }

    public static void shutDown() {
        if (DRIVER != null) {
            DRIVER.quit();
            DRIVER = null;
        }
    }

    /**
     * Instructs the driver to go to the default page, as defined in settings via SERVER_URI
     */
    public static void gotoDefaultServerURI() {
        DRIVER.get(Settings.getServerUri());
    }

    /**
     * Instruct the driver to open the given page
     *
     * @param uri
     */
    public static void gotoURI(String uri) {
        DRIVER.get(uri);
    }

    public static boolean isDriverInitialized() {
        return DRIVER != null;
    }

    /**
     * You can temporary set the implicit wait timeout for performance reasons, but make sure
     * you restore it back (in the finally block) by invoking the restoreDefaultTimeout() method!
     *
     * @param time
     * @param timeUnit
     */
    public static void setTimeout(long time, TimeUnit timeUnit) {
        DRIVER.manage().timeouts().implicitlyWait(time, timeUnit);
    }

    /**
     * If you have set a custom timeout, this is the method that restores the default one
     */
    public static void restoreDefaultTimeout() {
        DRIVER.manage().timeouts().implicitlyWait(WAIT_FOR_ELEMENT_TIMEOUT, TimeUnit.SECONDS);
    }
}
