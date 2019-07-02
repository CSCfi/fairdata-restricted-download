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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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

	private final static int NOTHREADS = 4;
	private static final int MB = 1048576 ;
	private HttpServletResponse hsr;
	String encoding = null; //UIDAn kirjautumistiedot
	static HttpClient[]  httpClienta = {
			HttpClient.newBuilder().version(Version.HTTP_1_1).build(),
			HttpClient.newBuilder().version(Version.HTTP_1_1).build(),		
			HttpClient.newBuilder().version(Version.HTTP_1_1).build(),
			HttpClient.newBuilder().version(Version.HTTP_1_1).build()
	};
	static int i = 0;

	
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
		HttpResponse<InputStream> response = null;
		double alkuaika = System.currentTimeMillis();
		try { 
			bof = new BufferedOutputStream(hsr.getOutputStream(), MB); //1M
			//System.out.println("Ladattavaksi tuli " + t.getIdentifier());
			
			HttpRequest request = HttpRequest.newBuilder()
    				.uri(URI.create(DownloadApplication.getUidaURL()+t.getIdentifier()+"/download"))
    				.header("Authorization", "Basic " + encoding)
    				.build();	
			response = httpClienta[i % httpClienta.length].send(request, BodyHandlers.ofInputStream());
			OptionalLong size = response.headers().firstValueAsLong("content-length");
			if (size.isPresent()) {
				System.out.println("Ladattavantiedoston koko " + size.getAsLong());
				hsr.setContentLengthLong(size.getAsLong()); //idabytes?
			} else {
				System.err.println("Ladattavantiedoston koko tuntematon");
			}
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

			BufferedInputStream in = new BufferedInputStream(response.body(), MB); //1MB	
			long tavut = in.transferTo(bof);
			double erotus = (System.currentTimeMillis() - alkuaika)/1000.0;
			double megat = tavut/MB;
			DecimalFormat df = new DecimalFormat("#.####");
			System.out.println(filename+" siirretty megatavuja: "+megat+" "
					+df.format(megat/erotus)+"MB/s" );
			bof.flush();
			in.close();
			//con.disconnect(); //??

		}catch (IOException e2) {
	
				int respCode = response.statusCode();				
				//int ret = 0;
				//byte[] buf = new byte[8192];
				System.err.print("Ida virhetilanne "+respCode+": ");
				System.err.println(t.getIdentifier()+":");
				/*while ((ret = es.read(buf)) > 0) {
					bof.write(buf);
					System.err.write(buf); 
					System.err.println();
				}
				es.close();
				*/
			
			System.err.println(e2.getMessage());
		} catch (InterruptedException e1) {
			System.err.println("Tiedostodownlaod: InterruptedException: ");
			e1.printStackTrace();
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
		hsr.setContentType("application/octet-stream; charset=UTF-8");
		try {
			hsr.addHeader("Content-Disposition", "attachment; filename=\""+zipfilename
					+ "\"; filename*=UTF-8''" +URLEncoder.encode(zipfilename, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.err.println("UTF-8 ei muka löydy!");
			e.printStackTrace();
		}
		ExecutorService executor = Executors.newFixedThreadPool(NOTHREADS);//Executors.newWorkStealingPool(); ei toimi
		List<Future<?>> futures = new ArrayList<>();
		
		tl.forEach(t -> {   

            Future tiedostoFuture = executor.submit(() -> {
        		HttpRequest request = HttpRequest.newBuilder()
        				.uri(URI.create(DownloadApplication.getUidaURL()+t.getIdentifier()+"/download"))
        				.header("Authorization", "Basic " + encoding)
        				.build();	
        		i++;
        		int respCode = -1;		
        		try { 
        		HttpResponse<InputStream> response = httpClienta[i % httpClienta.length].send(request, BodyHandlers.ofInputStream());						
        				respCode = response.statusCode();
        				z.entry(t.getFile_path());
        				BufferedInputStream in = new BufferedInputStream(response.body());	
        				in.transferTo((OutputStream)z.getZout());	//virtaa suoraan käyttäjälle
        				in.close();
        				z.getZout().closeEntry();
        				z.release();
        			}catch (IOException e2) {			
        				System.err.print("Ida zip virhetilanne "+respCode+": ");
        				System.err.println(t.getIdentifier()+":");
        				System.err.println(e2.getMessage());
        				z.release();
        			} catch (InterruptedException e) {
        				System.err.print("Ida zip interrup virhetilanne "+respCode+": ");
        				e.printStackTrace();
        				z.release();
        			}
            
		});
            futures.add(tiedostoFuture);
		});
		futures.forEach(f -> {
		    try {
		        f.get();
		    } catch (InterruptedException | ExecutionException ex) {
		        LOG.error("Error waiting for file load", ex);
		    }
		});
		
		z.sendFinal();
	}
}