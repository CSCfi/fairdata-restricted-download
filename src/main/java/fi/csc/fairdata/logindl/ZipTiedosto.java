/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.BufferedInputStream;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

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
	
	private final static int NOTHREADS = 4;
	private static final String PROTOKOLLA = "https://";
	private static final String PORTTIDIR = ":4443/files/";
	private static final String[] UIDAMACHINES =  {"uida1-vip.csc.fi", "uida2-vip.csc.fi", "uida3-vip.csc.fi",
			"uida4-vip.csc.fi", "uida5-vip.csc.fi"};

	private HttpServletResponse hsr;
	String encoding = null; //UIDAn kirjautumistiedot
	
	HttpClient[]  httpClienta = {
			HttpClient.newBuilder().version(Version.HTTP_1_1).build(),
			HttpClient.newBuilder().version(Version.HTTP_1_1).build(),		
			HttpClient.newBuilder().version(Version.HTTP_1_1).build(),
			HttpClient.newBuilder().version(Version.HTTP_1_1).build()
	};
	int i = 0;
	
	private final static Logger LOG = LoggerFactory.getLogger(ZipTiedosto.class);

	public ZipTiedosto(HttpServletResponse response) {
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
			String uida = UIDAMACHINES[ThreadLocalRandom.current().nextInt(0,5)];
            Future tiedostoFuture = executor.submit(() -> {
        		HttpRequest request = HttpRequest.newBuilder()
        				.uri(URI.create(PROTOKOLLA+uida+PORTTIDIR+t.getIdentifier()+"/download"))
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
