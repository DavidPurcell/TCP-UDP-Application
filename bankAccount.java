//ALL VALUES STORED IN PENNIES!!!!!!!

public class bankAccount {
    private String username;
    private String password;
    private long balance;
    
    public bankAccount(String username, String password, long balance){
        this.username = username;
        this.password = password;
        this.balance = balance;
    }
    
    public String getUsername(){
        return username;
    }
    
    public String getPassword(){
        return password;
    }
    
    //Remove specified amount of money, return new balance.
    public long withdraw(long withdrawl){
        return this.balance -= withdrawl;
    }
    
    //Add specified amount of money, return new balance.
    public long deposit(long amount){
        return this.balance += amount;
    }
    
    public long getBalance(){
        return this.balance;
    }
}