package si.halcom.test.selenium;


import org.junit.AfterClass;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import si.halcom.test.selenium.cucumber.BaseStepAdmin;
import si.halcom.test.selenium.cucumber.BaseStepRWD;
import si.halcom.test.selenium.cucumber.BaseStepsInterface;


/**
 * The JUnit Cucumber Runner class.
 * The JUnit runner uses the JUnit framework to run Cucumber (@RunWith annotation).
 * You can run this test in the same way you run your other JUnit tests, using your IDE or your build tool (for example mvn test).
 *
 * Configuration options can also be overridden and passed to any of the runners via the cucumber.options Java
 * system property. For example, if you are using Maven and want to run a subset of scenarios tagged with @smoke:
 *
 * mvn test -Dcucumber.options="--tags @smoke"
 *
 * To print out all the available configuration options, simply pass the --help option. For example:
 *
 * mvn test -Dcucumber.options="--help"
 *
 * For more documentation regarding Cucumber, visit the following page:
 * https://cucumber.io/docs/reference/jvm#java
 *
 * @author kris
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        format = {"pretty", "html:target/cucumber/", "junit:target/junit/junit.xml", "json:target/cucumber/cucumber.json"},
        monochrome = true)
@EnableAutoConfiguration(exclude = { HibernateJpaAutoConfiguration.class })
@ContextConfiguration
public class CucumberTest {
	@Autowired
	BaseStepsInterface baseStepsInterface;
	
	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(CucumberTest.class);
		builder.application().setWebEnvironment(false);
		builder.run(args);
		
		JUnitCore junit = new JUnitCore();
		junit.addListener(new TextListener(System.out));
		junit.run(CucumberTest.class);
	}
	
	
	
	@Bean
	public BaseStepsInterface getBaseStep() {
		String clientType = (System.getProperty("testEnvironment.profile") == null) ? "" : System.getProperty("testEnvironment.profile");

        switch(clientType.toLowerCase()) {
        case "admin":
        	return baseStepsInterface = new BaseStepAdmin();
        case "rwd":
        default:
        	return baseStepsInterface = new BaseStepRWD();
        	
        }
	}
	
	
//	/**
//     * In case we need to set up stuff before the tests are run.
//     *
//     * @throws Exception
//     */
//    @BeforeClass
//    public static void setup() throws Exception {
//        
//        //Settings.initSettings();
//    }


    /**
     * In case we need to clean up stuff after the tests are run.
     */
    @AfterClass
    public static void teardown() {
        SeleniumDriverHandler.shutDown();
    }

}
