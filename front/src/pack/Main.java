package pack;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/Main", "/api/joueurs", "/api/games", "/api/games/*" })
public class Main extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String BACK_BASE_URL = "http://localhost:8080/back";

    private transient ResteasyClient client;
    private transient FacadeClient facade;
    private final transient ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init() throws ServletException {
        this.client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(BACK_BASE_URL));
        this.facade = target.proxy(FacadeClient.class);
    }
 
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if ("/api/joueurs".equals(servletPath)) {
            serveJoueursJson(response);
            return;
        }

        if (servletPath != null && servletPath.startsWith("/api/games")) {
            serveGamesApi(request, response);
            return;
        }

        request.getRequestDispatcher("/index.html").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if ("/api/joueurs".equals(servletPath)) {
            proxyCreateJoueur(request, response);
            return;
        }

        if (servletPath != null && servletPath.startsWith("/api/games")) {
            proxyGamesApi(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if (servletPath == null || !servletPath.startsWith("/api/games")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String pathInfo = getGamesPathInfo(request);
        Long gameId = parseGameId(pathInfo);
        if (gameId == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        proxyDeleteGame(gameId.longValue(), response);
    }

    @Override
    public void destroy() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void serveJoueursJson(HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        List<JoueurDto> joueurs;
        try {
            joueurs = (facade == null) ? List.of() : facade.listJoueurs();
            if (joueurs == null) joueurs = List.of();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            joueurs = List.of();
        }

        objectMapper.writeValue(response.getOutputStream(), joueurs);
    }

    private void serveGamesApi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // /api/games           -> list
        // /api/games/{id}      -> get
        String pathInfo = getGamesPathInfo(request);
        if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isBlank()) {
            proxyListGames(response);
            return;
        }

        // /api/games/{id}/state
        if (pathInfo.matches("/\\d+/state")) {
            String[] segs = pathInfo.substring(1).split("/");
            proxyGetGameState(request, response, Long.parseLong(segs[0]));
            return;
        }

        Long gameId = parseGameId(pathInfo);
        if (gameId == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        proxyGetGame(gameId.longValue(), response);
    }

    private void proxyGamesApi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // POST /api/games                 -> create
        // POST /api/games/{id}/join       -> join
        // POST /api/games/{id}/start      -> start
        // POST /api/games/{id}/leave      -> leave
        String pathInfo = getGamesPathInfo(request);
        if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isBlank()) {
            proxyCreateGame(request, response);
            return;
        }

        String normalized = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        String[] parts = normalized.split("/");
        if (parts.length < 2) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long gameId;
        try {
            gameId = Long.valueOf(parts[0]);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String action = parts[1];
        if ("join".equals(action)) {
            proxyJoinGame(request, response, gameId.longValue());
            return;
        }
        if ("start".equals(action)) {
            proxyStartGame(request, response, gameId.longValue());
            return;
        }
		if ("leave".equals(action)) {
			proxyLeaveGame(request, response, gameId.longValue());
			return;
		}
        if ("play".equals(action)) {
            proxyPlayCard(request, response, gameId.longValue());
            return;
        }
        if ("draw".equals(action)) {
            proxyDrawCard(request, response, gameId.longValue());
            return;
        }
        if ("uno".equals(action)) {
            proxyCallUno(request, response, gameId.longValue());
            return;
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Selon le mapping effectif du servlet, Tomcat peut mettre le reste du chemin
     * soit dans pathInfo, soit directement dans servletPath.
     */
    private static String getGamesPathInfo(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            return pathInfo;
        }

        String servletPath = request.getServletPath();
        if (servletPath == null) {
            return null;
        }
        if (!servletPath.startsWith("/api/games")) {
            return null;
        }

        String rest = servletPath.substring("/api/games".length());
        return (rest == null || rest.isBlank()) ? "/" : rest;
    }

    private void proxyListGames(HttpServletResponse response) throws IOException {
        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.listGames();
            pipeBackResponse(backResponse, response, "application/json");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyGetGame(long gameId, HttpServletResponse response) throws IOException {
        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.getGame(gameId);
            pipeBackResponse(backResponse, response, "application/json");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyCreateGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");

        GameCreateRequest body;
        try {
            body = objectMapper.readValue(request.getInputStream(), GameCreateRequest.class);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("invalid json");
            return;
        }

        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.createGame(body);
            pipeBackResponse(backResponse, response, "application/json");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyJoinGame(HttpServletRequest request, HttpServletResponse response, long gameId) throws IOException {
        Long joueurId = parseLongQueryParam(request, "joueurId");
        if (joueurId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("joueurId requis");
            return;
        }

        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.joinGame(gameId, joueurId.longValue());
            pipeBackResponse(backResponse, response, "application/json");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyStartGame(HttpServletRequest request, HttpServletResponse response, long gameId) throws IOException {
        Long joueurId = parseLongQueryParam(request, "joueurId");
        if (joueurId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("joueurId requis");
            return;
        }

        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.startGame(gameId, joueurId.longValue());
            pipeBackResponse(backResponse, response, "application/json");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyLeaveGame(HttpServletRequest request, HttpServletResponse response, long gameId) throws IOException {
        Long joueurId = parseLongQueryParam(request, "joueurId");
        if (joueurId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("joueurId requis");
            return;
        }

        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.leaveGame(gameId, joueurId.longValue());
            // 204 possible si partie supprimée
            pipeBackResponse(backResponse, response, "application/json");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyDeleteGame(long gameId, HttpServletResponse response) throws IOException {
        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.deleteGame(gameId);
            pipeBackResponse(backResponse, response, "text/plain");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyGetGameState(HttpServletRequest request, HttpServletResponse response, long gameId) throws IOException {
        Long joueurId = parseLongQueryParam(request, "joueurId");
        if (joueurId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("joueurId requis");
            return;
        }
        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.getGameState(gameId, joueurId.longValue());
            pipeBackResponse(backResponse, response, "application/json");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyPlayCard(HttpServletRequest request, HttpServletResponse response, long gameId) throws IOException {
        Long joueurId = parseLongQueryParam(request, "joueurId");
        Long cardId   = parseLongQueryParam(request, "cardId");
        if (joueurId == null || cardId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("joueurId et cardId requis");
            return;
        }
        String chosenColor = request.getParameter("chosenColor");
        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.playCard(gameId, joueurId.longValue(), cardId.longValue(), chosenColor);
            pipeBackResponse(backResponse, response, "application/json");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyDrawCard(HttpServletRequest request, HttpServletResponse response, long gameId) throws IOException {
        Long joueurId = parseLongQueryParam(request, "joueurId");
        if (joueurId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("joueurId requis");
            return;
        }
        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.drawCard(gameId, joueurId.longValue());
            pipeBackResponse(backResponse, response, "application/json");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void proxyCallUno(HttpServletRequest request, HttpServletResponse response, long gameId) throws IOException {
        Long joueurId = parseLongQueryParam(request, "joueurId");
        if (joueurId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("joueurId requis");
            return;
        }
        javax.ws.rs.core.Response backResponse = null;
        try {
            backResponse = facade.callUno(gameId, joueurId.longValue());
            pipeBackResponse(backResponse, response, "text/plain");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        } finally {
            closeQuietly(backResponse);
        }
    }

    private void pipeBackResponse(javax.ws.rs.core.Response backResponse, HttpServletResponse response, String contentType)
            throws IOException {
        if (backResponse == null) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
            return;
        }

        response.setCharacterEncoding("UTF-8");
        response.setStatus(backResponse.getStatus());
        response.setContentType(contentType);

        String payload;
        try {
            payload = backResponse.readEntity(String.class);
        } catch (Exception e) {
            payload = "";
        }
        if (payload != null && !payload.isBlank()) {
            response.getWriter().write(payload);
        }
    }

    private static void closeQuietly(javax.ws.rs.core.Response r) {
        if (r == null) return;
        try {
            r.close();
        } catch (Exception e) {
            // ignore
        }
    }

    private static Long parseLongQueryParam(HttpServletRequest request, String name) {
        String raw = request.getParameter(name);
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseGameId(String pathInfo) {
        if (pathInfo == null) return null;
        String normalized = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        if (normalized.isBlank()) return null;

        // accepte "/{id}" uniquement
        if (normalized.contains("/")) return null;
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void proxyCreateJoueur(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");

        JoueurCreateRequest body;
        try {
            body = objectMapper.readValue(request.getInputStream(), JoueurCreateRequest.class);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("invalid json");
            return;
        }

        try {
            javax.ws.rs.core.Response backResponse = facade.addJoueur(body);
            int status = backResponse.getStatus();
            response.setStatus(status);
            response.setContentType("application/json");
            String payload = null;
            try {
                payload = backResponse.readEntity(String.class);
            } catch (Exception e) {
                payload = "";
            } finally {
                try {
                    backResponse.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            if (payload != null && !payload.isBlank()) {
                response.getWriter().write(payload);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("text/plain");
            response.getWriter().write("backend unavailable");
        }
    }
}