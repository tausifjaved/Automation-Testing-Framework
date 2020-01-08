package si.halcom.test.selenium;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;



/**
 * The settings manager class. Loads the settings from the settings.properties file and makes them
 * available for further use.
 * <p>
 * NOTE: you can override the settings file via a command line argument:
 * -Dsettings.path=PATH_TO_SETTINGS_FILE
 *
 * @author kris
 */

public class Settings {

    public enum BrowserEnum {
        Firefox, Chrome, IE11, Opera, Edge
    }
    
    private static final String SETTINGS_PATH = "settings.properties";
    private static final String SETTINGS_PATH_ADMIN = "admin-settings.properties";
    private static final String BROWSER = "BROWSER";
    private static final String HEADLESS = "HEADLESS";
    private static final String MOBILE_DEVICE = "MOBILE_DEVICE";
    private static final String USER_AGENT = "USER_AGENT";
    private static final String TEST_EXECUTION_SPEED = "TEST_EXECUTION_SPEED";
    private static final String SERVER_URI = "SERVER_URI";
    private static final String DATE_FORMAT_PATTERN = "DATE_FORMAT_PATTERN";
    private static final String CLICK_DELAY = "CLICK_DELAY";
    private static final String AUTOSELECT_CERTIFICATE = "AUTOSELECT_CERTIFICATE";
    private static final String KEEP_BROWSER_OPEN_BETWEEN_TESTS = "KEEP_BROWSER_OPEN_BETWEEN_TESTS";
    private static final String FIREFOX_EXECUTABLE = "FIREFOX_EXECUTABLE";
    private static final String FIREFOX_MARIONETTE = "FIREFOX_MARIONETTE";
    private static final String FIREFOX_PROFILE = "FIREFOX_PROFILE";
    private static final String VERBOSE_LOGS = "VERBOSE_LOGS";
    private static final String HEALTH_CHECK_TIMEOUT_IN_MINUTES = "HEALTH_CHECK_TIMEOUT_IN_MINUTES";
    private static final String SCREENSHOT_WITH_ASHOT = "SCREENSHOT_WITH_ASHOT";

    public static int HEALTH_CHECK_INTERVAL = 30000;

    private static final String TRANS_ENABLED = "TRANSLATIONS_ENABLED";
    private static final String TRANS_LANGUAGE = "TRANSLATIONS_LANGUAGE";
    private static final String TRANS_MBP_SERVER_URI = "TRANSLATIONS_MBP_SERVER_URI";
    private static final String TRANS_BRANCH = "TRANSLATIONS_BRANCH";
    private static final String TRANS_BANK_SYSTEM_ID = "TRANSLATIONS_BANK_SYSTEM_ID";

    private static final String DATA_SETUP_URI = "DATA_SETUP_URI";
    private static final String CERTIFICATE = "CERTIFICATE";

    private static Properties properties;

    public static void initSettings() {
        String environmentType = (System.getProperty("testEnvironment.profile") == null) ? "" : System.getProperty("testEnvironment.profile");
        properties = new Properties();

        if ("true".equals(System.getProperty("isApplicationExeJar"))) {
        	File settingsPath; 
        	if(environmentType.toLowerCase().equals("admin")) {
        		settingsPath = new File("settings" + File.separator + SETTINGS_PATH_ADMIN);
                System.out.println("Reading settings from JAR location, folder:" + settingsPath);
        	} else {
        		settingsPath = new File("settings" + File.separator + SETTINGS_PATH);
                System.out.println("Reading settings from JAR location, folder:" + settingsPath);
        	}
            
            loadSettings(settingsPath.getAbsolutePath());
            settingsPath = null;
        } else {
        	System.out.println("Reading default settings in project");
        	if (environmentType.toLowerCase().equals("admin")) {
                loadSettings(SETTINGS_PATH_ADMIN);
            } else {
                loadSettings(SETTINGS_PATH);
            }

        }
    }

    private static void loadSettings(String settingsPath) throws RuntimeException {
    	if("true".equals(System.getProperty("isApplicationExeJar"))) {
    		try (InputStream is = new FileInputStream(settingsPath)) {
    			properties.load(is);
                return;
    		} catch (Exception ex) {
                throw new RuntimeException("Could not access the settings file: [" + settingsPath + "].", ex);
            }
    	}
    
    	try (InputStream is = new ClassPathResource(settingsPath).getInputStream()) {
            properties.load(is);
            return;
        } catch (Exception ex) {
            throw new RuntimeException("Could not access the settings file: [" + settingsPath + "].", ex);
        }
    }

    private static String getSetting(String key) throws RuntimeException {
        return getSetting(key, null);
    }

