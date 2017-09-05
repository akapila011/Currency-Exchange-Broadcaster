import jdk.nashorn.internal.scripts.JO;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import javax.swing.JOptionPane;

public class CurrencyExchangeClient {

    public Socket sock;
    public ObjectOutputStream out;
    public ObjectInputStream in;
    protected static String serverIp;
    protected  static int serverPort;

    // GUI elements
    JFrame theFrame;
    JList ratesList;
    DefaultListModel<String> rateDataModel;
    JLabel date;
    JLabel time;
    JPanel dateTimePanel;

    public String selectedExchangeRate = null;
    public static String chosenExchangeRate = null;

    public static void main(String[] args){     // user should provide ip address & port
        serverIp = JOptionPane.showInputDialog(null, "Enter Server IP Address", "127.0.0.1");
        if (!(serverIp.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))){    // Check if invalid ip address given
            // can also match with (25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)
            JOptionPane.showMessageDialog(null, "The IP Address provided in invalid");
            System.exit(1);
        }
        String tempPort = JOptionPane.showInputDialog(null, "Enter Server Port", "9000");
        try{        // Ensure port given is valid
            serverPort = Integer.parseInt(tempPort);
            if ((serverPort < 1024) || (serverPort > 60000)){     // Ensure port given is between 0 and 60000
                JOptionPane.showMessageDialog(null, "The Port number must be between 1024 and 60000");
                System.exit(1);
            }
        }
        catch (Exception ex){
            JOptionPane.showMessageDialog(null, "The Port number provided in invalid");
            System.exit(1);
        }
        // Everything OK start the application
        CurrencyExchangeClient cec = new CurrencyExchangeClient();
        cec.startUp();
    }

    public void startUp(){
        // Start socket connection and provide GUI interface
        try{
            sock = new Socket(serverIp, serverPort);
            out = new ObjectOutputStream(sock.getOutputStream());
            out.writeObject("Update Request");
            in = new ObjectInputStream(sock.getInputStream());

            Thread t = new Thread(new CurrencyReader());
            t.start();
        }
        catch (ConnectException connEx){
            JOptionPane.showMessageDialog(null, "The Server could not be found at " + serverIp + ":" + serverPort);
            System.exit(1);
        }
        catch (SocketException sockEx){
            JOptionPane.showMessageDialog(null, "Connection to the Server has been closed successfully");
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        buildGUI();
    }

    public void buildGUI(){
        // Build the GUI layout for the user to interact with the interface
        theFrame = new JFrame("Currency Exchange");
        theFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        theFrame.setResizable(false);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        Box ratesBox = new Box(BoxLayout.Y_AXIS);

        JLabel ratesHeading = new JLabel("Exchange Rates");
        ratesBox.add(ratesHeading);

        rateDataModel = new DefaultListModel<>();   // Create initial rates data to show
        rateDataModel.addElement("USD/GBP : 0.0000");
        rateDataModel.addElement("USD/EUR : 0.0000");
        rateDataModel.addElement("USD/CAD : 0.0000");
        rateDataModel.addElement("USD/KSHS : 0.0000");
        rateDataModel.addElement("USD/TZS : 0.0000");
        ratesList = new JList(rateDataModel);
        ratesList.addListSelectionListener(new RateSelectedListener());
        ratesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(ratesList);
        theList.setPreferredSize(new Dimension(200, 300));
        ratesBox.add(theList);

        JLabel emptyLabel = new JLabel(" ");
        buttonBox.add(emptyLabel);

        JButton getCurrentRates = new JButton("Update Exchange Rates");
        getCurrentRates.addActionListener(new GetCurrentRatesListener());
        buttonBox.add(getCurrentRates);
        buttonBox.add(Box.createVerticalStrut(20));

        JButton getHistory = new JButton("Get Exchange Rate History");
        getHistory.addActionListener(new RatesHistoryListener());
        buttonBox.add(getHistory);
        buttonBox.add(Box.createVerticalStrut(70));

        JButton exit = new JButton("Exit");
        exit.addActionListener(new ExitListener());
        buttonBox.add(exit);

        GridLayout grid = new GridLayout(2, 2);
        grid.setVgap(2);
        grid.setHgap(2);
        dateTimePanel = new JPanel(grid);
        background.add(BorderLayout.SOUTH, dateTimePanel);

        time = new JLabel("Time (GMT) : ");
        date = new JLabel("Date : ");

        dateTimePanel.add(date);
        dateTimePanel.add(time);

        background.add(BorderLayout.WEST, ratesBox);
        background.add(BorderLayout.EAST, buttonBox);
        theFrame.getContentPane().add(background);
        theFrame.setBounds(50, 50, 500, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    public void updateData(CurrencyData cd) {
        // Updates Exchange rate data and date/time data on the GUI
        rateDataModel.removeAllElements();      // Replace all rates currently shown
        // Add new rates
        rateDataModel.addElement(cd.gbpName + " : " + cd.gbpRate);
        rateDataModel.addElement(cd.eurName + " : " + cd.eurRate);
        rateDataModel.addElement(cd.cadName + " : " + cd.cadRate);
        rateDataModel.addElement(cd.kshsName + " : " + cd.kshsRate);
        rateDataModel.addElement(cd.tzsName + " : " + cd.tzsRate);
        // Update Date and Time
        date.setText("Date : " + cd.date);
        time.setText("Time (GMT) : " + cd.time);
    }

    public class GetCurrentRatesListener implements ActionListener{
        public void actionPerformed(ActionEvent event){
            try{
                JOptionPane.showMessageDialog(theFrame, "Retrieving Current Exchange Rates");
                out.writeObject("Update Request");
            }
            catch (IOException ioEx){
                JOptionPane.showMessageDialog(theFrame, "Could not update rates. Server is busy");
            }
        }
    }

    public class RateSelectedListener implements ListSelectionListener{
        public void valueChanged(ListSelectionEvent event){
            if (!event.getValueIsAdjusting()){  // selected item is not changing
                String selected = (String) ratesList.getSelectedValue();
                if (selected == null){  // nothing selected
                    selectedExchangeRate = null;
                }
                else{   // A single exchange rate selected
                    if (selected.toLowerCase().contains("USD/GBP".toLowerCase())){
                        selectedExchangeRate = "USD/GBP";
                    }
                    else if (selected.toLowerCase().contains("USD/EUR".toLowerCase())){
                        // USD to EUR selected
                        selectedExchangeRate = "USD/EUR";
                    }
                    else if (selected.toLowerCase().contains("USD/CAD".toLowerCase())){
                        // USD to GBP selected
                        selectedExchangeRate = "USD/CAD";
                    }
                    else if (selected.toLowerCase().contains("USD/KSHS".toLowerCase())){
                        // USD to KSHS selected
                        selectedExchangeRate = "USD/KSHS";
                    }
                    else if (selected.toLowerCase().contains("USD/TZS".toLowerCase())){
                        // USD to TZS selected
                        selectedExchangeRate = "USD/TZS";
                    }
                    else{
                        // Resort to default selection
                        selectedExchangeRate = "USD/GBP";
                    }
                }
            }
        }
    }

    public class RatesHistoryListener implements ActionListener{
        public void actionPerformed(ActionEvent event){
            if (selectedExchangeRate == null){      // Ensure a currency exchange is selected
                JOptionPane.showMessageDialog(theFrame, "No Currency Exchange Rate Selected");
            }
            else{
                chosenExchangeRate = selectedExchangeRate;      // chosenExchangeRate only holds the currency pair name being queried
                RateHistoryRequest req = new RateHistoryRequest(chosenExchangeRate);
                JOptionPane.showMessageDialog(theFrame, "You have requested the exchange rate history for " + req.rateName);
                try{
                    out.writeObject(req);
                }
                catch (IOException ioEx){
                    System.out.println("Could not send Exchange Rate History request to the Server ");
                }

            }
        }
    }   // END RatesHistoryListener{}

    public class ExitListener implements ActionListener{
        public void actionPerformed(ActionEvent event){
            try{
                out.close();
                in.close();
                sock.close();
            }
            catch (Exception ex){
                // DO nothing if socket cannot be closed
            }
            finally {   // Ensure window always closes
                theFrame.dispose();
            }
        }
    }

    public class CurrencyReader implements Runnable {

        public Object obj = null;

        public void run() {
            try {
                // Server sends CurrencyData regularly
                while ((obj=in.readObject()) != null) {
                    if (obj.getClass().getName().equals("CurrencyData")){
                        CurrencyData cd = (CurrencyData) obj;
                        updateData(cd); // Update the GUI to reflect new exchange rates received from the server
                    }
                    else if (obj.getClass().getName().equals("RateHistoryResponse")){
                        RateHistoryResponse resp = (RateHistoryResponse) obj;
                        Thread reqRespHandler = new Thread(new ResponseHandler(resp));
                        reqRespHandler.start();
                    }
                }// End while loop
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }	// End run()
    }	// End RemoteReader Class {}

    public class ResponseHandler implements Runnable {

        public RateHistoryResponse response;

        public ResponseHandler(RateHistoryResponse resp){
            response = resp;
        }

        public void run(){
            // Get response in to the Response object
            // Build the GUI with the data
            HistoryWindow histWindow = new HistoryWindow(response.history);
        }
    }

}   // END CurrencyExchangeServer class {}
