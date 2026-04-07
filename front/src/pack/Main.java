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

@WebServlet(urlPatterns = { "/Main", "/api/joueurs" })
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

        request.getRequestDispatcher("/index.html").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if ("/api/joueurs".equals(servletPath)) {
            proxyCreateJoueur(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
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