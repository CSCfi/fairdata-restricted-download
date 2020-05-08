/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * @author pj
 *
 */
@SpringBootApplication
public class DownloadApplication extends SpringBootServletInitializer {
	
	  private static final String PROPERTIES = "/opt/secrets/metax.properties";
	  private static final String CONFPROPERTIES = "/opt/login-download/config.properties";
	  private static String auth;
	  private static String uida;
	  private static String metaxURL;
	  private static String uidaURL;
	  private final static Logger LOG = LoggerFactory.getLogger(DownloadApplication.class);

	
	 @Override
	    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
	        return application.sources(DownloadApplication.class);
	    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.setProperty("server.jetty.thread-idle-timeout","666000");
		System.setProperty("server.tomcat.connection-timeout","666000");
		System.setProperty("server.tomcat.max-threads", "8000");
		System.setProperty("server.tomcat.accept-count", "2000");
		System.setProperty("server.connection-timeout","666000");
		SpringApplication.run(DownloadApplication.class, args);
	}

	public static String getAuth() {
		LOG.info("using auth: {}", auth);
		return auth;
	}

	public static String getUida() {
		return uida;
	}


	static {
	 Properties prop = new Properties();
     try {
         File f = new File(PROPERTIES);
         FileInputStream in = new FileInputStream(f);
         prop.load(in);
         LOG.info("properties loaded");
         auth = prop.getProperty("auth").trim();
         uida = prop.getProperty("uida").trim();
         in.close();
         f = new File(CONFPROPERTIES);
         in = new FileInputStream(f);
         prop.load(in);
         LOG.info("Conf properties loaded");
         metaxURL = prop.getProperty("metaxURL").trim();
         uidaURL = prop.getProperty("uidaport").trim();
         in.close();
         LOG.info("All properties loaded successfully");
     }
     catch (IOException ex) {
     	LOG.error("loading props failed");
         ex.printStackTrace();
     }	
	}

	public static String getMetax() {
		return metaxURL;
	}
	
	/**
	 * Ignore machine name=uidaa[0], we are using just port, Because names are in ZipTiedosto
	 *  
	 * @return String port
	 */
	public static String getUidaPort() {
		String[] uidaa = uidaURL.split(":");
		return uidaa[1];
	}
}
