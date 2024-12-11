
import java.io.Serializable;

public class Massage implements Serializable { //메시지 클래스
    private String massage;
    private String name;

    public Massage(String name,String massage){
        this.name = name;
        this.massage = massage;
    }

    public String getMassage() {
        return massage;
    }

    public String getName() {
        return name;
    }
   
}