    private static String getSetting(String key, String defaultValue) throws RuntimeException {
        String sysValue = System.getProperty(key);
        if (sysValue != null) {
            return sysValue;
        }
        String propValue = properties.getProperty(key);
        if (propValue != null) {
            return propValue;
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new IllegalArgumentException("No setting was found for key = [" + key + "]");
    }

    public static BrowserEnum getBrowser() {
        String browser = getSetting(BROWSER);
        return BrowserEnum.valueOf(browser);
    }


    public static String getTestExecutionSpeed() {
        return getSetting(TEST_EXECUTION_SPEED);
    }

    public static boolean isHeadless() {
        String headless = getSetting(HEADLESS);

        if (headless.equalsIgnoreCase("True")) {
            return true;
        }
        return false;
    }

    public static String getMobileDevice() {
        String device = getSetting(MOBILE_DEVICE);

        return device;
    }

    public static String getUserAgent() {
        String ua = getSetting(USER_AGENT);

        return ua;
    }

    public static String getServerUri() {
        String serverUri = getSetting(SERVER_URI);
        //normalize server uri
        //server uri must end with /home for KEEP_BROWSER_OPEN_BETWEEN_TESTS to work
        //or repeated logins will occour
        if (Settings.isBrowserOpenBetweenTests()) {

            if (serverUri.contains("auth") || serverUri.contains("login")) {
                throw new IllegalArgumentException("Invalid server URI setting, must not contain auth or login, must end with home");
            }

            if (!serverUri.endsWith("/home")) {
                if (!serverUri.endsWith("/")) {
                    serverUri += "/";
                }
                serverUri += "home";
            }
        }
        return serverUri;
    }

    public static String getMultiBankUri(String key) {
        return getSetting(key.replaceAll("\\{", "").replaceAll("\\}", ""));
    }

    public static String getDataSetupUri() {
        return getSetting(DATA_SETUP_URI);
    }

    public static String getCertificate() {
        return getSetting(CERTIFICATE);
    }

    public static String getDateFormatPattern() {
        return getSetting(DATE_FORMAT_PATTERN);
    }

    public static boolean isAutoSelectCertificate() {
        return "true".equalsIgnoreCase(getSetting(AUTOSELECT_CERTIFICATE));
    }

    public static int getClickDelay() {
        try {
            return Integer.parseInt(getSetting(CLICK_DELAY));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not convert setting CLICK_DELAY to integer", e);
        }
    }

    public static boolean areTranslationsEnabled() {
        return Boolean.valueOf(getSetting(TRANS_ENABLED));
    }

    public static String getTranslationLanguage() {
        return getSetting(TRANS_LANGUAGE);
    }

    public static String getTranslationsMBPServerUri() {
        return getSetting(TRANS_MBP_SERVER_URI);
    }

    public static String getTranslationsBranch() {
        return getSetting(TRANS_BRANCH);
    }

    public static String getTranslationsBankSystemId() {
        return getSetting(TRANS_BANK_SYSTEM_ID);
    }

    /**
     * @return boolean return if firefox is newer than 45
     */
    public static boolean isFFMarionette() {
        return "true".equalsIgnoreCase(getSetting(FIREFOX_MARIONETTE, "false"));
    }

    /**
     * @return String path to FF executable from settings or null
     */
    public static String getFFExecutable() {
        String ffExec = getSetting(FIREFOX_EXECUTABLE);
        if (ffExec != null) {
            ffExec = ffExec.trim();
        }
        if ("".equals(ffExec)) {
            ffExec = null;
        }

        return ffExec;
    }

    /**
     * @return String path to FF profile folder from settings or null
     */
    public static String getFFEProfile() {
        String ffProfile = getSetting(FIREFOX_PROFILE);
        if (ffProfile != null) {
            ffProfile = ffProfile.trim();
        }
        if ("".equals(ffProfile)) {
            ffProfile = null;
        }

        return ffProfile;
    }

    public static boolean isBrowserOpenBetweenTests() {
        return Boolean.valueOf(getSetting(KEEP_BROWSER_OPEN_BETWEEN_TESTS));
    }

    public static boolean isVerboseLogs() {
        return Boolean.valueOf(getSetting(VERBOSE_LOGS));
    }

    public static boolean isScreenshotWithAShot() {
        return Boolean.valueOf(getSetting(SCREENSHOT_WITH_ASHOT, "false"));
    }

    public static String getServerHealthckeckUrl() {
        String urlString = Settings.getServerUri();
        if (urlString.endsWith("/home")) {
            urlString = urlString.replace("/home", "/status");
        }
        return urlString;
    }


    /**
     * @return if healthcheck is enabled or not
     */
    public static boolean isHealthCheck() {
        return !"".equals(getSetting(HEALTH_CHECK_TIMEOUT_IN_MINUTES)) && !"0".equals(getSetting(HEALTH_CHECK_TIMEOUT_IN_MINUTES));
    }

    /**
     * @return health check timeout in minutes
     */
    public static int getHealthCheckTimeout() {
        if (isHealthCheck()) {
            Integer value = Integer.parseInt(getSetting(HEALTH_CHECK_TIMEOUT_IN_MINUTES));
            if (value < 0) {
                value = value * -1;
            }
            return value;
        }
        return 0;
    }

}
