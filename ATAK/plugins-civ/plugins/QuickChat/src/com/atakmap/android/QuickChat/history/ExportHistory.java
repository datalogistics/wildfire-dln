
package com.atakmap.android.QuickChat.history;

import android.widget.Toast;

import com.atakmap.android.QuickChat.utils.FileUtils;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by Scott on 8/11/2016.
 * Handles the 3 layer tier of exporting current chat messsage popup history
 * the file can be created for .json .xml, .csv
 * saved in atak/tools/chatmessagepopups/export/ ?
 * files are given timestamps from beginning date to ending date with proper file extensions
 * @author Scott Auman
 */
public class ExportHistory {

    private final String TAG = getClass().getSimpleName();
    private final String DIRECTORY = FileSystemUtils.ATAK_ROOT_DIRECTORY
            + File.separator +
            FileSystemUtils.TOOL_DATA_DIRECTORY + File.separator
            + "QuickChat" +
            File.separator + "export" + File.separator;
    private final MapView mapView;
    private String FILENAME = "quick_chat_history";
    private String EXTENSION;

    public ExportHistory() {
        this.mapView = MapView.getMapView();
    }

    public void buildFile(int which) {
        switch (which) {
            case 0: //xml
                EXTENSION = ".xml";
                buildXmlFile();
                break;
            case 1: //json
                EXTENSION = ".json";
                buildJsonFile();
                break;
            case 2: //csv
                EXTENSION = ".csv";
                buildCsvFile();
                break;
            default:
                break;
        }
    }

    private void buildCsvFile() {

        FILENAME = attachFileTimeStamp(FILENAME);
        String csvString = createCSVString();
        if(csvString == null){
            showErrorToast();
            return;
        }
        writeFile(csvString);
    }

    private String createCSVString() {

        char comma = ',';
        StringBuilder sb = new StringBuilder();
        List<Message> allMessages = SavedMessageHistory.
                getAllMessagesInHistory(MapView.getMapView().getContext(), true);

        if (allMessages == null) {
            Log.d(TAG, "allMessages LIST null");
            showErrorToast();
            return null;
        }
        sb.append("Type").append(comma).append("Date").append(comma).append("Time").append(comma)
                .append("Callsign")
                .append(comma).append("Message").append(comma).append("\n"); //header row!

        //loop through each message object attach to a single row
        for (Message message : allMessages) {
            sb.append(message.getType().name()).append(comma).append(message.getDate()).append(comma)
                    .append(message.getTime())
                    .append(comma).append((message.getFrom().equals("") ? "To: " + message.getTo() : "From: " + message.getFrom())).append(comma)
                    .append(message.getMessage()).append(comma).append("\n");
        }

        return sb.toString();
    }

    /**
     * creates a document class file that
     * loops through each message object creating a child xml node for
     * each sub type "message" node
     * EXAMPLE:
     * <?xml version="1.0" encoding="UTF-8"?><messages>
     <message>
     <date>08/09/2016</date>
     <time>22:34 PM</time>
     <callsign>Callsign: auman_s5</callsign>
     <message>Roger</message>
     </message>
     <message>
     <date>08/09/2016</date>
     <time>22:34 PM</time>
     <callsign>Callsign: auman_s5</callsign>
     <message>at LCC</message>
     </message>
     <message>
     <date>08/09/2016</date>
     <time>22:34 PM</time>
     <callsign>Callsign: auman_s5</callsign>
     <message>at VDO</message>
     </message>
     <message>
     <date>08/09/2016</date>
     <time>22:34 PM</time>
     <callsign>Callsign: auman_s5</callsign>
     <message>at breach</message>
     </message>
     </messages>
     */
    private void buildXmlFile() {
        FILENAME = attachFileTimeStamp(FILENAME);
        String xmlString = createXmlDocument();
        if (xmlString == null) {
            Log.d(TAG, "xmlString is null");
            showErrorToast();
            return;
        }
        writeFile(xmlString);
    }

