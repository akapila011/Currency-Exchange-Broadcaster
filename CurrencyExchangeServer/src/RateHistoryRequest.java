import java.io.Serializable;

public class RateHistoryRequest implements Serializable{
    // Requests for exchange rate history for a specified currency pair

    public String rateName;

    public RateHistoryRequest(String name){
        rateName = name;
    }
}
