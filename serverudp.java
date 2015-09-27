import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;
import java.util.Arrays;
import java.security.MessageDigest;

/**
* UDP banking server class
*/
public class serverudp{
    private static char[] VALID_CHARACTERS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();
    private static HashMap<String, bankAccount> accounts = new HashMap<String, bankAccount>();
    private static HashMap<String, String> authIdToChallenge = new HashMap<String, String>();
    private static HashMap<String, String> authIdIsAuthorized = new HashMap<String, String>();
    
    /**
    * Main runner for the UDP banking server
    */
    public static void main(String[] args) throws Exception{
        if(args.length < 2){
            System.out.println("Invalid input. Please follow the format: ");
            System.out.println("bank-server portNumber");
            System.exit(0);
        }
        if(args.length == 3 && args[2].equals("-d")){
            ServerDebugger.isEnabled = true;
        } else {
            ServerDebugger.isEnabled = false;
        }
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(Integer.parseInt(args[1]));
        } catch (Exception ex) {
            System.out.println("Invalid port number");
            System.exit(0);
        }
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        bankAccount account1 = new bankAccount("DrEvil", "minime123", 100);
        bankAccount account2 = new bankAccount("Username", "Password", 100);
        bankAccount account3 = new bankAccount("MONEY", "MONEY", 100);
        accounts.put(account1.getUsername(), account1);
        accounts.put(account2.getUsername(), account2);
        accounts.put(account3.getUsername(), account3);
        
        while(true){
            DatagramPacket recieverPacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(recieverPacket);
            String data = (new String(recieverPacket.getData())).substring(0,recieverPacket.getLength());
            String opCode = data.split(",")[0];
            ServerDebugger.log("Received: " + data);
            if(opCode.equals("Let me in")){
                sendData = generateChallenge(data);
            } else if(opCode.equals("Verify me")){
                byte[] md5 = Arrays.copyOfRange(recieverPacket.getData(), recieverPacket.getLength()-16, recieverPacket.getLength());
                ServerDebugger.log(md5.length);
                sendData = userAuth(data, md5);
            } else if(opCode.equals("Deposit")){
                sendData = depositMoney(data);
            } else if(opCode.equals("Withdraw")){
                sendData = withdrawMoney(data);
            } else {
                sendData = "What?".getBytes();
            }
            InetAddress IPAddress = recieverPacket.getAddress();
            int responsePort = recieverPacket.getPort();
            String capitalizedSentence = data.toUpperCase();
            DatagramPacket sendPacket = 
            new DatagramPacket(sendData, sendData.length, IPAddress, responsePort);
            data = "";
            Random breakShit = new Random();
            serverSocket.send(sendPacket);
        }
    }
    
    /**
    * Deposits a set amount of money in the authorized account and sends the new balance
    */
    public static byte[] depositMoney(String data){
        String[] str = data.split(",");
        byte[] sendData = new byte[1024];
        bankAccount accountToEdit = accounts.get(str[3]);
        for(String s:authIdIsAuthorized.keySet()){
            ServerDebugger.log(s);
        }
        if(accountToEdit != null && authIdIsAuthorized.get(str[2]).equals("authorized, no")){
            ServerDebugger.log("Depositing " + str[1] + " Cents");
            long depositAmmount = Long.parseLong(str[1]);
            long balance = accountToEdit.deposit(depositAmmount);
            authIdIsAuthorized.put(str[2], "authorized, deposit");
            ServerDebugger.log("All done, sending new balance: " + balance);
            sendData = ("New balance, " + balance).getBytes();
        } else if(authIdIsAuthorized.get(str[2]).equals("authorized, deposit")){
            ServerDebugger.log("All done, sending new balance: " + accountToEdit.getBalance());
            sendData = ("New balance, " + accountToEdit.getBalance()).getBytes();
        } else{
            sendData = "New balance, Failure".getBytes();
        }
        return sendData;
    }
    
    /**
    * Withdraws a set amount of money in the authorized account and sends the new balance
    */
    public static byte[] withdrawMoney(String data){
        String[] str = data.split(",");
        byte[] sendData = new byte[1024];
        bankAccount accountToEdit = accounts.get(str[3]);
        if(accountToEdit != null && authIdIsAuthorized.get(str[2]).equals("authorized, no")){
            ServerDebugger.log("Depositing " + str[1] + " Cents");
            long withdrawAmount = Long.parseLong(str[1]);
            long balance = accountToEdit.withdraw(withdrawAmount);
            authIdIsAuthorized.put(str[2], "authorized, withdraw");
            ServerDebugger.log("All done, sending new balance: " + balance);
            sendData = ("New balance, " + balance).getBytes();
        } else if(authIdIsAuthorized.get(str[2]).equals("authorized, withdraw")){
            ServerDebugger.log("All done, sending new balance: " + accountToEdit.getBalance());
            sendData = ("New balance, " + accountToEdit.getBalance()).getBytes();
        } else{
            sendData = "New balance, ".getBytes();
        }
        return sendData;
    }
    
    /**
    * Checks if the hash sent matches against the backend calculated one
    * Returns Success or Failure depending on result
    */
    public static byte[] userAuth(String data, byte[] bytesToCompare) throws Exception{
        ServerDebugger.log("We are doing user auth");
        String[] str = data.split(",",4);
        ServerDebugger.log("authId: " + str[2]);
        String challenge = authIdToChallenge.get(str[2]);
        bankAccount maybeAccount = accounts.get(str[1]);
        boolean verified = false;
        if(maybeAccount != null){
            String userPassChallenge = maybeAccount.getUsername()+maybeAccount.getPassword()+challenge;
            verified = md5Compare(bytesToCompare, userPassChallenge);
            ServerDebugger.log("verification result: " + verified);
        }
        if(verified){
            ServerDebugger.log("Authorizing " + str[2] + " for transaction");
            authIdIsAuthorized.put(str[2], "authorized, no");
        } else {
            ServerDebugger.log("Not authorizing " + str[2] + " for transaction");
           authIdIsAuthorized.put(str[2], "not authorized");
        }
        return (verified ? "Success" : "Failure").getBytes();
    }
    
    /**
    * Generates the challenge for a authorization request
    */
    public static byte[] generateChallenge(String data){
        String[] str = data.split(",");
        String challenge = "";
        if(!authIdToChallenge.containsKey(str[1])){
            ServerDebugger.log("Generating challenge");
            challenge = challenge();
            authIdToChallenge.put(str[1], challenge);
        } else {
            challenge = authIdToChallenge.get(str[1]);
            ServerDebugger.log("Challenge already exists.  Here you go again!");
        }
        return challenge.getBytes();
    }
    
    /**
    * helper method for making 64 random alphanumeric characters
    */
    public static String challenge(){
        String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder( 64 );
        for( int i = 0; i < 64; i++ ) 
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }
    
    /**
    * helper method for comparing a encoded byte array to an un-encoded string
    */
    public static boolean md5Compare(byte[] encoded, String compareTo) throws Exception{
        ServerDebugger.log(compareTo);
        MessageDigest md= MessageDigest.getInstance("MD5");
		md.update(compareTo.getBytes("UTF-8"));
        String temp1 = new String(md.digest()).replace("\r","\n");
        byte[] temp = temp1.getBytes();
        boolean match = true;
        for(int i=0; i<temp.length; i++){
            if(temp[i] != encoded[i]){
                match = false;
            }
        }
        ServerDebugger.log("Match: " + match);
		return match;
    }
} 