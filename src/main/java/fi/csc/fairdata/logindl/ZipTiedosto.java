/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.HttpURLConnection;

import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpHeaders;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.Deflater;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Erotettu zip ja tiedostonkäsittely toisistaan, mikä normaalisti olisi tyhmää mutta nyt oli ongelmia  
 * 
 * @author pj
 *
 */
public class ZipTiedosto {
	public static final int MB = 1048576;
	public static final int MB4= MB*4;

	private final static int NOTHREADS = 2;
	public final static int PRODUCTION = 0;
	public final static int STABLE = 1;
	private static final String PROTOKOLLA = "https://";
	public static final String DIR = "/files/";
	public static final String[][] UIDAMACHINES =  {{"uida1-vip.csc.fi", "uida2-vip.csc.fi", "uida3-vip.csc.fi",
		"uida4-vip.csc.fi", "uida5-vip.csc.fi"},
			{"ida-stable.csc.fi", "ida-stable.csc.fi", "ida-stable.csc.fi",
			"ida-stable.csc.fi", "ida-stable.csc.fi"}};
	private HttpServletResponse hsr;
	private String port;
	private int machine = PRODUCTION;
	String encoding = null; //UIDAn kirjautumistiedot
	
	HttpClient[] httpClienta = {
		HttpClient.newBuilder().version(Version.HTTP_1_1).build(),
		HttpClient.newBuilder().version(Version.HTTP_1_1).build()
	};
	int i = 0;
	
	private final static Logger LOG = LoggerFactory.getLogger(ZipTiedosto.class);

