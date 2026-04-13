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

//PRoxy RestEasy coté client
@Path("/")
public interface FacadeClient {

	//liste des joueurs
	@GET
	@Path("/joueurs")
	@Produces("application/json")
	List<JoueurDto> listJoueurs();

	//créé un joueur avec le pseudo
	@POST
	@Path("/joueurs")
	@Consumes("application/json")
	@Produces("application/json")
	Response addJoueur(JoueurCreateRequest request);

	//recupère toutes les parties
	@GET
	@Path("/games")
	@Produces("application/json")
	Response listGames();

	//créé une partie
	@POST
	@Path("/games")
	@Consumes("application/json")
	@Produces("application/json")
	Response createGame(GameCreateRequest request);

	//récup les infos d'une partie
	@GET
	@Path("/games/{gameId}")
	@Produces("application/json")
	Response getGame(@PathParam("gameId") long gameId);

	//join un joueur a une partie
	@POST
	@Path("/games/{gameId}/join")
	@Produces("application/json")
	Response joinGame(@PathParam("gameId") long gameId, @QueryParam("joueurId") long joueurId);

	//lance la game
	@POST
	@Path("/games/{gameId}/start")
	@Produces("application/json")
	Response startGame(@PathParam("gameId") long gameId, @QueryParam("joueurId") long joueurId);

	//quitter un joueur de la partie
	@POST
	@Path("/games/{gameId}/leave")
	@Produces("application/json")
	Response leaveGame(@PathParam("gameId") long gameId, @QueryParam("joueurId") long joueurId);

	//supprimer une partie
	@javax.ws.rs.DELETE
	@Path("/games/{gameId}")
	Response deleteGame(@PathParam("gameId") long gameId);

	//Endpoint nécessaires pour faire tourner une game

	//etat du jeu via un joueur
	@GET
	@Path("/games/{gameId}/state")
	@Produces("application/json")
	Response getGameState(@PathParam("gameId") long gameId, @QueryParam("joueurId") long joueurId);

	//joue une carte
	@POST
	@Path("/games/{gameId}/play")
	@Produces("application/json")
	Response playCard(@PathParam("gameId") long gameId,
	                  @QueryParam("joueurId") long joueurId,
	                  @QueryParam("cardId") long cardId,
	                  @QueryParam("chosenColor") String chosenColor);

	//pioche une carte
	@POST
	@Path("/games/{gameId}/draw")
	@Produces("application/json")
	Response drawCard(@PathParam("gameId") long gameId, @QueryParam("joueurId") long joueurId);

	//dis uno quand on a qu'une carte
	@POST
	@Path("/games/{gameId}/uno")
	Response callUno(@PathParam("gameId") long gameId, @QueryParam("joueurId") long joueurId);
}
