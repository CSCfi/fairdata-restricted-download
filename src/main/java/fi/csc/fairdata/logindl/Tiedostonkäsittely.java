/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Tiedostojen nouto UIDAsta
 * 
 * @author pj
 *
 */
public class Tiedostonkäsittely  {


	private static final int MB = 1048576;
	private static final int MB4= MB*4;
	private static final String PROTOKOLLA = "https://";
	private static final String PORTTIDIR = ":4443/files/";
	private static final String[] UIDAMACHINES =  {"uida1-vip.csc.fi", "uida2-vip.csc.fi", "uida3-vip.csc.fi",
			"uida4-vip.csc.fi", "uida5-vip.csc.fi"};
	private HttpServletResponse hsr;
	String encoding = null; //UIDAn kirjautumistiedot
	
	
	private final static Logger LOG = LoggerFactory.getLogger(Tiedostonkäsittely.class);
	/**
	 * encoding sisältää UIDAn kirjautumistiedot
	 */
	public Tiedostonkäsittely(HttpServletResponse response) {
		this.hsr = response;
		try {
			encoding = Base64.getEncoder().encodeToString((DownloadApplication.getUida()).getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void tiedosto(Tiedosto t) {
		BufferedOutputStream bof = null;
		HttpURLConnection con = null;
		int respCode = 0;
		double alkuaika = System.currentTimeMillis();
		try { 
			bof = new BufferedOutputStream(hsr.getOutputStream(), MB4); 
			//System.out.println("Ladattavaksi tuli " + t.getIdentifier());
			String uida = UIDAMACHINES[ThreadLocalRandom.current().nextInt(0,5)];
			URL url = new URL(PROTOKOLLA+uida+PORTTIDIR+t.getIdentifier()+"/download");
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Authorization", "Basic " + encoding);
			con.setRequestMethod("GET");
			hsr.setContentLengthLong(con.getContentLength()); //idabytes?
			hsr.setContentType("application/octet-stream; charset=UTF-8");  		
			String[] sa = t.getFile_path().split("/");
			String filename = sa[sa.length-1];
			try {
				hsr.addHeader("Content-Disposition", "attachment; filename=\""+filename
						+ "\"; filename*=UTF-8''" +URLEncoder.encode(filename, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				System.err.println("UTF-8 ei muka löydy!");
				e.printStackTrace();
			}
			respCode = con.getResponseCode();
			
			BufferedInputStream in = new BufferedInputStream(con.getInputStream(), MB4); 			
			long tavut = in.transferTo(bof);
			double erotus = (System.currentTimeMillis() - alkuaika)/1000.0;
			double megat = tavut/MB;
			DecimalFormat df = new DecimalFormat("#.####");
			System.out.println(filename+" siirretty megatavuja: "+megat+" "
					+df.format(megat/erotus)+"MB/s" );
			bof.flush();
			in.close();
			con.disconnect(); 

		}catch (IOException e2) {
			LOG.error("Ida virhetilanne "+respCode+": ");
			LOG.error(t.getIdentifier()+":");
			try {
				InputStream es = ((HttpURLConnection)con).getErrorStream();	
				if (null != es) {
					int ret = 0;
					byte[] buf = new byte[8192];

					while ((ret = es.read(buf)) > 0) {
						bof.write(buf);
						System.err.write(buf); 
						System.err.println();
					}
					es.close();
				}
			} catch (IOException e3) {
				LOG.error(e3.getMessage());
			}

			System.err.println(e2.getMessage());
		}
	}


}