/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * Kaikki json parsinta. Luotetaan googlen laatuun eli tehdään gson:lla
 * 
 * @author pj
 *
 */
public class Json {
	
	public static final String ACCESSFALSE = "access-false";
	
	public boolean dataset(String vastaus) {
		Gson gson = new GsonBuilder().create();
		JsonObject jo = gson.fromJson(vastaus, JsonObject.class);
		if (null != jo ) {
			try {
				JsonObject rd = jo.get("research_dataset").getAsJsonObject();
				JsonObject at = rd.get("access_rights").getAsJsonObject().
						get("access_type").getAsJsonObject();
				String a = at.get("identifier").getAsString();
				if  (a.endsWith("open") || 
						a.endsWith("login") || 
						a.endsWith("embargo"))
					return true;
			} catch (java.lang.NullPointerException e) {
				System.err.println("Muu virhe dataset parsinnassa: "+jo.getAsString());				
			}
		} else {
			System.err.println("Dataset object parsing failed.");
			return false;
		}
		return false;
	}
	
	/**
	 * Selvittää datasetin tiedostot ja hakemistot metaxin vastauksesta
	 * 
	 * @param vastaus String metaxin dataset/{id}/files kyselyn vastaus
	 * @return List<Tiedosto>, avointen tiedostojen lista
	 */
	public List<Tiedosto> file(String vastaus) {

		List<Tiedosto> lsf = new ArrayList<Tiedosto>();
		//List<String> lsd = new ArrayList<String>();

		Gson gson = new GsonBuilder().create();
		JsonArray ja = gson.fromJson(vastaus, JsonArray.class);
		try {
			try {
				//JsonArray ja = rd.get("files").getAsJsonArray();
				ja.forEach(o -> etsiidt(o.getAsJsonObject(), lsf));
			} catch (java.lang.NullPointerException e) {
				System.out.println("Datasetistä ei löytynyt tiedostoja.");
			}
			return lsf;
		} catch (java.lang.NullPointerException e) {
			System.err.println("Muu virhe json parsinnassa");
			return null;
		}
	}

	/**
	 * Parsii tiedoston tunnisteen, file_path:in ja saatavuuden
	 * Junnu flowdockissa 9:52
Open kyllä, login kyllä, permit ei (tässä vaiheessa), restricted ei,  embargo kyllä
	 * 
	 * @param o JsonObject tiedoston tiedot
	 * @param ls List<Tiedosto>, lista johon tiedosto lisätään
	 */
	private void etsiidt(JsonObject o, List<Tiedosto> ls) {
		
		if (null != o ) {
			JsonElement i = o.get("identifier");
			if (null != i) {
				JsonElement j = o.get("file_path");
				ls.add(new Tiedosto(j.getAsString(), i.getAsString()));
			}
		}
	}

	/**
	 * selvittää hakemiston tiedostot
	 * 
	 * @param content String metaxin vastaus hakemistokyselyyn
	 * @return List<Tiedosto> tiedostolista
	 */
	public List<Tiedosto> dir(String content, Prosessor p) {
		List<Tiedosto> lsf = new ArrayList<Tiedosto>();
		//List<String> alihakemistot = new ArrayList<String>();
		Gson gson = new GsonBuilder().create();
		JsonArray jo = gson.fromJson(content, JsonArray.class);
		if (null != jo) {
			jo.forEach(f -> etsiid(f, lsf));		
		}
		return lsf;
	}
	
	/**
	 * Parsii tiedoston tunnisteen
	 * 
	 * @param o JsonElement tiedoston tiedot
	 * @param ls List<String>, lista johon tunniste lisätään
	 */
	private void etsiid(JsonElement o, List<Tiedosto> ls) {

		if (null != o) {
			JsonObject jo = o.getAsJsonObject();
			try {
				String fp = jo.get("file_path").getAsString();
				String id = jo.get("identifier").getAsString();
				//if (jo.get("open_access").getAsBoolean())
					ls.add(new Tiedosto(fp, id));
				/*else
					System.out.println(id+" Ei ollut avoin "+fp);*/
			} catch (java.lang.NullPointerException e) {
				System.err.println("Tiedoston tiedot vaillinaiset"+e.getMessage());
			}					

		}
	}

	public String name(String jsons) {
		Gson gson = new GsonBuilder().create();
		JsonObject jo = gson.fromJson(jsons, JsonObject.class);
		Boolean oa = jo.get("open_access").getAsBoolean();
		if (null != oa) {
			if (!oa) return ACCESSFALSE;
		}
		return jo.get("file_path").getAsString();
		//return jo.get("file_name").getAsString();
	}

}
