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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpHeaders;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
	
	HttpClient[]  httpClienta = {
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
		if (port.equals("4433")) {
			machine = STABLE;
		}
		try {
			encoding = Base64.getEncoder().encodeToString((DownloadApplication.getUida()).getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
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
		System.out.println("Zippattavaksi tuli " + tl.size());

		Zip z = new Zip(hsr);
		String zipfilename = dsid+".zip";

		hsr.setContentType("application/octet-stream");
		try {
			hsr.addHeader("Content-Disposition", "attachment; filename=\""+zipfilename
					+ "\"; filename*=UTF-8''" +URLEncoder.encode(zipfilename, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.err.println("UTF-8 ei muka löydy!");
			e.printStackTrace();
		}

		tl.forEach(t -> {   
			String uida = UIDAMACHINES[machine][ThreadLocalRandom.current().nextInt(0,5)];
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(PROTOKOLLA+uida+":"+port+DIR+t.getIdentifier()+"/download"))
						.header("Authorization", "Basic " + encoding)
						.build();
				i++;
				int respCode = -1;
				double alkuaika = System.currentTimeMillis();
				long tavut = 0;
				String tiedostoPolku = t.getFile_path();
				System.out.println("URI: " + request.uri());
				System.out.println("Aloitetaan pakkausta: " + tiedostoPolku);
				try { 					
					HttpResponse<InputStream> response = httpClienta[i % httpClienta.length].send(request, BodyHandlers.ofInputStream());						
					respCode = response.statusCode();
					long contentLength = 0;
					HttpHeaders headers = response.headers();
					Optional<String> contentLengthHeader = headers.firstValue("Content-Length");
					if (contentLengthHeader.isPresent()) {
						try {
							contentLength = Long.parseLong(contentLengthHeader.get());
						} catch (NoSuchElementException e) {
							System.out.println("Unable to fetch content-length");
						}
					}
					System.out.println("Status code: " + respCode);
					System.out.println("Content Length: " + contentLength);

					z.entry(t.getFile_path());
					if (t.getFile_path().endsWith("jp2") || t.getFile_path().endsWith("zip") || t.getFile_path().endsWith("bz2")
							|| t.getFile_path().endsWith("gz")) {
						z.getZout().setLevel(Deflater.NO_COMPRESSION);
						System.out.println("NO_COMPRESSION: " + t.getFile_path());
					}
					BufferedInputStream in = new BufferedInputStream(response.body());	
					tavut = 0;
					try {
						tavut = in.transferTo((OutputStream)z.getZout());	//virtaa suoraan käyttäjälle
					} catch (IOException e2) {
						System.out.println("TransferTo IOException:");
						System.err.println(e2.getMessage());
						System.out.println("Sulkuaika: "+ (System.currentTimeMillis() - alkuaika));
						System.out.println("siirretty tavuja: " + tavut);
						System.out.println("tiedosto: " + tiedostoPolku);
						in.close();
						z.getZout().closeEntry();
						z.getZout().setLevel(Deflater.BEST_COMPRESSION);
						z.release();
						throw e2;
					}

					if (tavut != contentLength) {
						System.out.println("Virhe: Eri suuri määrä tavuja.");
					}
					System.out.println("Vastaanotettu tavuja: " + tavut);
					/*int ret = 0;
					byte[] buf = new byte[18192];
					BufferedOutputStream bzout = new BufferedOutputStream(z.getZout(), Tiedostonkäsittely.MB4);
					while ((ret = in.read(buf)) > 0) {
						bzout.write(buf);
						tavut += ret;
					}*/
					in.close();
					z.getZout().closeEntry();
					z.getZout().setLevel(Deflater.BEST_COMPRESSION);					
					z.release();
					
					double erotus = (System.currentTimeMillis() - alkuaika)/1000.0;
					double megat = tavut/Tiedostonkäsittely.MB;
					DecimalFormat df = new DecimalFormat("#.####");
					System.out.println(t.getIdentifier()+" zipattu megatavuja: "+megat+" "
							+df.format(megat/erotus)+"MB/s" );
					System.out.println("siirretty tavuja: " + tavut);
					System.out.println("tiedosto: " + tiedostoPolku);
				} catch (IOException e2) {
					System.err.print("Ida zip virhetilanne "+respCode+": ");
					System.err.println(t.getIdentifier()+":");
					System.err.println(e2.getMessage());
					System.out.println("Sulkuaika: "+ (System.currentTimeMillis() - alkuaika));
					System.out.println("siirretty tavuja: " + tavut);
					System.out.println("tiedosto: " + tiedostoPolku);
					z.release();
				} catch (InterruptedException e) {
					System.err.print("Ida zip interrup virhetilanne "+respCode+": ");
					System.out.println("siirretty tavuja: " + tavut);
					System.out.println("tiedosto: " + tiedostoPolku);
					System.err.println(e.getMessage());
					e.printStackTrace();
					z.release();
				}
			});
		z.sendFinal();
	}
}
