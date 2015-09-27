import java.io.*;
import java.net.*;
import java.util.Random;
import java.security.MessageDigest;

public class remotebankudp {
    public static InetAddress serverAddress;
    public static int port;
    public static String username;
    public static String password;
    public static long money;
    public static DatagramSocket clientSocket;

    /**
    * Main runner for UDP client
    */
    public static void main(String[] args) throws Exception{
        clientSocket = new DatagramSocket();
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
                    while(result == null){
                        result = deposit(money, authId);
                    }
                } else if (args[4].equals("withdraw")){
                    while(result == null){
                        result = withdraw(money, authId);
                    }
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
        }
    }
    
    /**
    * Sends the packet for a deposit and returns the new balance
    */
    public static String deposit(long money, String authId) throws Exception{
        byte[] sendData = ("Deposit,"+money+","+authId+","+username).getBytes();
        ClientDebugger.log("sending deposit stuffs " + sendData);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
        clientSocket.send(sendPacket);
        String result = getDepositResult();
        return result;
    }
    
    /**
    * Gets the response containing the new balance following a deposit
    */
    public static String getDepositResult() throws Exception{
        byte[] bytes = new byte[1024];
        DatagramPacket recieverPacket = new DatagramPacket(new byte[1024], bytes.length);
        String data = null;
        try{
            clientSocket.setSoTimeout(2000);
            clientSocket.receive(recieverPacket);
            data = (new String(recieverPacket.getData())).substring(0,recieverPacket.getLength());
        } catch (Exception ex){
            ClientDebugger.log("We timed out :(");
        }
        if(data!= null){
            String[] parse = data.split(",");
            ClientDebugger.log(data);
            if(!parse[0].equals("New balance")){
                data = null;
            }
        }
        ClientDebugger.log("Deposit response: " + data);
        return data;
    }
    /**
    * Sends the packet for a withdraw and returns the new balance
    */
    public static String withdraw(long money, String authId) throws Exception{
        byte[] sendData = ("Withdraw,"+money+","+authId+","+username).getBytes();
        ClientDebugger.log("sending withdraw stuffs " + sendData);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
        clientSocket.send(sendPacket);
        String result = getWithdrawResult();
        return result;
    }
    
    /**
    * Gets the response containing the new balance following a withdraw
    */
    public static String getWithdrawResult() throws Exception{
        byte[] bytes = new byte[1024];
        DatagramPacket recieverPacket = new DatagramPacket(new byte[1024], bytes.length);
        String data = null;
        try{
            clientSocket.setSoTimeout(2000);
            clientSocket.receive(recieverPacket);
            data = (new String(recieverPacket.getData())).substring(0,recieverPacket.getLength());
        } catch (Exception ex){
            ClientDebugger.log("We timed out :(");
        }
        if(data!= null){
            String[] parse = data.split(",");
            ClientDebugger.log(data);
            if(!parse[0].equals("New balance")){
                data = null;
            }
        }
        ClientDebugger.log("Withdraw response: " + data);
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
        while(challenge == null){
            challenge = sendAuthRequest(authId);
        }
        while(success == null){
            success = sendUsernamePassword(challenge, authId);
        }
        if(success != null && success.equals("Failure")){
            authId = null;
        }
        return authId;
    }
    
    /**
    * Sends the authorization request and returns the challenge
    */
    public static String sendAuthRequest(String authId) throws Exception{
        byte[] sendData = ("Let me in,"+authId).getBytes();
        ClientDebugger.log("sending initial request: " + sendData);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
        clientSocket.send(sendPacket);
        ClientDebugger.log("We sent the thing");
        return getChallengeResponse();
    }
    
    /**
    * Accepts and parses the packet for the challenge
    */
    public static String getChallengeResponse() throws Exception{
        ClientDebugger.log("Awaiting Challenge Response");
        byte[] bytes = new byte[1024];
        DatagramPacket recieverPacket = new DatagramPacket(new byte[1024], bytes.length);
        String data = null;
        try{
            clientSocket.setSoTimeout(2);
            clientSocket.receive(recieverPacket);
            data = (new String(recieverPacket.getData())).substring(0,recieverPacket.getLength());
        } catch (Exception ex){
            ClientDebugger.log("We timed out :(");
        }
        ClientDebugger.log("Challenge: " + data);
        return data;
    }
    
    /**
    * Sends the hashed password returns success or failure depending on success
    */
    public static String sendUsernamePassword(String challenge, String authId) throws Exception{
        String hash = md5Encode(username + password + challenge);
        ClientDebugger.log("hashed: " + hash);
        byte[] sendData = ("Verify me," + username + "," + authId + "," + hash).getBytes();
        ClientDebugger.log("sending data: " + sendData);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
        clientSocket.send(sendPacket);
        return getAuthSuccess();
    }
    
    /**
    * Accepts and parses the packet for authorization success or failure
    */
    public static String getAuthSuccess() throws Exception{      
        byte[] bytes = new byte[1024];
        DatagramPacket recieverPacket = new DatagramPacket(new byte[1024], bytes.length);
        try{
            clientSocket.setSoTimeout(2000);
            clientSocket.receive(recieverPacket);
        } catch (Exception ex){
            ClientDebugger.log("We timed out :(");
        }
        String data = (new String(recieverPacket.getData())).substring(0,recieverPacket.getLength());
        if((data.equals("Success") || data.equals("Failure"))){
            ClientDebugger.log("Success or failure");
        } else {
            data = null;
        }
        ClientDebugger.log("Authorization result: " + data);
        return data;
    }
    
    /**
    * MD5 encodes the given input
    */
    public static String md5Encode(String input) throws Exception{
        MessageDigest md= MessageDigest.getInstance("MD5");
		md.update(input.getBytes("UTF-8"));
		return new String(md.digest());
    }
}