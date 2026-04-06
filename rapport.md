# Intro

Dans ce premier rapport, l’architecture du projet sera détaillée, nous n’avons pas
encore commencé à implémenter le projet (nous commencerons pendant la première séance
en présentiel). Le but du projet est de développer une application web permettant de jouer
en multijoueur au UNO, avec des contraintes, comme un chat de discussion par exemple,
à respecter.

## 1.1 Fonctionnalités Principales

```
— Une page d’accueil principale qui permet de créer des parties (avec sélection de
règles optionnelles), de rejoindre des parties et d’accéder au classement des joueurs.
— Une page de partie où la partie va se dérouler avec les règles du UNO avec un chat
intégré affiché au-dessus des joueurs lors de l’envoi d’un message.
— Règles classiques du UNO respectées avec notamment les cartes spéciales, pos-
sibilité de sélectionner le nombre de joueurs et possibilité de rajouter des règles
optionnelles, par exemple piocher jusqu’à ce que l’on puisse jouer.
— Système de lobby pour créer et rejoindre les parties.
— Historique de parties et leaderboard.
```
## 1.2 Choix esthétiques (Design)

Bien que cela ne soit pas demandé explicitement dans les attendus du projet,
nous avons décidé de réfléchir rapidement à quoi notre site ressemblerait visuellement.
Nous voulons un fond animé (sous un effet de flou) et des assets pour les cartes, les icônes
de joueurs, etc, qui soient dans un style pixelisé/pixel art (référence au jeu Balatro).

# 2. Architecture Générales

Bien entendu, notre application respectera le pattern MVC (Modèle-Vue-Contrôleur)
que nous avons vu en cours et qui est demandé pour ce projet

## 2.1 Communication en temps réel

Comme la communication en temps réel entre les joueurs est dans les attendus du
projet, nous allons utiliser des WebSockets afin d’établir une connexion persistante (ac-
cessible en permanence par les deux côtés) entre le(s) client et le serveur. Chaque client
aura une session sur la socket dédiée pour envoyer et recevoir des informations comme le
fait qu’une carte est jouée ou bien envoyer/recevoir un message.

## 2.2 Frontend dynamique

Pour avoir une expérience utilisateur plus immersive l’on va utiliser l’API Fetch Ja-
vaScript permettant une mise à jour de la page sans rechargement complet et évitant donc
du temps de load et un écran qui clignote à chaque changement d’informations.


# 3. Modèle de données

L’application va définir des entités JPA pour représenter les données nécessaires.
Chaque entité correspond à une table de base de données. Cela va être géré par Spring
Boot.

## 3.

```
Entité Table DB Description
Player player Joueur identifié (pseudo, mot de
passe, stats)
Game game Partie en cours ou terminée (état,
règles, date)
GamePlayer game_player Participation d’un joueur à une par-
tie (main, ordre)
Card card Carte du jeu (couleur, type, valeur)
HandCard hand_card Carte dans la main d’un joueur pen-
dant une partie
DrawPile draw_pile Pile de pioche (cartes restantes or-
données)
DiscardPile discard_pile Pile de défausse (cartes jouées)
ChatMessage chat_message Message de chat lié à une partie
GameRule game_rule Règles optionnelles activées pour
une partie
LeaderboardEntry leaderboard_entry Entrée de classement calculée
```
## 3.2 Associations entre les entités

```
Nous avons quelques idées de relation entre entités :
— Game→ Player : OneToMany, une partie possède plusieurs joueurs.
— Game→ GameRule : OneToMany, une partie peut avoir plusieurs règles option-
nelles activées.
— Game→ DrawPile, DiscardPile : OneToOne, une partie possède une pile de pioche
et une pile de défausse.
— GamePlayer→ HandCard : OneToMany, chaque participation a une collection de
cartes en main.
```
# 4. Backend

Le backend sera développé avec Spring Boot avec un restController qui lui gérera la
logique du jeu. Les données seront persistées via des repositories JPA.

## 4.1 Endpoints REST principaux

```
POST /createGame Créer une nouvelle partie
GET /listGames Lister les parties pour afficher les lobbys en cours
POST /joinGame Rejoindre une partie existante
POST /shuffle Vide la pile actuelle et remélange la pioche
```

## 4.2 Facade

C’est la Facade qui encapsulera toutes les règles du jeu, qui vérifiera si une carte est
jouable ou bien que la partie n’est pas terminée.

## 4.3 Sessions

L’identification des joueurs se fera via les sessions HTTP gérées par le Servlet. On
annule la session au moment de la déconnexion du joueur.

# 5. Frontend

## 5.1 Pages visuelles

Le frontend est construit avec JSP, HTML et CSS. Les JSP sont utilisées pour le rendu
des pages nécessitant des données dynamiques issues du Modèle (liste de parties, état du
jeu). Les pages statiques (accueil, règles) sont en HTML. Le CSS assure le fond animé
par exemple (pas demandé par les consignes du projet).

## 5.2 Communication temps réel côté client

La connexion WebSocket se fera dès la connexion sur le site. Le chat fonctionne grâce
à ces sockets qui envoient les informations au serveur pour ensuite les diffuser aux autres
joueurs.

## 5.3 Architecture des pages principales

```
— Page accueil : boutons pour créer une partie, rejoindre une partie, afficher le lea-
derboard
— Lobby : affichage des joueurs déjà connectés à la partie, choix des règles pour la
personne ayant créé la partie, bouton pour démarrer le jeu.
— Jeu : main du joueur, pile de défausse, chat de discussion et affichage du jeu en
cours.
— Leaderboard : tableau des meilleurs joueurs.
```
# 6 Conclusion

Nous combinons donc les technologies vues en cours pour pouvoir construire notre
projet UNO en ligne de façon cohérente et logique. Les prochaines étapes sont donc de se
répartir le travail et de commencer à coder les différentes parties du projet.


