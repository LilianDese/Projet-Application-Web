package n7.facade.joueur.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import n7.facade.joueur.model.Joueur;
import n7.facade.joueur.repository.JoueurRepository;

@Service
public class JoueurService {

	private final JoueurRepository joueurRepository;

	public JoueurService(JoueurRepository joueurRepository) {
		this.joueurRepository = joueurRepository;
	}

	@Transactional
	public Joueur creer(String pseudo) {
		String normalized = normalize(pseudo);
		if (normalized == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pseudo requis");
		}
		if (joueurRepository.existsByPseudo(normalized)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "pseudo déjà utilisé");
		}
		return joueurRepository.save(new Joueur(normalized));
	}

	@Transactional(readOnly = true)
	public List<Joueur> lister() {
		return joueurRepository.findAll();
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
