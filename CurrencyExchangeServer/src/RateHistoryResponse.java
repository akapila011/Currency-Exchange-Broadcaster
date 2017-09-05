import java.io.Serializable;
import java.util.ArrayList;


public class RateHistoryResponse implements Serializable {

    public String rateName;
    public ArrayList<String[]> history;

    public RateHistoryResponse(String name){
        rateName = name;
    }

}
