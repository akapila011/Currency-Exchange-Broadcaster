import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Abhz on 11/19/2016.
 */
public class HistoryWindow {

    public JFrame histFrame;
    public JLabel title;
    public JList historyRatesList;
    DefaultListModel<String> rateHistoryDataModel;
    public boolean saveable = false;
    ArrayList<String[]> historyData;

    public HistoryWindow(ArrayList<String[]> histData){
        historyData = histData;
        histFrame = new JFrame("Currency Exchange");
        histFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        histFrame.setResizable(false);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Box contentBox = new Box(BoxLayout.Y_AXIS);

        title = new JLabel("Exchange Rate History for " + CurrencyExchangeClient.chosenExchangeRate);
        contentBox.add(title);

        rateHistoryDataModel = new DefaultListModel<>();   // Enter initial rates history data to show
        if (histData.size() == 0){   // No History Data
            rateHistoryDataModel.addElement("No Exchange Rate History is Available for " + CurrencyExchangeClient.chosenExchangeRate);
        }
        else{       // Exchange rate history is available
            for (int i = 0; i != histData.size(); i++){
                rateHistoryDataModel.addElement(histData.get(i)[0] + " : " + histData.get(i)[1] + "  -  " + histData.get(i)[2] + " " + histData.get(i)[3]);
            }
            saveable = true;
        }
        historyRatesList = new JList(rateHistoryDataModel);
        historyRatesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(historyRatesList);
        theList.setPreferredSize(new Dimension(200, 350));
        contentBox.add(theList);

        JButton save = new JButton("Save");
        save.setEnabled(saveable);
        save.addActionListener(new SaveListener());
        contentBox.add(save);

        background.add(BorderLayout.NORTH, contentBox);
        histFrame.getContentPane().add(background);
        histFrame.setBounds(300, 300, 250, 400);
        histFrame.pack();
        histFrame.setVisible(true);
    }

    public class SaveListener implements ActionListener{
        public void actionPerformed(ActionEvent event){
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = chooser.showSaveDialog(histFrame);
            if (result ==  chooser.APPROVE_OPTION){
                if (chooser.getSelectedFile().getAbsolutePath().contains(".csv")){
                    BufferedWriter writer = null;
                    try{
                        writer = new BufferedWriter(new FileWriter(chooser.getSelectedFile().getAbsolutePath()));
                        for (int i = 0; i != historyData.size(); i++){
                            writer.write(historyData.get(i)[0] + "," + historyData.get(i)[1] + "," + historyData.get(i)[2] + "," + historyData.get(i)[3] + "\n");
                        }
                        JOptionPane.showMessageDialog(histFrame, "Successfully saved file");
                    }
                    catch (IOException ioEx){
                        JOptionPane.showMessageDialog(histFrame, "Unable to save file");
                    }
                    finally{
                        try{
                            if (writer != null){
                                writer.close();
                            }
                        }
                        catch (IOException ioEx){

                        }
                    }
                }
                else{
                    JOptionPane.showMessageDialog(histFrame, "Could not save: file name must have a csv extension");
                }
            }
        }
    }
}