	/**
	 * Contructori
	 * 
	 * @param response HttpServletResponse vastaus etsimelle
	 */
	public ZipTiedosto(HttpServletResponse response, String port) {
		this.hsr = response;
		this.port = port;
		LOG.info("port: {} ", port);
		if (port.equals("4433")) {
			machine = STABLE;
		}
		try {
			encoding = Base64.getEncoder().encodeToString((DownloadApplication.getUida()).getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			LOG.error("UnsupportedEncodingException: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	private String getHeaderAsNumber(Map<String, List<String>> headers, String name) {
		List<String> value = headers.getOrDefault(name, Arrays.asList("0"));
		if (value.isEmpty()) {
			return "0";
		} else {
			return value.get(0);
		}
	}
	/**
	 * Noutaa UIDAsta ja zippaa joukon tiedostoja. 
	 * 
	 * @param tl List<Tiedosto> lista zipattavista tiedostoista
	 * @param dsid String dataset ID, tulee zipin nimeksi
	 * @param metadata String Datasetin metadata metaxista
	 */
	public void zip(List<Tiedosto> tl, String dsid, String metadata) {
		String uuid = UUID.randomUUID().toString();

		LOG.info("{} | zip function called for {}", uuid, dsid);
		LOG.info("{} | number of files: {}", uuid, tl.size());
		LOG.info("{} | metadata: {}", uuid, metadata);

		Zip z = new Zip(hsr);
		z.getZout().setLevel(Deflater.NO_COMPRESSION);
		String zipfilename = dsid+".zip";
		OutputStream notbof = (OutputStream)z.getZout();

		hsr.setContentType("application/octet-stream");
		hsr.addHeader("Transfer-Encoding","chunked");

		try {
			hsr.addHeader("Content-Disposition", "attachment; filename=\""+zipfilename
					+ "\"; filename*=UTF-8''" +URLEncoder.encode(zipfilename, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			LOG.error("{} | UnsupportedEncodingException: {}", uuid, e.getMessage());
			e.printStackTrace();
		}

		long totalBytes = 0;

		Boolean transfer_failed = false;
		long filesZipped = 0;
		for (int idx=0; idx < tl.size(); idx++) {
			Tiedosto t = tl.get(idx);
			String tiedostoPolku = t.getFile_path();
			if (transfer_failed) {
				LOG.info("{} | Skipped a file due to earlier error: {}", uuid, tiedostoPolku);
				return;
			}
			filesZipped++;

			String uida = UIDAMACHINES[machine][ThreadLocalRandom.current().nextInt(0,5)];
			HttpURLConnection con = null;
			BufferedInputStream in = null;
			int respCode = -1;
			double alkuaika = System.currentTimeMillis();
			long tavut = 0;

			try {
				URL url = new URL(PROTOKOLLA+uida+":"+port+DIR+t.getIdentifier()+"/download");
				con = (HttpURLConnection) url.openConnection();
				con.setReadTimeout(1000*666);
				con.setConnectTimeout(1000*666);
				con.setRequestProperty("Authorization", "Basic " + encoding);
				con.setRequestMethod("GET");

				i++;

				LOG.info("{} | URI: {}", uuid, url);

				respCode = con.getResponseCode();
				if (respCode != 200) {
					hsr.sendError(respCode);
					LOG.error("{} | Status code: {}", uuid, respCode);
					throw new IOException("IDA error: " + respCode);
				}
				Map<String, List<String>> headers = con.getHeaderFields();
				LOG.trace("{} | headers: {}", uuid, headers.keySet());
				long contentLength = Long.parseLong(getHeaderAsNumber(headers, "content-length"));

				LOG.info("{} | Status code: {}", uuid, respCode);
				LOG.info("{} | Content Length: {}", uuid, contentLength);

				in = new BufferedInputStream(con.getInputStream()); 
				z.entry(t.getFile_path());

				tavut = 0;

				try {
					tavut = in.transferTo(notbof);	//virtaa suoraan käyttäjälle
				} catch (IOException e2) {
					LOG.error("{} | TransferTo IOException: {}", uuid, e2.getMessage());
					try {
						LOG.info("{} | {}", uuid, con.getContent().toString());
					} catch (IOException e3) {
						LOG.error("{} | {}", uuid, e3.getMessage());
					}
					LOG.info("{} | Time used in milliseconds: {}", uuid, (System.currentTimeMillis() - alkuaika));
					LOG.info("{} | transferred bytes: ", uuid, tavut);
					in.close();
					in = null;

					throw e2;
				} finally {
					if (in != null) {
						in.close();
						in = null;
					}
				}

				if (tavut != contentLength) {
					LOG.error("{} | Transferred bytes ({}) != content-length ({})", uuid, tavut, contentLength);
					throw new IOException("Transferred bytes does not match the bytes in IDA.");
				}

				double erotus = (System.currentTimeMillis() - alkuaika)/1000.0;
				double megat = tavut/Tiedostonkäsittely.MB;
				DecimalFormat df = new DecimalFormat("#.####");
				LOG.info("{} | {} zipped in megabytes: {} {}MB/s", uuid, t.getIdentifier(), megat, df.format(megat/erotus));
				LOG.info("{} | Time used in milliseconds: {}", uuid, (System.currentTimeMillis() - alkuaika));
				LOG.info("{} | Transferred bytes: {} ", uuid, tavut);
				LOG.info("{} | Transfer complete", uuid);
			} catch (IOException e2) {
				LOG.error("{} | IOException: {}: {}: {}", uuid, respCode, t.getIdentifier(), e2.getMessage());
				LOG.info("{} | Time used in milliseconds: {}", uuid, (System.currentTimeMillis() - alkuaika));
				LOG.info("{} | Transferred bytes: {} ", uuid, tavut);
				LOG.info("{} | Transfer failed", uuid);
				transfer_failed = true;
			} finally {
				if (con != null) {
					try {
						con.getInputStream().close();
					} catch (IOException e) {}
					try {
						con.getOutputStream().close();
					} catch (IOException e) {}
					con.disconnect();
					con = null;
				}
				try {
					z.getZout().closeEntry();
					z.getZout().flush();
				} catch (IOException e) {}
				z.release();

				if (in != null) {
					try {
						in.close();
						in = null;
					} catch (IOException e) {
						LOG.error("{} | in close: {}", uuid, e.getMessage());
					}
				}
				totalBytes += tavut;
			}
		}

		z.sendFinal();

		if (notbof != null) {
			try {
				notbof.flush();
			} catch (IOException e) {
				LOG.error("{} | notbof flush: {}", uuid, e.getMessage());
			}
			try {
				notbof.close();
			} catch (IOException e) {
				LOG.error("{} | notbof close: {}", uuid, e.getMessage());
			}

			notbof = null;
		}

		LOG.info("{} | total bytes transferred: {}", uuid, totalBytes);
		LOG.info("{} | total files zipped: {}/{}", uuid, filesZipped, tl.size());
		if (transfer_failed) {
			LOG.error("{} | zip failed", uuid);
		} else {
			LOG.info("{} | zip done", uuid);
		}
		z = null;
	}
}
