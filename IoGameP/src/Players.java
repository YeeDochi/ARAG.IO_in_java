import java.io.Serializable;
import java.util.ArrayList;

public class Players implements Serializable { //플레이어 리스트 클래스
    ArrayList<Player> players = new ArrayList<>();

    public void addPlayer(Player p){
        players.add(p);
    }

    public void rmPlayer(Player p){
        players.remove(p);
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }
    public void setPlayers(ArrayList<Player> players) {
        this.players = players;
    }
}
