
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;

public class CurrencyExchangeServer {
    // Server gets current exchange rates, listens for clients, sends current exchange rates to clients and
    // sends previous day exchange rates to clients

    private static int port = 9000;
    public HashMap<Socket, ObjectOutputStream> clients;
    public String[] currentRates;
    protected static int THREAD_SLEEP = 60000;      // (in milliseconds) 1 minute

    JFrame theFrame;
    JList serverLog;
    JList clientLog;
    DefaultListModel<String> serverLogModel;
    DefaultListModel<String> clientLogModel;

    public DataHandler dh;

    public static void main (String[] args){
        // user must provide bind port and broadcast interval
        String tempPort = JOptionPane.showInputDialog(null, "Enter the Port to bind to", "9000");
        try {
            port = Integer.parseInt(tempPort);
            if ((port < 1024) || (port > 60000)){
                JOptionPane.showMessageDialog(null, "Server can only be bound to a port between 1024 and 60000");
                System.exit(1);
            }
        }
        catch (Exception ex){
            JOptionPane.showMessageDialog(null, "The Port number provided in invalid");
            System.exit(1);
        }
        String broadcastIntervalTemp = JOptionPane.showInputDialog(null, "Enter Exchange Rate broadcast interval (in seconds)", "30");
        try{
            THREAD_SLEEP = Integer.parseInt(broadcastIntervalTemp) * 1000;
            if ((THREAD_SLEEP < 30000) || (THREAD_SLEEP > 300000)){  // broadcast interval must be greater than 30 seconds and less than 5 minutes
                JOptionPane.showMessageDialog(null, "Broadcast interval must be between 30 and 300 seconds");
                System.exit(1);
            }
        }
        catch (Exception ex){
            JOptionPane.showMessageDialog(null, "The Broadcast interval value is invalid");
            System.exit(1);
        }
        CurrencyExchangeServer ces = new CurrencyExchangeServer();
        ces.start();
    }

