import java.io.Serializable;
import java.util.ArrayList;

public class Datas implements Serializable { // 초기화용 데이터 클래스
    private static final long serialVersionUID = 1L;
    private ArrayList<Food> Foods;
    private Players Players;


    public Datas(ArrayList<Food> Foods, Players Players) {
        this.Foods =Foods;
        this.Players = Players;
    }

    // Getter and Setter
    public ArrayList<Food> getFoods() {
        return Foods;
    }

    public void setFoods(ArrayList<Food> F){
        this.Foods = F;
    }

    public Players getPlayers() {
        return Players;
    }

    public void setPlayers(Players P){
        Players = P;
    }



}
