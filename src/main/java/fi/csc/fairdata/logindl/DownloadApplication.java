/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

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
	  private static final String CONFPROPERTIES = "/opt/od/config.properties";
	  private static String auth;
	  private static String uida;
	  private static String metaxURL;
	  private static String uidaURL;

	
	 @Override
	    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
	        return application.sources(DownloadApplication.class);
	    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
				
		SpringApplication.run(DownloadApplication.class, args);
	}

	public static String getAuth() {
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
         auth = prop.getProperty("auth").trim();
         uida = prop.getProperty("uida").trim();
         in.close();
         f = new File(CONFPROPERTIES);
         in = new FileInputStream(f);
         prop.load(in);
         metaxURL = prop.getProperty("metaxURL").trim();
         uidaURL = prop.getProperty("uidaURL").trim();
         in.close();
     }
     catch (IOException ex) {
         ex.printStackTrace();
     }	
	}

	public static String getMetax() {
		return metaxURL;
	}
	public static String getUidaURL() {
		return uidaURL;
	}
}
