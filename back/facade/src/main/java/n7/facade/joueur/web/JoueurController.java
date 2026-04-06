package n7.facade.joueur.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import n7.facade.joueur.model.Joueur;
import n7.facade.joueur.service.JoueurService;
import n7.facade.joueur.web.dto.JoueurRequest;
import n7.facade.joueur.web.dto.JoueurResponse;

@RestController
@RequestMapping("/joueurs")
public class JoueurController {

	private final JoueurService joueurService;

	public JoueurController(JoueurService joueurService) {
		this.joueurService = joueurService;
	}

	@PostMapping
	public ResponseEntity<JoueurResponse> creer(@RequestBody JoueurRequest request) {
		Joueur joueur = joueurService.creer(request.getPseudo());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new JoueurResponse(joueur.getId(), joueur.getPseudo()));
	}

	@GetMapping
	public List<JoueurResponse> lister() {
		return joueurService.lister().stream()
				.map(j -> new JoueurResponse(j.getId(), j.getPseudo()))
				.toList();
	}
}
