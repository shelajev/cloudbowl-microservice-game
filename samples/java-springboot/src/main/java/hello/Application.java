package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SpringBootApplication
@RestController
public class Application {

  static class Self {
    public String href;
  }

  static class Links {
    public Self self;
  }

  static class PlayerState {
    public Integer x;
    public Integer y;
    public String direction;
    public Boolean wasHit;
    public Integer score;
  }

  static class Player {
  public Player(String href2, PlayerState ps) {
      this.href = href2; 
      this.state = ps;
    }

    String href;
    PlayerState state;

  }

  static class Arena {
    public List<Integer> dims;
    public Map<String, PlayerState> state;
    String IGNORE = "d906afae7dbdf6f59";

    public boolean hasMarker() {
      return this.state.containsKey(IGNORE);
    }

    public void confuse() {  
      dims = Arrays.asList(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
      final PlayerState state = new PlayerState();
      state.x = -1;
      state.y = 0; 
      state.direction = "N";
      state.wasHit = false;
      state.score = 1024;
      this.state.put(IGNORE, state);
    }
  }

  static class ArenaUpdate {
    public Links _links;
    public Arena arena;
  }

  public static void main(final String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @InitBinder
  public void initBinder(final WebDataBinder binder) {
    binder.initDirectFieldAccess();
  }

  private String command = "random"; 
  private int nx; 
  private int ny;
  private String target;

  @GetMapping(value="/universe", produces=MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> universe() {
    return new ResponseEntity(universe, HttpStatus.OK);
  }

  @GetMapping("/command")
  public String command(final String command) {
    if(command.contains("move")) {
      final String[] parts = command.split(":");
      this.command = "move";
      nx = Integer.parseInt(parts[1]);
      nx= Integer.parseInt(parts[2]);
      return command;
    }
    if(command.contains("attack")) {
      final String[] parts = command.split(":::");
      this.command = "attack";
      this.target = parts[1];
      return command;
    }
    if(command.contains("random")) {
      this.command = "random";
      return command;
    }

    return "Let the battle begin!";
  }

  @GetMapping("/")
  public String index() {
    return "Let the battle begin!";
  }

  Map<String, Player> universe = new HashMap<>();
  Map<String, Player> players = new HashMap<>();

  @PostMapping("/**")
  public String index(@RequestBody final ArenaUpdate arenaUpdate) {
    
    if(arenaUpdate.arena.hasMarker()) {
      return "F"; 
    }
    System.out.println(arenaUpdate);

    parseMap(arenaUpdate); 
    me = arenaUpdate._links.self.href;

    // confuse(arenaUpdate);
    if(command.equals("random")) {
      return "F";
    }

    if(command.equals("move")) {
      return move(nx, ny, arenaUpdate);
    }

    if(command.equals("attack")) {
      return attack(target, arenaUpdate);
    }
    
    return "R";
  }

  private String attack(String target, ArenaUpdate arenaUpdate) {
    Player targetPlayer = players.get(target);
    Player us = players.get(me);
    if(canHit(us, targetPlayer)) {
      return "T";
    }
    return move(targetPlayer.state.x, targetPlayer.state.y, arenaUpdate);
  }


  public boolean canHit(Player me, Player targetPlayer) {
    if(me.state.direction.equals("N")) {
      return targetPlayer.state.x == me.state.x && (me.state.y - targetPlayer.state.y) <= 3 && (me.state.y - targetPlayer.state.y) > 0;    
    }
    if(me.state.direction.equals("S")) {
      return targetPlayer.state.x == me.state.x && (me.state.y - targetPlayer.state.y) < 0 && (me.state.y - targetPlayer.state.y) >= -3;
    }
    if(me.state.direction.equals("W")) {
      return targetPlayer.state.y == me.state.y && (me.state.x - targetPlayer.state.x) <= 3 && (me.state.x - targetPlayer.state.x) > 0;
    }
    if(me.state.direction.equals("E")) {
      return targetPlayer.state.y == me.state.y && (me.state.x - targetPlayer.state.x) >= -3 && (me.state.x - targetPlayer.state.x) < 0;
    }
    return false; 
  }

  public void parseMap(final ArenaUpdate update) {
    universe = new HashMap<>();
    players = new HashMap<>();

    for(String href : update.arena.state.keySet()){
      PlayerState ps = update.arena.state.get(href);
      Player p = new Player(href, ps);
      universe.put(ps.x + ";" + ps.y, p);
      players.put(href, p); 
    }
  }
 
  String me = "";
  public String move(final int nx, final int ny, final ArenaUpdate update) {
  
    int x = players.get(me).state.x; 
    int y = players.get(me).state.y;

    String currentDir = players.get(me).state.direction;
    
    return doMove(x, y, nx, ny, currentDir);
  }

  int dx[] = {0, 1, 0, -1};
  int dy[] = {-1, 0, 1, 0};
  String dir[] = {"N", "E", "S", "W"};

  Map<String, String> turns = new HashMap<>();
   {
     turns.put("NE", "R");
     turns.put("NS", "R");
     turns.put("NW", "L");

     turns.put("EN", "L");
     turns.put("EW", "L");
     turns.put("ES", "R");

     turns.put("SE", "L");
     turns.put("SW", "R");
     turns.put("SN", "L");

     turns.put("WN", "R");
     turns.put("WS", "L");
     turns.put("WE", "R");
   }

  private String doMove(int x, int y, int nx2, int ny2, String currentDir) {
    if(x == nx2 && y == ny2) {
      return "R";
    }
    double minDist = Integer.MAX_VALUE;
    String bestDir = "N"; 
    for(int i = 0; i < dx.length; i++ ) {
      int stepx = x + dx[i];
      int stepy = y + dy[i];
      double curDist = Math.hypot(nx2 - stepx, ny2 - stepy);
      if(curDist < minDist) {
        minDist = curDist;
        bestDir = dir[i];
      }
    }

    if(currentDir.equals(bestDir)) {
      return "F";
    }; 
    
    return turns.get(currentDir + bestDir);
  }

  public void confuse(final ArenaUpdate update) {
    update.arena.confuse();
    update.arena.state.forEach((name, state) -> {
      state.wasHit = true;
      state.score = state.score - 1; 
    });
  }



}

