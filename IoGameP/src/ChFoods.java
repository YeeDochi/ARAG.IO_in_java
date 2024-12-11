import java.io.Serializable;

public class ChFoods implements Serializable {
    //private static final long serialVersionUID = 1L;
    private Food Deleted;
    private Food New;

    public ChFoods(Food D,Food N){
        Deleted = D;
        New = N;
    }

    public void setDeleted(Food deleted) {
        Deleted = deleted;
    }
    public void setNew(Food new1) {
        New = new1;
    }

    public Food getDeleted() {
        return Deleted;
    }
    public Food getNew() {
        return New;
    }
}
