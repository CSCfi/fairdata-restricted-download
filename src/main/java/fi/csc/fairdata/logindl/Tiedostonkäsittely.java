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


	public static final int MB = 1048576;
	public static final int MB4= MB*4;
	private static final String PROTOKOLLA = "https://";
	/* production private static final String PORTTIDIR = ":4443/files/";
	private static final String[] UIDAMACHINES =  {"uida1-vip.csc.fi", "uida2-vip.csc.fi", "uida3-vip.csc.fi",
			"uida4-vip.csc.fi", "uida5-vip.csc.fi"};*/
	private HttpServletResponse hsr;
	String encoding = null; //UIDAn kirjautumistiedot
	private String port;
	private int machine = ZipTiedosto.PRODUCTION;
	
	private final static Logger LOG = LoggerFactory.getLogger(Tiedostonkäsittely.class);
	/**
	 * encoding sisältää UIDAn kirjautumistiedot
	 */
	public Tiedostonkäsittely(HttpServletResponse response, String port) {
		this.hsr = response;
		this.port = port;
		if (port.equals("4433")) {
			machine = ZipTiedosto.STABLE;
			//System.out.println("Machine stable");
		} /*else {
			System.out.println("port: " + port);
		}*/
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
			String uida = ZipTiedosto.UIDAMACHINES[machine][ThreadLocalRandom.current().nextInt(0,5)];
			//System.out.println("Uida; "+uida);
			URL url = new URL(PROTOKOLLA+uida+":"+port+ZipTiedosto.DIR+t.getIdentifier()+"/download");
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Authorization", "Basic " + encoding);
			con.setRequestMethod("GET");
			hsr.setContentLengthLong(con.getContentLength()); //idabytes?
			hsr.setContentType("application/octet-stream");  		
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
			//bof.flush();
			System.out.println("Väliaika, ennen transfer: "+ (System.currentTimeMillis()-alkuaika));
			long tavut = in.transferTo(bof);
			/*int ret = 0;
			byte[] buf = new byte[18192];
			while ((ret = in.read(buf)) > 0) {
				bof.write(buf);
				tavut += ret;
			}*/
			double erotus = (System.currentTimeMillis() - alkuaika)/1000.0;
			double megat = tavut/MB;
			DecimalFormat df = new DecimalFormat("#.####");
			System.out.println(filename+" siirretty megatavuja: "+megat+" "
					+df.format(megat/erotus)+"MB/s" );
			bof.flush();			
			in.close();
			bof.close();
			//con.disconnect(); 
		}catch (IOException e2) {
			LOG.error("Ida virhetilanne "+respCode+": ");
			LOG.error(t.getIdentifier()+":" + e2.getMessage());
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
					bof.close();
				}
			} catch (IOException e3) {
				LOG.error(e3.getMessage());
			}

			System.err.println(e2.getMessage());
		} finally {
			if (null != con)
				con.disconnect();
			else 
				LOG.error("Tiedostonkäsittely: Con was null");
		}
	}


}