    protected void start(){
        // Start up the server to listen for incoming connections
        clients = new HashMap<Socket, ObjectOutputStream>();
        try{
            dh = new DataHandler();
            buildGUI();
            ServerSocket serverSock = new ServerSocket(port);
            serverLogModel.addElement("Server started on: " + serverSock.getLocalSocketAddress().toString());
            serverLogModel.addElement("Broadcasting rates every " + THREAD_SLEEP / 1000 + " seconds.");
            serverLogModel.addElement("Listening...");

            System.out.println("Server started on: " + serverSock.getLocalSocketAddress().toString());
            System.out.println("Broadcasting rates every " + THREAD_SLEEP / 1000 + " seconds.");
            System.out.println("Listening....");

            // Start loop to continuously get new exchange rates
            Thread updateAndBroadcastRates = new Thread(new CurrencyUpdater());
            updateAndBroadcastRates.start();

            while (true){
                // Accept new connections
                Socket clientSock = serverSock.accept();    // Accept incoming connection
                ObjectOutputStream out = new ObjectOutputStream(clientSock.getOutputStream());
                serverLogModel.addElement("--Got a new connection " + clientSock.getRemoteSocketAddress().toString());
                clientLogModel.addElement(clientSock.getRemoteSocketAddress().toString());
                System.out.println("--Got a new connection " + clientSock.getRemoteSocketAddress().toString());

                Thread clientHandler = new Thread(new ClientHandler(clientSock, out));
                clientHandler.start();
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void buildGUI(){
        // Build the GUI layout to give server details
        theFrame = new JFrame("Currency Exchange Server");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //theFrame.setExtendedState(JFrame.MAXIMIZED_VERT);
        theFrame.setResizable(false);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Box serverLogBox = new Box(BoxLayout.Y_AXIS);
        Box clientsBox = new Box(BoxLayout.Y_AXIS);

        JLabel serverLogTitle = new JLabel("Server Log");
        serverLogBox.add(serverLogTitle);

        serverLogModel = new DefaultListModel<>();   // Shows the server log
        serverLog = new JList(serverLogModel);
        serverLog.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //serverLog.setPreferredSize(new Dimension(750, 730));
        JScrollPane serverList = new JScrollPane(serverLog);
        serverList.setPreferredSize(new Dimension(800, 600));

        serverLogBox.add(serverList);

        JLabel clientsLogTitle = new JLabel("Connected Clients");
        clientsBox.add(clientsLogTitle);

        clientLogModel = new DefaultListModel<>();      // Shows all connected clients
        clientLog = new JList(clientLogModel);
        clientLog.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //clientLog.setPreferredSize(new Dimension(140, 550));
        JScrollPane clientList = new JScrollPane(clientLog);
        clientList.setPreferredSize(new Dimension(150, 600));

        clientsBox.add(clientList);

        background.add(BorderLayout.WEST, serverLogBox);
        background.add(BorderLayout.EAST, clientsBox);
        theFrame.getContentPane().add(background);
        theFrame.setBounds(50, 50, 950, 800);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    public CurrencyData buildCurrencyData(String[] data){
        // Clean JSON Data to give only required data
        // GBP
        String name1_1 = data[4].replace("\"Name\":", "");
        String name1_f = name1_1.replace("\"", "");
        String rate1_1 = data[5].replace("\"Rate\":", "");
        String rate1_f = rate1_1.replace("\"", "");
        // EUR
        String name2_1 = data[11].replace("\"Name\":", "");
        String name2_f = name2_1.replace("\"", "");
        String rate2_1 = data[12].replace("\"Rate\":", "");
        String rate2_f = rate2_1.replace("\"", "");
        // CAD
        String name3_1 = data[18].replace("\"Name\":", "");
        String name3_f = name3_1.replace("\"", "");
        String rate3_1 = data[19].replace("\"Rate\":", "");
        String rate3_f = rate3_1.replace("\"", "");
        // KSHS & TZS
        String kshsRate;
        String tzsRate;
        try{
            DecimalFormat kshsFormat = new DecimalFormat("#.00");
            DecimalFormat tzsFormat = new DecimalFormat("#.0");
            double kshsRate_f = Double.parseDouble(rate1_f) * 126.4;
            double tzsRate_f = Double.parseDouble(rate1_f) * 3377.5;
            kshsRate = kshsFormat.format(kshsRate_f);
            tzsRate = tzsFormat.format(tzsRate_f);
        }
        catch (Exception ex){
            kshsRate = "0.0000";
            tzsRate = "0.0000";
        }
        // Time - Get current GMT time
        final Date currentTime = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String currentTimeGMT = sdf.format(currentTime);
        // Date - Get current GMT date
        final Date currentDate = new Date();
        final SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyy");
        sdfDate.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdfDate.format(currentDate);

        String[] relevantData = new String[] {name1_f, rate1_f, name2_f, rate2_f, name3_f, rate3_f, "USD/KSHS", kshsRate, "USD/TZS", tzsRate, date, currentTimeGMT};
        return new CurrencyData(relevantData);
    }

    private String[] getCurrentRates() {
        // Uses Yahoo Finance API to get exchange rates for USD to GBP, EUR and CAD
        String result = "";
        try {
            URL yahoo = new URL("https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.xchange%" +
                    "20where%20pair%20in%20(%22USDGBP%22%2C%20%22USDEUR%22%2C%20%22USDCAD%22)&format=json&env=store%3A%2" +
                    "F%2Fdatatables.org%2Falltableswithkeys&callback=");

            URLConnection urlConn = yahoo.openConnection();
            BufferedReader inBuffer = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String inputLine;
            while ((inputLine = inBuffer.readLine()) != null){
                result += inputLine;
            }
            inBuffer.close();
            String[] finalResult = result.split(",");       // Split JSON data in an array
            return finalResult;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (currentRates == null){      // attribute currentRates has no data so update with custom values
            String[] finalResult = new String[] {"USD/GBP", "0.0000", "USD/EUR", "0.0000", "USD/CAD", "0.0000", "NODATE", "NOTIME"};
            return finalResult;     // Error, return an array with empty values
        }
        return currentRates;    // Error, but use previous exchange rate values
    }


    public class ClientHandler implements Runnable{

        public Socket client;
        public ObjectOutputStream cOut;
        public ObjectInputStream cIn;

        public ClientHandler(Socket clientSock, ObjectOutputStream clientOutput){
            client = clientSock;
            cOut = clientOutput;
        }

        public void run(){
            if (!(clients.containsKey(client))){  // Add clientOutputStreams only if not already in the list
                clients.put(client, cOut);
            }
            try{
                // Get data from client
                cIn = new ObjectInputStream(client.getInputStream());
                Object objIn;
                while (true){       // Keep listening for incoming requests
                    objIn = cIn.readObject();
                    if (objIn != null){
                        if (objIn instanceof String){   // A request to get current rates received
                            serverLogModel.addElement("--Instant Exchange Rate Update Request received from " + client.getRemoteSocketAddress().toString());
                            System.out.println("--Instant Exchange Rate Update Request received from " + client.getRemoteSocketAddress().toString());
                            CurrencyData cd = buildCurrencyData(getCurrentRates());
                            cOut.writeObject(cd);
                        }
                        else{   // A request (for exchange rate history) received
                            serverLogModel.addElement("--Exchange Rate History Request received from " + client.getRemoteSocketAddress().toString());
                            System.out.println("--Exchange Rate History Request received from " + client.getRemoteSocketAddress().toString());
                            RateHistoryRequest req = (RateHistoryRequest) objIn;    // Can only get one type of request
                            RateHistoryResponse resp = new RateHistoryResponse(req.rateName);
                            resp.history = dh.findRateHistory(req.rateName);
                            cOut.writeObject(resp);
                        }
                    }
                    Thread.sleep(5000);        // Stop listening for requests for 5 seconds
                }
            }
            catch (IOException ioEx){
                serverLogModel.addElement("--Client has disconnected " + client.getRemoteSocketAddress().toString());
                clientLogModel.removeElement(client.getRemoteSocketAddress().toString());
                System.out.println("--Client has disconnected " + client.getRemoteSocketAddress().toString());
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        }

    }   // END ClientHandler (nested class)

    public class CurrencyUpdater implements Runnable{
        // Responsible for Getting new exchange rates from the Yahoo API, saving to database and
        // broadcasting new rates to connected clients

        public void run(){
            // Get new currency exchange rates every THREAD_SLEEP milliseconds
            while (true){
                try{
                    CurrencyData cd = buildCurrencyData(getCurrentRates());
                    if (!(cd.saveMe())){
                        serverLogModel.addElement("--Could not save current exchange rates");
                        System.out.println("--Could not save current exchange rates");
                    }
                    broadcastData(cd);
                    Thread.sleep(THREAD_SLEEP);     // pause thread for x milliseconds
                }
                catch (InterruptedException ieEx){
                    serverLogModel.addElement("--Problem in pausing the Currency Updater thread");
                    System.out.println("--Problem in pausing the Currency Updater thread");
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }

        public void broadcastData(CurrencyData newData){
            // Broadcast the new currency rates to all connected clients, update current rates so new clients access that
            if (clients.isEmpty()){     // No clients connected so only show on the server
                serverLogModel.addElement("--Did not broadcast (no clients). " + newData.gbpName + ":" + newData.gbpRate + " , " +
                        newData.eurName + ":" + newData.eurRate + " , " + newData.cadName + ":" + newData.cadRate + " , " + newData.kshsName +
                        ":" + newData.kshsRate + " , " + newData.tzsName + ":" + newData.tzsRate + " @ " + newData.time + " " + newData.date);
                System.out.println("--Did not broadcast (no clients). " + newData.gbpName + ":" + newData.gbpRate + " , " +
                newData.eurName + ":" + newData.eurRate + " , " + newData.cadName + ":" + newData.cadRate + " , " + newData.kshsName +
                ":" + newData.kshsRate + " , " + newData.tzsName + ":" + newData.tzsRate + " @ " + newData.time + " " + newData.date);

                return;
            }
            Boolean broadcasted = false;
            // Clients connected, send exchange rates to them
            Iterator it = clients.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry pair = (Map.Entry) it.next();
                Socket sendSock = (Socket) pair.getKey();
                try{
                    ObjectOutputStream out = (ObjectOutputStream) pair.getValue();
                    out.writeObject(newData);       // Output new exchange rates to connected clients
                    broadcasted = true;
                }
                catch (IOException ioEx){
                    serverLogModel.addElement("--Could not output to client " + sendSock.getRemoteSocketAddress().toString());
                    System.out.println("--Could not output to client " + sendSock.getRemoteSocketAddress().toString());
                    it.remove();
                    clientLogModel.removeElement(sendSock.getRemoteSocketAddress().toString());
                }
                catch (Exception ex){
                    // When a client disconnects remove their output stream to avoid errors in broadcasting
                    serverLogModel.addElement("--A client has disconnected. No longer broadcasting to " + sendSock.getRemoteSocketAddress().toString());
                    System.out.println("--A client has disconnected. No longer broadcasting to " + sendSock.getRemoteSocketAddress().toString());
                    it.remove();
                    clientLogModel.removeElement(sendSock.getRemoteSocketAddress().toString());
                }
            }
            if (broadcasted){
                serverLogModel.addElement("--Broadcasted " + newData.gbpName + ":" + newData.gbpRate + " , " +
                        newData.eurName + ":" + newData.eurRate + " , " + newData.cadName + ":" + newData.cadRate + " , " + newData.kshsName +
                        ":" + newData.kshsRate + " , " + newData.tzsName + ":" + newData.tzsRate + " @ " + newData.time + " " + newData.date);
                System.out.println("--Broadcasted " + newData.gbpName + ":" + newData.gbpRate + " , " +
                        newData.eurName + ":" + newData.eurRate + " , " + newData.cadName + ":" + newData.cadRate + " , " + newData.kshsName +
                        ":" + newData.kshsRate + " , " + newData.tzsName + ":" + newData.tzsRate + " @ " + newData.time + " " + newData.date);
            }
        }
    }   // End CurrencyUpdater (nested class)

}   // End CurrencyExchangeServer
