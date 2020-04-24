/**
 * 
 */
package fi.csc.fairdata.logindl;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * @author pj
 *
 */

@RestController
@RequestMapping("/secure/api/v1/dataset")
public class Controller {

	
	    @RequestMapping(value = "/{id}", method = RequestMethod.GET )
	    public void dataset(@PathVariable("id") String id,
	    		@RequestParam(value="file", required = false) String file,
	    		@RequestParam(value="dir", required = false) String dir,
	    		HttpServletResponse response) throws IOException {
	    	Dataset ds = new Dataset(id, file, dir, response);
	    	ds.käsittele();
	    }
		    
}
