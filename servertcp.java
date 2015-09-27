import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;
import java.util.Arrays;
import java.security.MessageDigest;

/**
* TCP banking server class
*/
public class servertcp {
    public static BufferedReader input;
    public static PrintWriter pw;
    private static HashMap<String, bankAccount> accounts = new HashMap<String, bankAccount>();
    private static HashMap<String, String> authIdToChallenge = new HashMap<String, String>();
    private static HashMap<String, String> authIdIsAuthorized = new HashMap<String, String>();
    
    /**
    * Main runner for the TCP banking server
    */
    public static void main(String[] args) throws Exception {
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
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(Integer.parseInt(args[1]));
        } catch (Exception ex) {
            System.out.println("Invalid port number");
            System.exit(0);
        }
        
        bankAccount account1 = new bankAccount("DrEvil", "minime123", 100);
        bankAccount account2 = new bankAccount("Username", "Password", 100);
        bankAccount account3 = new bankAccount("MONEY", "MONEY", 100);
        accounts.put(account1.getUsername(), account1);
        accounts.put(account2.getUsername(), account2);
        accounts.put(account3.getUsername(), account3);
        while (true) {
            Socket socket = listener.accept();
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String readInput = "";
                while ((readInput = input.readLine()) != null) {
                    ServerDebugger.log("Received Data: " + readInput);
                    
                    String opCode = readInput.split(",")[0];
                    ServerDebugger.log(opCode);
                    String sendData = "";
                 
                    if(opCode.equals("Let me in")){
                        ServerDebugger.log("Send Challenge");
                        String newChallenge = challenge();
                        authIdToChallenge.put(readInput.split(",")[1], newChallenge);
                        ServerDebugger.log(readInput.split(",")[1]);
                        out.println(newChallenge);
                    } else if(opCode.equals("Verify me")){
                        ServerDebugger.log("Verify stuff");
                        String md5 = readInput.split(",",4)[3];
                        ServerDebugger.log("md5: " + md5 + " length " + md5.length());
                        String readerIn;
                        while(md5.length() < 16 && (readerIn = input.readLine()) != null){
                            ServerDebugger.log("md5: " + md5 + " length " + md5.length());
                            ServerDebugger.log("readerIn: " + readerIn + " length " + readerIn.length());
                            md5 = md5 + "\n" + readerIn;
                        }
                        ServerDebugger.log("MD5 from client: " + md5);
                        bankAccount maybeAccount = accounts.get(readInput.split(",")[1]);
                        String challenge = authIdToChallenge.get(readInput.split(",")[2]);
                        String userPassChallenge = maybeAccount.getUsername()+maybeAccount.getPassword()+challenge;
                        boolean verify = md5Compare(md5.getBytes(), userPassChallenge);
                        authIdIsAuthorized.put(readInput.split(",")[2], "authorized, no");
                        out.println(verify ? "Success" : "Failure");
                    } else if(opCode.equals("Deposit")){
                        ServerDebugger.log("Deposit Things");
                        String newBalance = depositMoney(readInput);
                        out.println(newBalance);
                    } else if(opCode.equals("Withdraw")){
                        ServerDebugger.log("Withdraw Things");
                        String newBalance = withdrawMoney(readInput);
                        out.println(newBalance);
                    } else {
                        //sendData = "What?".getBytes();
                    }
                }
            } finally {
                socket.close();
            }
        }
    }
    
    public static String depositMoney(String data){
        String[] str = data.split(",");
        bankAccount accountToEdit = accounts.get(str[3]);
        for(String s:authIdIsAuthorized.keySet()){
            ServerDebugger.log(s);
        }
        String sendData;
        if(accountToEdit != null && authIdIsAuthorized.get(str[2]).equals("authorized, no")){
            ServerDebugger.log("Depositing " + str[1] + " Cents");
            long depositAmmount = Long.parseLong(str[1]);
            long balance = accountToEdit.deposit(depositAmmount);
            authIdIsAuthorized.put(str[2], "authorized, deposit");
            ServerDebugger.log("All done, sending new balance: " + balance);
            sendData = ("New balance, " + balance);
        } else{
            sendData = "New balance, Failure";
        }
        return sendData;
    }
    
    public static String withdrawMoney(String data){
        String[] str = data.split(",");
        bankAccount accountToEdit = accounts.get(str[3]);
        for(String s:authIdIsAuthorized.keySet()){
            ServerDebugger.log(s);
        }
        String sendData;
        if(accountToEdit != null && authIdIsAuthorized.get(str[2]).equals("authorized, no")){
            ServerDebugger.log("Withdrawing " + str[1] + " Cents");
            long withdrawAmmount = Long.parseLong(str[1]);
            long balance = accountToEdit.withdraw(withdrawAmmount);
            authIdIsAuthorized.put(str[2], "authorized, withdraw");
            ServerDebugger.log("All done, sending new balance: " + balance);
            sendData = ("New balance, " + balance);
        } else{
            sendData = "New balance, Failure";
        }
        return sendData;
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