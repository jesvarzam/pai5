import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

public class Server {
  
    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    private static InputStreamReader inputStreamReader;
    private static BufferedReader bufferedReader;
    private static String message="";
	private static Integer port=7070;

    public static Map<String, String> getDataFromMessage(String message) {
        return Arrays.asList(message.split(";"))
            .stream()
            .map(s -> s.split(":"))
            .collect(Collectors.toMap(e -> e[0], e -> e[1]));
    }

    public static PublicKey convertPublicKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static Boolean checkSign(Map<String, String> data) {
        try {
            PublicKey publicKey = convertPublicKey(data.get("publicKey"));
            byte[] signBytes = data.get("firma").getBytes();
            data.remove("firma");
            data.remove("publicKey");
            byte[] dataBytes = data.toString().getBytes();
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(dataBytes);
            return signature.verify(signBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static void createDatabase() throws ClassNotFoundException {

        String path = "jdbc:sqlite:" + new File("mobifirma.db").getAbsolutePath();
       
        try {  
            Connection conn = DriverManager.getConnection(path);  
            if (conn != null) {
                System.out.println("Connection with database is successful"); 
            }
        } catch (SQLException e) {  
            System.out.println(e.getMessage());  
        }
    }

    public static void createTable() {
        
         String path = "jdbc:sqlite:" + new File("mobifirma.db").getAbsolutePath();  
          
         String sql = "CREATE TABLE IF NOT EXISTS mobifirma (\n"  
                 + " id integer PRIMARY KEY,\n"  
                 + " timestamp timestamp NOT NULL,\n"  
                 + " data text NOT NULL,\n"
                 + " signature boolean NOT NULL\n"
                 + ");";  
           
         try{  
             Connection conn = DriverManager.getConnection(path);  
             Statement stmt = conn.createStatement();  
             stmt.execute(sql);  
         } catch (SQLException e) {  
             System.out.println(e.getMessage());  
         }  
     }

     public static void insertData(Map<String, String> data) {

        String path = "jdbc:sqlite:" + new File("mobifirma.db").getAbsolutePath();
        try {  
            Connection conn = DriverManager.getConnection(path);
            if (conn != null) {
                System.out.println("Connection with database is successful");
                String sql = "INSERT INTO mobifirma(timestamp, data, signature) VALUES (?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, LocalDateTime.now().toString());
                pstmt.setString(2, data.toString());
                pstmt.setBoolean(3, checkSign(data));
                pstmt.executeUpdate();
            }
            
        } catch (SQLException e) {  
            System.out.println(e.getMessage());  
        }  
     }

    public static void main(String[] args) throws InvalidKeySpecException, ClassNotFoundException {
    
        try {
            serverSocket = new ServerSocket(port); 
    
        } catch (IOException e) {
            System.out.println("Could not listen on port: " + port);
        }
    
        System.out.println("Server started. Listening to the port " + port);
    
        while (!message.equalsIgnoreCase("over")) {
            try {
    
                clientSocket = serverSocket.accept(); 
    
                inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
                bufferedReader = new BufferedReader(inputStreamReader);                     
                
                message = bufferedReader.readLine();

                Map<String, String> data = getDataFromMessage(message);
                createDatabase();
                createTable();
                insertData(data);

                inputStreamReader.close();
                clientSocket.close();
                
    
            } catch (IOException ex) {
                System.out.println("Problem in message reading");
            }
        }
    }
}