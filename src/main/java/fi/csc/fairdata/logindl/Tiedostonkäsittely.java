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
import java.util.UUID;
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
	private int machine = ZipTiedosto.TEST;
	
	private final static Logger LOG = LoggerFactory.getLogger(Tiedostonkäsittely.class);
	/**
	 * encoding sisältää UIDAn kirjautumistiedot
	 */
	public Tiedostonkäsittely(HttpServletResponse response, String port) {
		this.hsr = response;
		this.port = port;
		if (port.equals("4433")) {
			//machine = ZipTiedosto.STABLE;
			machine = ZipTiedosto.TEST;
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
		String uuid = UUID.randomUUID().toString();
		LOG.info("{} | Sending file: {}", uuid, t.getFile_path());
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
			LOG.info("opening uida url: {}", url);
			con = (HttpURLConnection) url.openConnection();
			LOG.info("uida url opened");
			con.setReadTimeout(1000*666);
			con.setConnectTimeout(1000*666);

			con.setRequestProperty("Authorization", "Basic " + encoding);
			con.setRequestMethod("GET");
			LOG.info("IDA connection headers set");
			hsr.setContentLengthLong(con.getContentLengthLong()); //idabytes?
			hsr.setContentType("application/octet-stream");  		
			String[] sa = t.getFile_path().split("/");
			String filename = sa[sa.length-1];
			try {
				hsr.addHeader("Content-Disposition", "attachment; filename=\""+filename
						+ "\"; filename*=UTF-8''" +URLEncoder.encode(filename, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				LOG.error("{} | UTF-8 ei muka löydy!", uuid);
				e.printStackTrace();
			}
			respCode = con.getResponseCode();
			String cnt = (String) con.getContent();
			LOG.info("connection response code and content retrieved");
			if (respCode != 200) {
				hsr.sendError(respCode);
				LOG.error("IDA error: {} | Status code: {} content: {}", uuid, respCode, cnt);
				throw new IOException("IDA error: " + respCode);
			}
			BufferedInputStream in = new BufferedInputStream(con.getInputStream(), MB4); 
			LOG.info("{} | Väliaika, ennen transfer: {}", uuid, (System.currentTimeMillis()-alkuaika));
			long tavut = in.transferTo(bof);
			double erotus = (System.currentTimeMillis() - alkuaika)/1000.0;
			double megat = tavut/MB;
			DecimalFormat df = new DecimalFormat("#.####");
			LOG.info("{} | {} siirretty megatavuja: {} {}MB/s", uuid, filename, megat, df.format(megat/erotus) );
			bof.flush();			
			in.close();
			bof.close();
		} catch (IOException e2) {
			try {
				String cnt = (String) con.getContent();
				LOG.error("{} | Ida virhetilanne {}, content: {}", uuid, respCode, cnt);
				LOG.error("{} | {}: {}", uuid, t.getIdentifier(), e2.getMessage());
				try {
					InputStream es = ((HttpURLConnection)con).getErrorStream();
					if (null != es) {
						int ret = 0;
						byte[] buf = new byte[8192];

						while ((ret = es.read(buf)) > 0) {
							bof.write(buf);
							LOG.error("{} | {}", uuid, buf);
						}
						es.close();
						bof.close();
					}
				} catch (IOException e3) {
					LOG.error("{} | {}", uuid, e3.getMessage());
				}
			} catch (IOException | NullPointerException ioe){
				ioe.printStackTrace();
			}


			LOG.error("{} | {}", uuid, e2.getMessage());
		} finally {
			if (null != con)
				con.disconnect();
			else 
				LOG.error("{} | Tiedostonkäsittely: Con was null", uuid);
		}

		LOG.info("{} | done: Sending file: {}", uuid, t.getFile_path());
	}


}