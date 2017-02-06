import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Timer;
import com.google.common.io.Files;
import org.yaml.snakeyaml.Yaml;
import com.beimin.eveapi.exception.ApiException;
import com.beimin.eveapi.model.character.Notification;
import com.beimin.eveapi.model.character.NotificationText;
import com.beimin.eveapi.model.eve.Alliance;
import com.beimin.eveapi.parser.ApiAuth;
import com.beimin.eveapi.parser.ApiAuthorization;
import com.beimin.eveapi.parser.character.NotificationTextsParser;
import com.beimin.eveapi.parser.character.NotificationsParser;
import com.beimin.eveapi.parser.corporation.CorpSheetParser;
import com.beimin.eveapi.parser.eve.AllianceListParser;
import com.beimin.eveapi.response.character.NotificationTextsResponse;
import com.beimin.eveapi.response.character.NotificationsResponse;
import com.beimin.eveapi.response.corporation.CorpSheetResponse;
import com.beimin.eveapi.response.eve.AllianceListResponse;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;


public class main {
    static main obj = new main();
    public static void main(String[] args) {

        String BotAuthKey = "xoxb-84817292611-OKm9wrcc9pPZfef1VgnRidLH";
        //int keyId = 5941390;
        //String vCode = "TMUabSeBYnBRswSKvXeTAPK373ajhnCPnBCEdSNpZrhhFSELNnVceTiGhNUF3V54";

        try {
            String filetest = obj.zipDecompress();
            //Timer timer = new Timer();
            ApiReturn();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ApiReturn(/*int keyId, String vCode, String file, String BotAuthKey*/) {

        int keyId;
        String vCode;
        int i = -1;

        LinkedHashMap<Integer, String> keyMap = pickAPI();
        Integer[] keyArray = new Integer[keyMap.size()];
        for(Map.Entry<Integer, String> entry : keyMap.entrySet()) {
            i++;
            keyId = entry.getKey();
            keyArray[i] = keyId;
            //System.out.println(keyId + ", " + vCode);
        }

        //getResponse(keyId, vCode, filetest, BotAuthKey);
    }

    public static LinkedHashMap pickAPI() {
        int key1 = 0;
        String value1 = "";
        LinkedHashMap<Integer, String> apiMap = new LinkedHashMap<Integer, String>();
        InputStream in = main.class.getResourceAsStream("/apisheet.csv");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        int i = 0;
        String line;
        ArrayList<String> stringList = new ArrayList<String>();
        try {
            while((line = br.readLine()) != null) {
                stringList.add(line);
            } for (String x : stringList) {
                String[] parts = x.split(";");
                for (i=0;i<parts.length; i++) {
                    if (parts[i].matches("[0-9]+")) {
                        key1 = Integer.parseInt(parts[i]);
                    } else {
                        //(parts[i].contains(".*[a-zA-Z]+.*"))
                        value1 = parts[i];
                    }
                }
                apiMap.put(key1, value1);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return apiMap;
    }

    public String zipDecompress(){
        File folder = Files.createTempDir();
        byte[] buffer = new byte[1024];
        //System.out.println(folder);
        InputStream in = getClass().getResourceAsStream("/staticdataexport.zip");
        try {
            ZipInputStream zis = new ZipInputStream(in);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null) {
                String fileName = ze.getName();
                File newFile = new File(folder + File.separator + fileName);
                System.out.println("Unzipped to: " +newFile);
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                return newFile.toString();
            }
            zis.closeEntry();
            zis.close();
            System.out.println("Unzipped");
            System.out.println("test");
        } catch (IOException e){
            e.printStackTrace();
        }
        return "";
    }

    public static String sqlConnect(int moonID, String sql, String file) throws IOException {
        //initialize connection
        Connection connection;
        String systemName;
        try {
            //initialize connection to internal resource db in jar
            //System.out.println("jdbc:sqlite:" + file);
            connection = DriverManager.getConnection("jdbc:sqlite:" + file);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(10);

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, moonID);

            ResultSet rs = preparedStatement.executeQuery();

            while(rs.next()) {
                systemName = rs.getString("itemName");
                return systemName;
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        return "";
    }

    public static void getResponse(int keyId, String vCode, String file, String BotAuthKey) throws ApiException {

        ArrayList<Long> notificationIDstore = new ArrayList<Long>();
        long ID;
        int moonIDint;
        int i = 0;
        String moonName = "";
        String aggressorName = "";
        Yaml yaml = new Yaml();
        Date date;

        ApiAuth auth = new ApiAuthorization(keyId, vCode);
        NotificationTextsParser notificationTextsParser = new NotificationTextsParser();
        NotificationsParser notificationsParser = new NotificationsParser();
        NotificationsResponse notificationsResponse = notificationsParser.getResponse(auth);
        Collection<Notification> notifications = notificationsResponse.getAll();
        int size = notifications.size();
        long arrayid[] = new long[size];
        for (Notification notification : notifications) {
            if (notification.getTypeID() == 75 && !notification.isRead()) {
                i++;
                ID = notification.getNotificationID();
                arrayid[i] = ID;
                date = notification.getSentDate();
                NotificationTextsResponse notificationTextsResponse = notificationTextsParser.getResponse(auth, ID);
                Collection<NotificationText> notificationTexts = notificationTextsResponse.getAll();
                for (NotificationText notificationText : notificationTexts) {
                    if (notificationText.getNotificationID() == ID) {
                        long notificationID = 
                        String notificationBody = notificationText.getText();
                        Map notificationmap = (Map) yaml.load(notificationBody);
                        Iterator iterator = notificationmap.keySet().iterator();
                        while(iterator.hasNext()) {
                            String k_ey = iterator.next().toString();
                            if (k_ey.contains("moonID")) {
                                Object moonObj = notificationmap.get(k_ey);
                                moonIDint = Integer.parseInt(moonObj.toString());
                                String sql = "SELECT itemName FROM mapDenormalize WHERE itemID = ?";
                                try{
                                    moonName = sqlConnect(moonIDint, sql, file);
                                } catch (IOException e){
                                    e.printStackTrace();
                                }
                            } if(k_ey.contains("aggressorCorpID")){
                                Object obj2 = notificationmap.get(k_ey);
                                long corpID = Long.parseLong(obj2.toString());
                                CorpSheetParser corpParser = new CorpSheetParser();
                                CorpSheetResponse corpResponse = corpParser.getResponse(corpID);
                                long aggressorAllianceID = corpResponse.getAllianceID();
                                AllianceListParser allianceParser = new AllianceListParser();
                                AllianceListResponse allianceResponse = allianceParser.getResponse(auth);
                                final Collection<Alliance> alliances = allianceResponse.getAll();
                                for (Alliance alliance : alliances){
                                    if(alliance.getAllianceID() == aggressorAllianceID) {
                                        aggressorName = alliance.getName();
                                    }
                                }
                            }
                        }
                        String constructedmessage = moonName + " is under attack by: " + aggressorName + " at: " + date;
                        System.out.println(constructedmessage);
                        /*
                        SlackSession session = SlackSessionFactory.createWebSocketSlackSession(BotAuthKey);
                        String CHANNEL = "random";
                        try {
                            session.connect();
                            SlackChannel channel = session.findChannelByName("random");
                            //session.sendMessage(channel, constructedmessage);
                            System.exit(0);
                        } catch (IOException e){
                            e.printStackTrace();
                        }*/
                    }
                }
            }
        }
    }
}