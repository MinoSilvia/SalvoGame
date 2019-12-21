package com.codeoftheweb.salvo.models;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long   id;

    @OneToMany(mappedBy = "player", fetch = FetchType.EAGER)
    List<GamePlayer> gamePlayers;

    @OneToMany(mappedBy = "player",fetch = FetchType.EAGER)
    Set<Score> scores;

    private String  email;
    private String password;


    public  Player(){}

    public Player (String email, String password){
        this.email = email;
        this.password   =   password;
    }

    public Map<String, Object> makePlayerDTO(){

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", this.getId());
        dto.put("email", this.email);
        return dto;
    }

    public Optional<Score> getScore(Game game){
        Optional<Score> score = this.getScores()
                .stream()
                .filter(score1 -> score1.getGame().getId()   ==  game.getId())
                .findFirst();

        return  score;
    }

    public Map<String,Object>   makePlayerScoreDTO(){
        Map<String,  Object>    dto =    new LinkedHashMap<>();
        Map<String,  Object>    score =    new LinkedHashMap<>();

        dto.put("id",   this.getId());
        dto.put("email", this.getEmail());
        dto.put("score",score);
        score.put("total", this.getTotalScore());
        score.put("won", this.getWinScore());
        score.put("lost", this.getLostScore());
        score.put("tied", this.getTiedScore());
        return  dto;
    }

    public Double   getTotalScore(){
        return  this.getWinScore() * 1.0D  +   this.getTiedScore()  * 0.5D;
    }

    public long  getWinScore(){
        return this.getScores().stream()
                .filter(score -> score.getScore()   == 1.0D)
                .count();
    }

    public long  getLostScore(){
        return this.getScores().stream()
                .filter(score -> score.getScore()   == 0.0D)
                .count();
    }

    public long  getTiedScore(){
        return this.getScores().stream()
                .filter(score -> score.getScore()   == 0.5D)
                .count();
    }


    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(List<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public Set<Score> getScores() {
        return scores;
    }

    public void setScores(Set<Score> scores) {
        this.scores = scores;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}


