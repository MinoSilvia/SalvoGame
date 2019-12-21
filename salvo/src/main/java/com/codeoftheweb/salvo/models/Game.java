package com.codeoftheweb.salvo.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER)
    Set<GamePlayer> gamePlayers;

    @OneToMany(mappedBy = "game",fetch = FetchType.EAGER)
    Set<Score> scores;

    private Date created;

    public Game() {
        this.created = new Date();
    }

    public Map<String,  Object> makeGameDTO(){
        Map<String,  Object>    dto=    new LinkedHashMap<>();
        dto.put("id",   this.getId());
        dto.put("created", this.getCreated());
        dto.put("gamePlayers",  this.getGamePlayers()
                .stream()
                .map(gamePlayer -> gamePlayer.makeGamePlayerDTO())
                .collect(Collectors.toList()));
        dto.put("scores",   this.getScores().stream().map(score -> score.makeScoreDTO()).collect(Collectors.toList()));
        /*dto.put("scores",   this.getGamePlayers().stream()
                                                .map(gamePlayer -> gamePlayer.getScore().get().makeScoreDTO())
                                                .collect(Collectors.toList()));*/
        return  dto;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Set<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(Set<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public Set<Score> getScores() {
        return scores;
    }

    public void setScores(Set<Score> scores) {
        this.scores = scores;
    }
}