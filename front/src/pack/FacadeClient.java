package pack;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Interface cliente RestEasy (proxy) — comme dans le TP.
 */
@Path("/")
public interface FacadeClient {

	@GET
	@Path("/joueurs")
	@Produces("application/json")
	List<JoueurDto> listJoueurs();

	@POST
	@Path("/joueurs")
	@Consumes("application/json")
	@Produces("application/json")
	Response addJoueur(JoueurCreateRequest request);
}