    private void showErrorToast(){
        Toast.makeText(MapView.getMapView().getContext(),"Error Creating Histroy File",Toast.LENGTH_SHORT).show();
    }

    private boolean doubleCheck() {
        HashMap<String, List<Message>> messageMap =
                new MessageGrouper(SavedMessageHistory.getAllMessagesInHistory
                        (mapView.getContext(), true)).getMessageMap();
        if (messageMap == null) {
            Log.d(TAG, "message dates are null");
            return false;
        }

        if(messageMap.size() == 0){
            return false;
        }
        return true;
    }

    /**
     * easier conversion to do the original file objects were created into json objects
     * just return the string version to writeFile();
     */
    private void buildJsonFile() {

        String jsonString = getMessagesJson();
        if(jsonString == null){
            showErrorToast();
            return;
        }
        FILENAME = attachFileTimeStamp(FILENAME);
        writeFile(jsonString);
    }


    private String getMessagesJson() {
        String jsonString = SavedMessageHistory.getAllMessagesInHistory(mapView
                .getContext());
        if (jsonString == null || jsonString.equals(SavedMessageHistory.getDefault()) ||
                jsonString.equals(" ")) {
            Log.d(TAG, "jsonString is null or empty");
            return null;
        }
        return jsonString;
    }

    private void writeFile(String jsonString) {
        boolean result = new FileUtils(mapView.getContext()).write(DIRECTORY,
                FILENAME, jsonString);
        if (!result) {
            Log.d(TAG, "Problem Writing File See logs...");
        }
    }

    /**Creates the ending unique time stamp that stamps the beginning dates of the current exporting history
     * and ending date as well as a unique int value to separate from matching
     * date stamps
     */
    private String attachFileTimeStamp(String string) {
        //attach date stamp for first to last message in saved history
        HashMap<String, List<Message>> messageMap =
                new MessageGrouper(SavedMessageHistory.getAllMessagesInHistory
                        (mapView.getContext(), true)).getMessageMap();
        if (messageMap == null) {
            Log.d(TAG, "message dates are null");
        }

        if(messageMap.size() == 0){
            return null;
        }

        List<String> dates = new ArrayList<String>(messageMap.keySet());

        string += dates.get(dates.size() - 1).replaceAll("/", ".") + "_";
        string += dates.get(0).replaceAll("/", ".") + "_";
        string += String.valueOf(new SecureRandom().nextInt(5000));
        string += EXTENSION;

        return string;
    }

    /** Parses and adds unique attributes for XML file
     * this includes spacing, header and properly executes the
     * document class file to a string that we can send into FileUtils()
     */
    private String domToString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer
                    .setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    private String createXmlDocument() {

        List<Message> allMessages = SavedMessageHistory.
                getAllMessagesInHistory(MapView.getMapView().getContext(), true);

        if (allMessages == null) {
            Log.d(TAG, "allMessages LIST null");
            return null;
        }

        try {

            DocumentBuilderFactory dbFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder =
                    dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            // root element
            Element rootElement = doc.createElement("messages");
            doc.appendChild(rootElement);

            for (Message message : allMessages) {

                Element messageElement = doc.createElement("message");
                rootElement.appendChild(messageElement);

                Element type = doc.createElement("type");
                type.appendChild(
                        doc.createTextNode(message.getType().name()));
                messageElement.appendChild(type);

                Element date = doc.createElement("date");
                date.appendChild(
                        doc.createTextNode(message.getDate()));
                messageElement.appendChild(date);

                Element time = doc.createElement("time");
                time.appendChild(
                        doc.createTextNode(message.getTime()));
                messageElement.appendChild(time);

                Element callsign = doc.createElement("callsign");
                callsign.appendChild(
                        doc.createTextNode((message.getFrom().equals("") ? "To: " + message.getTo() :
                                "From: " + message.getFrom())));
                messageElement.appendChild(callsign);

                Element text = doc.createElement("message");
                text.appendChild(
                        doc.createTextNode(message.getMessage()));
                messageElement.appendChild(text);
            }

            return domToString(doc);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
