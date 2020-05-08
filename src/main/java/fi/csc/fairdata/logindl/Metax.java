/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pj
 *
 */
public class Metax {

	private final String METAXREST;//"https://metax-test.csc.fi/rest/";
	private final String METAXDATASETURL; // = METAXREST+"datasets/";

	public String getMETAXREST() {
		return METAXREST;
	}

	public String getMETAXDATASETURL() {
		return METAXDATASETURL;
	}

	public String getMETAXDIRURL() {
		return METAXDIRURL;
	}

	public String getMETAXFILEURL() {
		return METAXFILEURL;
	}

	private final String METAXDIRURL;// = METAXREST+"directories/";
	private final String METAXFILEURL;// = METAXREST+"files/";
	private final static String FORMAT = "?format=json";
	public final static String DIR = "Dir";
	public final static String DATASET = "Dataset";
	public final static String FILES = "/files";
	String datasetid;
	String encoding = null;

	private final static Logger LOG = LoggerFactory.getLogger(Metax.class);
	
	public Metax(String id, String auth) {
		this.datasetid = id;
		METAXREST = DownloadApplication.getMetax();
		METAXDATASETURL = METAXREST+"datasets/";
		METAXDIRURL = METAXREST+"directories/";
		METAXFILEURL = METAXREST+"files/";
		try {
			encoding = Base64.getEncoder().encodeToString((auth).getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Hakee datasetin tiedot metaxista. Erityisen tärkeä on metaxrestin 3. parametri,
	 * jonka on oltava false, jottei tietoja vuoda.
	 * 
	 * @param id String dataset id
	 * @return MetaxResponse Metaxin vastauksen koodi ja sisältö
	 */
	public MetaxResponse puredataset(String id) {
		return metaxrest(id, METAXDATASETURL, true, DATASET);
	}
	
	
	public MetaxResponse files(String id) {
		return metaxrest(id, METAXDATASETURL, true, FILES);
	}
	
	public MetaxResponse directories(String id) {
		return metaxrest(id, METAXDIRURL, true, DIR);
	}
	
	/**
	 * 
	 * @param id String dataset id
	 * @param url String basic URL to connect, some options will be added
	 * @param auth boolean true basic auth, false NO authentication: very important to not use auth
	 * because you'll get GDPR information with auth
	 * @param name String metax API to use
	 * @return MetaxResponse Object with code and content
	 */
	MetaxResponse metaxrest(String id, String url, boolean auth, String name ) {
	StringBuilder content = new StringBuilder();
	HttpURLConnection con = null;
	BufferedReader in = null;
	URL furl = null;
	try { //+/?cr_identifier="+datasetid "&recursive=true"
		String optio = name.equals(DIR) || name.equals(FILES) ? FILES : ""; 
		String optio2 =  name.equals(FILES) ? "&file_fields=file_path,identifier" : ""; 
		if (name.equals(DIR))
			optio2 =  "&cr_identifier="+datasetid+"&recursive=true&depth=*&file_fields=identifier,file_path&directory_fields=identifier,directory_path";
		furl = new URL(url+id+optio+FORMAT+optio2);
		LOG.info("trying to open metax url: {}", furl);
		long alku =  System.currentTimeMillis();
		con = (HttpURLConnection) furl.openConnection();
		LOG.info("metax url opened");
		con.setRequestMethod("GET");	
		if (auth)
			LOG.info("setting authorization for metax request");
			con.setRequestProperty  ("Authorization", "Basic " + encoding);
			in = new BufferedReader(
				new InputStreamReader(con.getInputStream(), "UTF-8"));//con.getContentEncoding()
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
		}
		in.close();
		con.disconnect(); //??
		System.out.println(furl.toString()+ " take "+ (System.currentTimeMillis()-alku) + "ms");
		return new MetaxResponse(con.getResponseCode(), content.toString());
	} catch (IOException e2) {
		try {
		int respCode = ((HttpURLConnection)con).getResponseCode();
		String cnt = (String) ((HttpURLConnection)con).getContent();
		InputStream es = ((HttpURLConnection)con).getErrorStream();
		int ret = 0;
		byte[] buf = new byte[8192];
		//System.err.print(name +" virhetilanne "+respCode+": ");
		LOG.error("IOException with statusCode: {}, content: {}", respCode, cnt);
		e2.printStackTrace();
		int i = 0;
        while ((ret = es.read(buf)) > 0) {
        	content.append(Arrays.toString(buf));
        	//System.err.write(buf); 
        	//System.err.println(furl.toString());
			LOG.error("IOException ErrorStream furl: {}: {}", i, furl.toString());
        }
        es.close();
        LOG.error("IOException ErrorStream content: {}", content.toString());
        return new MetaxResponse(respCode, content.toString());
        } catch (IOException e3) {
        	System.err.println(e3.getMessage());
        }
		System.err.println(e2.getMessage());
	} catch (Exception ge) {
		LOG.error("Other error occurred");
		System.err.println(ge.getMessage());
		ge.printStackTrace();
	}
	finally {
		try {
			if (null != in)			
				in.close();
			else 
				LOG.error("Metax: BufferedReader close failed");
		} catch (IOException e) {
			LOG.error("Metax: IOException:"+e.getMessage());
			//e.printStackTrace();
		}
		if (null != con)
			con.disconnect();
		else 
			LOG.error("Metax: Connection disconnect failed, connection is null");
	}

	return new MetaxResponse(1234, "");
}
	
	public String file(String id) {	
		StringBuilder content = new StringBuilder();
		//boolean b = false;
		HttpURLConnection con = null;
		try {
			URL url = new URL(METAXFILEURL+id+FORMAT);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");			
			con.setRequestProperty  ("Authorization", "Basic " + encoding);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream(), "UTF-8"));//con.getContentEncoding()
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
				
			}
			in.close();
			con.disconnect(); //??
			return content.toString();
		} catch (IOException e) { //https://docs.oracle.com/javase/7/docs/technotes/guides/net/http-keepalive.html
	        try {
	                int respCode = ((HttpURLConnection)con).getResponseCode();
	                InputStream es = ((HttpURLConnection)con).getErrorStream();
	                int ret = 0;
	                // read the response body
	                byte[] buf = new byte[8192];
	                while ((ret = es.read(buf)) > 0) {
	                	System.err.println("File virhetilanne "+respCode+": "+buf.toString());
	                	
	                }
	                
	                // close the errorstream
	                es.close();
	        } catch (IOException e2) {
	        	System.err.println(e2.getMessage());
	        }
		}
		return null;//content.toString();
	}

}
