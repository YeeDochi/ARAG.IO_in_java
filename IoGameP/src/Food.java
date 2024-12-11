import java.io.Serializable;
import java.util.Random;

public class Food implements Objects, Serializable { // 먹이 단일 데이터 클래스
    Random random = new Random();
    private int X,Y;
    private int size = 150;
    private int id;

    public Food(int Max,int id){
        X = random.nextInt(Max);
        Y = random.nextInt(Max);
        this.id = id;
    }


    @Override
    public int getX() {
        return X;
    }

    @Override
    public int getY() {
        return Y;
    }

    public int getId(){
        return id;
    }

    public int getSize(){
        return size;
    }

    @Override
    public void setX(int x) {
        X = x;
    }

    @Override
    public void setY(int y) {
        Y = y;
    }


}
