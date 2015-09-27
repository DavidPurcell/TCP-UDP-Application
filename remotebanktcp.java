import java.io.*;
import java.net.*;
import java.util.Random;
import java.security.MessageDigest;

/**
* Class that contains the TCP client
*/
public class remotebanktcp {
    public static InetAddress serverAddress;
    public static int port;
    public static String username;
    public static String password;
    public static long money;
    public static Socket socket;
    public static BufferedReader input;
    public static PrintWriter out;
    
    /**
    * Main runner for TCP client
    */
    public static void main(String[] args) throws Exception{
        if(args.length < 6){
            System.out.println("Invalid input. Please follow the format: ");
            System.out.println("remotebank ip address:port \"username\" \"password\" deposit/withdraw amount");
            System.exit(0);
        } else {
            if(args.length == 7 && args[6].equals("-d")){
                ClientDebugger.isEnabled = true;
            } else {
                ClientDebugger.isEnabled = false;
            }
            String[] address = args[1].split(":");
            try {
                serverAddress = InetAddress.getByName(address[0]);
                port = Integer.parseInt(address[1]);
            } catch (Exception ex) {
                System.out.println("Invalid address / port");
                System.exit(0);
            }
            if(!(args[4].equals("deposit") || args[4].equals("withdraw"))){
                System.out.println("Please select either deposit or withdraw");
                System.exit(0);
            }
            try {
            socket = new Socket(serverAddress, port);
            } catch (Exception ex){
                System.out.println("Our servers are busy, try again later! (Or turn the server on)");
                System.exit(0);
            }
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String readInput = null;
            username = args[2].substring(1, args[2].length()-1);
            System.out.println("Welcome " + username + " We here at pennies only bank eagerly await your change." );
            password = args[3].substring(1, args[3].length()-1);
            try {
                money = (int)(Double.parseDouble(args[5])*100);
            } catch (Exception ex) {
                System.out.println("That isn't a valid amount of money.");
                System.exit(0);
            }
            String authId = authorize();
            ClientDebugger.log("Authorized Id: " + authId);
            if(authId != null){
                ClientDebugger.log(args[4]);
                String result = null;
                if(args[4].equals("deposit")){
                    result = deposit(money, authId);
                } else if (args[4].equals("withdraw")){
                    result = withdraw(money, authId);
                } else {
                    System.out.println("I'm not sure what you want me to do here");
                    System.exit(0);
                }
                if(result != null){
                    String[] splitMe = result.split(",");
                    System.out.println("Your "+args[4] + " of " + money + " pennies was a success!");
                    System.out.println("Your balance is now " + splitMe[1]);
                    System.out.println("Have a nice day.  Hail Hydra!");
                }
                ClientDebugger.log(result);
            } else {
                System.out.println("Your transaction isn't authorized");
            }
            out.close();
            input.close();
            socket.close();
        }
    }
    
    public static String deposit(long money, String authId) throws Exception{
        String sendData = ("Deposit,"+money+","+authId+","+username);
        ClientDebugger.log("sending deposit stuffs " + sendData);
        out.println(sendData);
        String data = input.readLine();
        ClientDebugger.log("New Balance: " + data);
        return data;
    }
    
    public static String withdraw(long money, String authId) throws Exception{
        String sendData = ("Withdraw,"+money+","+authId+","+username);
        ClientDebugger.log("sending withdraw stuffs " + sendData);
        out.println(sendData);
        String data = input.readLine();
        ClientDebugger.log("New Balance: " + data);
        return data;
    }
    
    /**
    * Sequence of operations that returns either null or a validated authId
    */
    public static String authorize() throws Exception{
        String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder( 32 );
        for( int i = 0; i < 32; i++ ) 
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        String authId = sb.toString();
        
        String challenge = null;
        String success = null;
        challenge = sendAuthRequest(authId);
        success = sendUsernamePassword(challenge, authId);
        if(success != null && success.equals("Failure")){
            authId = null;
        }
        return authId;
    }
    
    /**
    * Sends the authorization request and returns the challenge
    */
    public static String sendAuthRequest(String authId) throws Exception{
        String sendData = ("Let me in,"+authId);
        ClientDebugger.log("sending initial request: " + sendData);
        out.println(sendData);
        ClientDebugger.log("We sent the thing");
        String data = input.readLine();
        ClientDebugger.log("Challenge: " + data);
        return data;
    }
    
    /**
    * Sends the hashed password returns success or failure depending on success
    */
    public static String sendUsernamePassword(String challenge, String authId) throws Exception{
        ClientDebugger.log("sending username / password");
        String hash = md5Encode(username + password + challenge);
        ClientDebugger.log("hash: " + hash);
        String sendData = ("Verify me," + username + "," + authId + "," + hash);
        out.println(sendData);
        ClientDebugger.log("We sent the thing");
        ClientDebugger.log("Awaiting Challenge Response");
        String success = input.readLine();
        ClientDebugger.log("Authorization: " + success);
        return success;
    }
    
    /**
    * MD5 encodes the given input
    */
    public static String md5Encode(String input) throws Exception{
        MessageDigest md= MessageDigest.getInstance("MD5");
		md.update(input.getBytes());
		return new String(md.digest());
    }
}