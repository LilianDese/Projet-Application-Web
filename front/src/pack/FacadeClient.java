package pack;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

	@GET
	@Path("/games")
	@Produces("application/json")
	Response listGames();

	@POST
	@Path("/games")
	@Consumes("application/json")
	@Produces("application/json")
	Response createGame(GameCreateRequest request);

	@GET
	@Path("/games/{gameId}")
	@Produces("application/json")
	Response getGame(@PathParam("gameId") long gameId);

	@POST
	@Path("/games/{gameId}/join")
	@Produces("application/json")
	Response joinGame(@PathParam("gameId") long gameId, @QueryParam("joueurId") long joueurId);

	@POST
	@Path("/games/{gameId}/start")
	@Produces("application/json")
	Response startGame(@PathParam("gameId") long gameId, @QueryParam("joueurId") long joueurId);

	@POST
	@Path("/games/{gameId}/leave")
	@Produces("application/json")
	Response leaveGame(@PathParam("gameId") long gameId, @QueryParam("joueurId") long joueurId);

	@javax.ws.rs.DELETE
	@Path("/games/{gameId}")
	Response deleteGame(@PathParam("gameId") long gameId);
}
