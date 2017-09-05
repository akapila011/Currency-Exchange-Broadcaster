
import java.io.Serializable;

public class CurrencyData implements Serializable{

    private static final int classSerialID = 1;

    // Holds all data for retrieved currencies (i.e. online data)
    public String gbpName;
    public String gbpRate;

    public String eurName;
    public String eurRate;

    public String cadName;
    public String cadRate;

    public String kshsName;
    public String kshsRate;

    public String tzsName;
    public String tzsRate;

    public String date;
    public String time;

    public CurrencyData(String[] retrievedCurrencyData){
        gbpName = retrievedCurrencyData[0];
        gbpRate = retrievedCurrencyData[1];
        eurName = retrievedCurrencyData[2];
        eurRate = retrievedCurrencyData[3];
        cadName = retrievedCurrencyData[4];
        cadRate = retrievedCurrencyData[5];
        kshsName = retrievedCurrencyData[6];
        kshsRate = retrievedCurrencyData[7];
        tzsName = retrievedCurrencyData[8];
        tzsRate = retrievedCurrencyData[9];
        date = retrievedCurrencyData[10];
        time = retrievedCurrencyData[11];
    }

    public boolean saveMe(){
        try{
            DataHandler dh = new DataHandler();
            dh.insertRate(new String[] {gbpName, gbpRate, date, time});
            dh.insertRate(new String[] {eurName, eurRate, date, time});
            dh.insertRate(new String[] {cadName, cadRate, date, time});
            dh.insertRate(new String[] {kshsName, kshsRate, date, time});
            dh.insertRate(new String[] {tzsName, tzsRate, date, time});
            return true;
        }
        catch (Exception ex){
            return false;
        }
    }
}
