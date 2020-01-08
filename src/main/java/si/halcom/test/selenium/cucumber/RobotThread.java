package si.halcom.test.selenium.cucumber;

import org.openqa.selenium.WebDriver;

import si.halcom.test.selenium.SeleniumDriverHandler;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * A AWT Robot that takes care of entering the PIN into the Nexus Personal Plugin-Plugout by simulating keystrokes.
 * The thread is needed since the selenium driver.get() method is blocking, so we need to create a robot
 * in alternate thread in order to enter pin for the certificate.
 *
 * For other uses just use the static robotEnterPin() method.
 *
 * Workflow for the RobotThread:
 * - wait 3 seconds
 * - enter PIN and press the ENTER key
 *
 * @author kris
 */
class RobotThread extends Thread {

    private final WebDriver driver;
    private Robot robot = null;
    private CertUsage certUsage;
    
    public enum CertUsage {
    	WITH_CERT,
    	WITHOUT_CERT
    }

    public RobotThread(CertUsage certUsage) {
    	this.certUsage = certUsage;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        this.driver = SeleniumDriverHandler.DRIVER;
        if (driver == null) {
            throw new IllegalStateException("FATAL: Selenium driver not available!");
        }
        start();
    }

    @Override
    public void run() {
        try {
            Thread.sleep(3000);//give some time for cert selection to pop up
            if(certUsage == CertUsage.WITH_CERT) {
            	//select default cert
            	robot.keyPress(KeyEvent.VK_ENTER);
            	robot.keyRelease(KeyEvent.VK_ENTER);
            	
            	robotEnterPin(robot);
            }else {
            	robot.keyPress(KeyEvent.VK_ESCAPE);
            	robot.keyRelease(KeyEvent.VK_ESCAPE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Simulates keystrokes (1,2,3,4,5,6 and ENTER) for entering PIN in the active window.
     * Use for entering the PIN in the Nexus Personal.
     *
     * @param robot the AWT Robot instance
     */
    static void robotEnterPin(Robot robot) throws Exception {
    	Thread.sleep(5000);//give some time for nexus to pop upp
    	
        robot.keyPress(KeyEvent.VK_1);
        robot.keyRelease(KeyEvent.VK_1);
        robot.keyPress(KeyEvent.VK_2);
        robot.keyRelease(KeyEvent.VK_2);
        robot.keyPress(KeyEvent.VK_3);
        robot.keyRelease(KeyEvent.VK_3);
        robot.keyPress(KeyEvent.VK_4);
        robot.keyRelease(KeyEvent.VK_4);
        robot.keyPress(KeyEvent.VK_5);
        robot.keyRelease(KeyEvent.VK_5);
        robot.keyPress(KeyEvent.VK_6);
        robot.keyRelease(KeyEvent.VK_6);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }
    
    static void robotSelectPlugoutAndEnterPin(Robot robot) throws Exception {
    	Thread.sleep(5000);
    	robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    	robotEnterPin(robot);
    }
}

