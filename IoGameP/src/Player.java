import java.awt.*;
import java.io.Serializable;
import java.util.Random;

public class Player implements Objects, Serializable { // 플레이어 단일 클래스

    private int X, Y;
    private int size;
    private Color color;
    private int ID;
    private String name;

    public Player(int x, int y, int size,int id,String name) {
        X = x;
        Y = y;
        this.color = getRandomColor();
        this.size = size;
        ID = id;
        this.name = name;
    }

    private Color getRandomColor() {
        Random random = new Random();

        // RGB 값을 0~255 사이의 무작위 값으로 생성
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        return new Color(red, green, blue);
    }

    public int getX() {
        return this.X;
    }

    public int getY() {
        return this.Y;
    }

    public int getSize(){
        return size;
    }

    public Color getColor() {
        return color;
    }

    public void setY(int y) {
        Y = y;
    }

    public void setX(int x) {
        X = x;
    }

    public void setID(int id){
        ID = id;
    }

    public int getID(){
        return ID;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setSize(int size) {
        this.size = size;
    }
    public void incrSize(){
        this.size+=2;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

}
