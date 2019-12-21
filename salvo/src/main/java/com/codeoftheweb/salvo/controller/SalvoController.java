package com.codeoftheweb.salvo.controller;

import com.codeoftheweb.salvo.models.*;
import com.codeoftheweb.salvo.repositories.*;
import com.codeoftheweb.salvo.util.GameState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SalvoController {

    @Autowired
    private GameRepository  gameRepository;

    @Autowired
    private GamePlayerRepository  gamePlayerRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private SalvoRepository salvoRepository;


    @RequestMapping("/game_view/{nn}")
    public ResponseEntity<Map<String, Object>> getGameViewByGamePlayerID(@PathVariable Long nn, Authentication  authentication) {

        if(isGuest(authentication)){
            return new  ResponseEntity<>(makeMap("error","Paso algo"),HttpStatus.UNAUTHORIZED);
        }

        Player  player  = playerRepository.findByEmail(authentication.getName());
        GamePlayer gamePlayer = gamePlayerRepository.findById(nn).orElse(null);

        if(player ==  null){
            return new  ResponseEntity<>(makeMap("error","Paso algo"),HttpStatus.UNAUTHORIZED);
        }

        //if(gamePlayer ==  null ){
         //   return new  ResponseEntity<>(makeMap("error","Paso algo"),HttpStatus.UNAUTHORIZED);
       // }

        if(gamePlayer.getPlayer().getId() !=  player.getId()){
            return new  ResponseEntity<>(makeMap("error","Paso algo"),HttpStatus.CONFLICT);
        }


        Map<String,  Object>  dto = new LinkedHashMap<>();
        Map<String, Object> hits = new LinkedHashMap<>();

        hits.put("self", gethits(gamePlayer, gamePlayer.getOpponent()));
        hits.put("opponent", gethits(gamePlayer.getOpponent(),  gamePlayer));

        dto.put("id", gamePlayer.getGame().getId());
        dto.put("created",  gamePlayer.getGame().getCreated());
        dto.put("gameState", getGameState(gamePlayer));
        dto.put("gamePlayers", gamePlayer.getGame().getGamePlayers()
                .stream()
                .map(gamePlayer1 -> gamePlayer1.makeGamePlayerDTO())
                .collect(Collectors.toList()));
        dto.put("ships",  gamePlayer.getShips()
                .stream()
                .map(ship -> ship.makeShipDTO())
                .collect(Collectors.toList()));
        dto.put("salvoes",  gamePlayer.getGame().getGamePlayers()
                .stream()
                .flatMap(gamePlayer1 -> gamePlayer1.getSalvoes()
                        .stream()
                        .map(salvo -> salvo.makeSalvoDTO()))
                .collect(Collectors.toList()));
        dto.put("hits", hits);

        return  new ResponseEntity<>(dto,HttpStatus.OK);
    }


    @RequestMapping(path = "/game/{gameID}/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long gameID, Authentication authentication) {
        if (isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error", "You can't join a Game if You're Not Logged In!"), HttpStatus.UNAUTHORIZED);
        }

        Player  player  = playerRepository.findByEmail(authentication.getName());
        Game gameToJoin = gameRepository.getOne(gameID);

        // assert (gameToJoin != null);

        if (gameRepository.getOne(gameID) == null) {
            return new ResponseEntity<>(makeMap("error", "No such game."), HttpStatus.FORBIDDEN);
        }

        if(player ==  null){
            return new ResponseEntity<>(makeMap("error", "No such game."), HttpStatus.FORBIDDEN);
        }

        int gamePlayersCount = gameToJoin.getGamePlayers().size();

        if (gamePlayersCount == 1) {
            GamePlayer gameplayer = gamePlayerRepository.save(new GamePlayer(gameToJoin, player));
            return new ResponseEntity<>(makeMap("gpid", gameplayer.getId()), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(makeMap("error", "Game is full!"), HttpStatus.FORBIDDEN);
        }

    }

    @RequestMapping(path="/games/players/{gpid}/ships", method = RequestMethod.POST)
    public ResponseEntity<Map <String, Object>> placeShip(@PathVariable Long gpid, Authentication authentication, @RequestBody Set<Ship> ships){

        if (isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error", "You can't add ships if You're Not Logged In!"), HttpStatus.UNAUTHORIZED);
        }

        Player  player  = playerRepository.findByEmail(authentication.getName());
        GamePlayer  gamePlayer  = gamePlayerRepository.getOne(gpid);

        if(gamePlayer == null){
            return new ResponseEntity<>(makeMap("error","NO esta autorizado, Is guest"), HttpStatus.UNAUTHORIZED);
        }
        if(gamePlayer.getPlayer().getId() !=  player.getId()){
            return new ResponseEntity<>(makeMap("error","Los players no coinciden"), HttpStatus.FORBIDDEN);
        }
        if(!gamePlayer.getShips().isEmpty()){
            return new ResponseEntity<>(makeMap("error","NO esta autorizado ya tengo ships"), HttpStatus.UNAUTHORIZED);
        }
        ships.forEach(ship -> {
            ship.setGamePlayer(gamePlayer);
            shipRepository.save(ship);
        });

        return new ResponseEntity<>(makeMap("OK","Ship created"), HttpStatus.CREATED);
    }


    @RequestMapping("/leaderBoard")
    public  List<Map<String,Object>> leaderBoard(){

        return  playerRepository.findAll()
                .stream()
                .map(player  ->  player.makePlayerScoreDTO())
                .collect(Collectors.toList());
    }

    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }


    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    @RequestMapping(value = "/games/players/{gpid}/salvoes",  method = RequestMethod.POST)
    public ResponseEntity<Map>  addSalvo(@PathVariable long gpid, @RequestBody Salvo salvo, Authentication authentication){
        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error","NO esta autorizado"), HttpStatus.UNAUTHORIZED);
        }
        Player player  = playerRepository.findByEmail(authentication.getName());
        GamePlayer self  = gamePlayerRepository.getOne(gpid);
        if(player ==  null){
            return new ResponseEntity<>(makeMap("error","NO esta autorizado"), HttpStatus.UNAUTHORIZED);
        }
        if(self == null){
            return new ResponseEntity<>(makeMap("error","NO esta autorizado"), HttpStatus.UNAUTHORIZED);
        }
        if(self.getPlayer().getId() !=  player.getId()){
            return new ResponseEntity<>(makeMap("error","Los players no coinciden"), HttpStatus.FORBIDDEN);
        }
        GamePlayer  opponent  = self.getGame().getGamePlayers().stream()

                .filter(gamePlayer -> gamePlayer.getId()  !=  self.getId())
                .findFirst()
                .orElse(new GamePlayer());

        if(self.getSalvoes().size() <=  opponent.getSalvoes().size()){

            salvo.setTurn(self.getSalvoes().size()  + 1);
            salvo.setGamePlayer(self);
            salvoRepository.save(salvo);
            return  new ResponseEntity<>(makeMap("OK","Salvo created!!"), HttpStatus.CREATED);
        }

        return  new ResponseEntity<>(makeMap("Error","Ya jugaste"), HttpStatus.CREATED);
    }

    private List<Map> gethits(GamePlayer  self, GamePlayer  opponent){

        List<Map> hits  = new ArrayList<>();

        List <String> carrierLocation = getLocatiosByType("carrier",self);
        List <String> battleshipLocation = getLocatiosByType("battleship",self);
        List <String> submarineLocation = getLocatiosByType("submarine",self);
        List <String> destroyerLocation = getLocatiosByType("destroyer",self);
        List <String> patrolboatLocation = getLocatiosByType("patrolboat",self);

        long carrierDamage = 0;
        long battleshipDamage = 0;
        long submarineDamage = 0;
        long destroyerDamage = 0;
        long patrolboatDamage = 0;

        for (Salvo  salvo : opponent.getSalvoes()){

            long carrierHits = 0;
            long battleshipHits = 0;
            long submarineHits = 0;
            long destroyerHits = 0;
            long patrolboatHits = 0;
            long missed = salvo.getSalvoLocations().size();



            Map<String, Object> hitsMapPerTurn = new LinkedHashMap<>();
            Map<String, Object> damagesPerTurn = new LinkedHashMap<>();

            List<String> salvoLocationsList = new ArrayList<>();
            List<String> hitsLocations = new ArrayList<>();


            for (String locationsShot : salvo.getSalvoLocations()) {

                if (carrierLocation.contains(locationsShot)) {

                    carrierDamage++;
                    carrierHits++;
                    hitsLocations.add(locationsShot);
                    missed--;
                }
                if (battleshipLocation.contains(locationsShot)) {
                    battleshipDamage++;
                    battleshipHits++;
                    hitsLocations.add(locationsShot);
                    missed--;
                }
                if (submarineLocation.contains(locationsShot)) {
                    submarineDamage++;
                    submarineHits++;
                    hitsLocations.add(locationsShot);
                    missed--;
                }
                if (destroyerLocation.contains(locationsShot)) {
                    destroyerDamage++;
                    destroyerHits++;
                    hitsLocations.add(locationsShot);
                    missed--;
                }
                if (patrolboatLocation.contains(locationsShot)) {
                    patrolboatDamage++;
                    patrolboatHits++;
                    hitsLocations.add(locationsShot);
                    missed--;
                }
            }

            damagesPerTurn.put("carrierHits", carrierHits);
            damagesPerTurn.put("battleshipHits", battleshipHits);
            damagesPerTurn.put("submarineHits", submarineHits);
            damagesPerTurn.put("destroyerHits", destroyerHits);
            damagesPerTurn.put("patrolboatHits", patrolboatHits);

            damagesPerTurn.put("carrier", carrierDamage);
            damagesPerTurn.put("battleship", battleshipDamage);
            damagesPerTurn.put("submarine", submarineDamage);
            damagesPerTurn.put("destroyer", destroyerDamage);
            damagesPerTurn.put("patrolboat", patrolboatDamage);

            hitsMapPerTurn.put("turn", salvo.getTurn());
            hitsMapPerTurn.put("hitLocations", hitsLocations);
            hitsMapPerTurn.put("damages", damagesPerTurn);
            hitsMapPerTurn.put("missed", missed);

            hits.add(hitsMapPerTurn);
        };

        return hits;
    }

    private GameState getGameState (GamePlayer gamePlayer) {

        if (gamePlayer.getShips().size() == 0) {
            return GameState.PLACESHIPS;
        }
        if (gamePlayer.getGame().getGamePlayers().size() == 1){
            return GameState.WAITINGFOROPP;
        }
        if (gamePlayer.getGame().getGamePlayers().size() == 2) {

            GamePlayer opponentGp = gamePlayer.getOpponent();

            if ((gamePlayer.getSalvoes().size() == opponentGp.getSalvoes().size()) && (getIfAllSunk(opponentGp, gamePlayer)) && (!getIfAllSunk(gamePlayer, opponentGp))) {
                return GameState.WON;
            }
            if ((gamePlayer.getSalvoes().size() == opponentGp.getSalvoes().size()) && (getIfAllSunk(opponentGp, gamePlayer)) && (getIfAllSunk(gamePlayer, opponentGp))) {
                return GameState.TIE;
            }
            if ((gamePlayer.getSalvoes().size() == opponentGp.getSalvoes().size()) && (!getIfAllSunk(opponentGp, gamePlayer)) && (getIfAllSunk(gamePlayer, opponentGp))) {
                return GameState.LOST;
            }

            if ((gamePlayer.getSalvoes().size() == opponentGp.getSalvoes().size()) && (gamePlayer.getId() < opponentGp.getId())) {
                return GameState.PLAY;
            }
            if (gamePlayer.getSalvoes().size() < opponentGp.getSalvoes().size()){
                return GameState.PLAY;
            }
            if ((gamePlayer.getSalvoes().size() == opponentGp.getSalvoes().size()) && (gamePlayer.getId() > opponentGp.getId())) {
                return GameState.WAIT;
            }
            if (gamePlayer.getSalvoes().size() > opponentGp.getSalvoes().size()){
                return GameState.WAIT;
            }

        }
        return GameState.UNDEFINED;
    }


    private List<String>  getLocatiosByType(String type, GamePlayer self){
        return  self.getShips().size()  ==  0 ? new ArrayList<>() : self.getShips().stream().filter(ship -> ship.getType().equals(type)).findFirst().get().getShipLocations();
    }

    private Boolean getIfAllSunk (GamePlayer self, GamePlayer opponent) {

        if(!opponent.getShips().isEmpty() && !self.getSalvoes().isEmpty()){
            return opponent.getSalvoes().stream().flatMap(salvo -> salvo.getSalvoLocations().stream()).collect(Collectors.toList()).containsAll(self.getShips().stream()
                    .flatMap(ship -> ship.getShipLocations().stream()).collect(Collectors.toList()));
        }
        return false;
    }

}
