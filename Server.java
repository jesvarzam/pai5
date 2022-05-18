import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;


public class Server {
  
    // declaring required variables
    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    private static InputStreamReader inputStreamReader;
    private static BufferedReader bufferedReader;
    private static String message="";
	private static Integer port=7070;


    public static void main(String[] args) throws InvalidKeySpecException {
    
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

                Map<String, String> map =  Arrays.asList(message.split(";")).stream().map(s -> s.split(":")).collect(Collectors.toMap(e -> e[0], e -> e[1]));                
                byte[] publicBytes = Base64.getDecoder().decode(map.get("publicKey"));
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(keySpec);

                System.out.println(publicKey);

                inputStreamReader.close();
                clientSocket.close();
                
    
            } catch (IOException ex) {
                System.out.println("Problem in message reading");
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            }
        }
    }
}