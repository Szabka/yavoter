package hu.kag.yavoter;

import java.io.File;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.properties.PropertiesConfiguration;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory;
import org.junit.Test;

public class Log4j2Tests {

	private static Logger logger = LogManager.getLogger();

	public void performSomeTask() {
		logger.debug("This is a debug message");
		logger.info("This is an info message");
		logger.warn("This is a warn message");
		logger.error("This is an error message");
		logger.fatal("This is a fatal message");
	}

    @Test
    public void testPerformSomeTask() throws Exception {
//    	System.setProperty("log4j2.debug","true");
    	URI configLocation = new File("src/main/config","log4j2.properties").getAbsoluteFile().toURI();
    	System.out.println("Using log4j2 config file:"+configLocation);

    	PropertiesConfigurationFactory factory = new PropertiesConfigurationFactory();
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
		PropertiesConfiguration lconfig = factory.getConfiguration(lc, ConfigurationSource.fromUri(configLocation));
		lc.setConfiguration(lconfig);
//		Configurator.initialize(lconfig);
//		logger = LogManager.getLogger();
		
    	performSomeTask();
    }
}