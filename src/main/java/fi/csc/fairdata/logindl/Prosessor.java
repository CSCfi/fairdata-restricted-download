/**
 * 
 */
package fi.csc.fairdata.logindl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

/**
 * Pyynnön parameterit prosessoidaan sallitut tiedostot listaksi
 * 
 * @author pj
 *
 */
public class Prosessor {
	
	//private static final String ZIP = ".zip";
	Dataset dataset;
	String auth;
	Metax m = null;
	Zip zip = null;
	Json json = null;
	final List<String> lf;
	private final static Logger LOG = LoggerFactory.getLogger(Prosessor.class);
	
	Prosessor(Dataset ds, String files, String auth) {
        this.dataset = ds;
		this.auth = auth;
		if (null != files) {
			System.out.println(files);
			lf = Arrays.asList(files.split(","));//.stream().collect(Collectors.toList());
		} else
			lf= null;
	}
	
	/**
	 * Tarkistaa aineiston=dataset rajoitukset ja siihen kuuluvat tiedostot
	 * 
	 * @return List<Tiedosto> mahdolliset aineiston avoimet tiedostot lista
	 */
	public List<Tiedosto> metaxtarkistus(String dirs) {

		String dsid = dataset.getId(); 
		if (null != dsid) {		
			m = new Metax(dsid, auth);
			LOG.info("metax rest: {}", m.getMETAXREST());
			LOG.info("metax dataset url: {}", m.getMETAXDATASETURL());



			//String dsd = dataset.getDir();
			MetaxResponse vastaus = m.puredataset(dsid);
			LOG.info("Metax responded with code: {} and content of: {}", vastaus.getCode(), vastaus.getContent());
			if (vastaus.getCode() == 404) {
				virheilmoitus(404, "datasetid: "+dsid+ " Not found from metax.\n");
				return null;
			}
			json = new Json();
			if (!json.dataset(vastaus.getContent())) {
				virheilmoitus(401, "Datasetin access_type ei ollut login, embarko tai open.");
				return null;
			}	//pääsyoikeudet ovat tässä OK.			
			vastaus = m.files(dsid);
			if (vastaus.getCode() == 404) {
				virheilmoitus(404, "datasetid: "+dsid+ " lost from metax.\n");
				return null;
			}
			if (vastaus.getCode() >= 204) {
				LOG.info("Metax response was other than 2xx: {}", vastaus.getCode());
			}

			List<Tiedosto> dsfiles = json.file(vastaus.getContent());
			if (null != dsfiles) {
				//dataset.setMetadata(vastaus.getContent()); // lisätään zippiin
				if (dsfiles.isEmpty()) {
					virheilmoitus(404, "Datasetissa EI ollut sallittuja tiedostoja");
					return null;
				}
				if (null != dirs) { // parametrina hakemisto
					List<String> ld = Arrays.asList(dirs.split(","));
					List<Tiedosto> tl = new ArrayList<Tiedosto>();
					ld.forEach(d -> selvitähakemistonsisältömetaxista(d, tl));
					List<String> lid = new ArrayList<String>();
					dsfiles.forEach(f -> lid.add(f.getIdentifier()));
					List<Tiedosto>  valmiit = tl.stream().filter(t -> lid.contains(t.getIdentifier())).collect(Collectors.toList());
					if (null != lf) { // parametrina tiedosto/ja
						dsfiles.stream().filter(t -> lf.contains(t.getIdentifier())).map(t -> valmiit.add(t));
					}
					return valmiit;
				}
				if (null != lf) { // parametrina tiedosto/ja
					// oikeasti voi lisätä zippiin!!!
					//System.out.println("Debug: "+lf.get(0));
					return dsfiles.stream().filter(t -> lf.contains(t.getIdentifier())).collect(Collectors.toList());
				} else { // koko aineisto
					return dsfiles;
				}
			} else {
				virheilmoitus(400, "Metaxin palauttamien datasetin tietojen parsinta epännistui "+
			"(yleensä tämä tarkoittaa, että datasetissä ei ole pääsyoikeustietoja).");
				return null;
			}
		
			//return null; //tänne ei pitäsi koskaan päätyä
		} else {	
			virheilmoitus(400, "datasetid on pakollinen parametri!!!");
			return null;
		}
	}	

	/*
	private void selvitätiedostonnimimetaxista(String f, List<Tiedosto> tl) {
		String jsons = m.file(f);
		if (null != jsons) {
			String filename = json.name(jsons);
			if (!Json.OPENACCESFALSE.equals(filename))
				tl.add(new Tiedosto(filename, f));
			else 
				System.out.println("Tiedosto "+f+" ei ollut avoin");
		}
	}*/

	/**
	 * Selvittää REKURSIIVISESTI (käyttäen  recursive=true parametria) metaxisata
	 * hakemiston kaikki tiedostot.
	 * 
	 * @param dir String hakemiston tunniste
	 * @param filelist List<String> palautetaan tiedostojen tunnisteet
	 */
	public void selvitähakemistonsisältömetaxista(String dir,  List<Tiedosto> filelist) {
		MetaxResponse d = m.directories(dir);
		if (200 == d.getCode()) {
			filelist.addAll(json.dir(d.getContent(), this));
			//System.out.println(d.getContent());
		}
		else {
			System.err.println("Metax vastasi "+dir+"-hakemistokyselyyn muuta kuin 200: "+d.getCode()+d.getContent());
		}
	}

	
	/**
	 * Näyttää käyttäjälle virheilmoituksen
	 * 
	 * @param code int HTTP status code
	 * @param sisältö String seliseli
	 */
	public void virheilmoitus(int code, String sisältö) {
		HttpServletResponse r = dataset.getResponse();
		r.setContentType("text/plain;charset=UTF-8");
		r.setStatus(code);
	    try {
			r.getWriter().println(sisältö);
			 r.flushBuffer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @return int tiedostojen lukumäärä
	 */
	public int noOfFiles() {
		if ( null == lf) 
			return 0;
		else
			return lf.size();
	}

	
 /* tilapäisesti pois käytöstä
	private void dirprosess(String id) {
		MetaxResponse j = m.directories(id);
		if (j.getCode() != 200) {
			zip.entry(Metax.DIR+id, j.getCode() + j.getContent());
			System.err.println("Dir vastaus: "+j.getCode() + j.getContent());
		} else 
			zip.entry(Metax.DIR+id, j.getContent());
	}
*/
}
