README:
NAME: David Purcell
EMAIL: dpurcell7@gatech.edu
CLASS NAME: Systems and Networking 1 (CS 3251)
SECTION: B
DATE: 9/26/2015 (1 day late)
ASSIGNMENT TITLE: Programming Assignment 1

File names and descriptions:
bankAccount.java - The class that represents the individual user bank accounts.
ClientDebugger.java - A simple debugger used to enable or disable debug output for client services.
remotebanktcp.java - The TCP client for depositing or withdrawing.
remotebankudp.java - The UDP client for depositing or withdrawing.
ServerDebugger.java - A simple debugger used to enable or disable debug output for server services.
servertcp.java - The server for accepting and processing TCP bank requests.
serverudp.java - The server for accepting and processing UDP bank requests.
README.txt - Describes how to run the banking server / client, and some other stuff.

Compilation Instructions:
Unzip the files into a folder that can access Java.  If you need
help setting up Java, please contact any 1331 professor.  To compile all the files, 
simply type "javac *.java", or compile each file individually.  This will generate the needed
.class files to run the clients, servers, and debuggers.  To run a client or server, 
 from command line just type "java [filename]" along with the command line args.  
 Make sure that the appropriate server is running before  trying to run a client.
 I wrote / tested this on Windows using java 1.8._025, but it *should* run on anything with Java.

Command line args:
Client: remotebank [IP Address]:[Port Number] “[username]” “[password]” [deposit/withdraw] [money]
Server: bank-server [Port Number]
Add -d to either command to enable debug output.

Protocol description:
NOTE: "" indicates a raw string value, [] indicates some variable
Client commands:
AuthRequest: "Let me in",[authId]
    -Used to initiate a transaction
    -Contains the client's authId, which will be used to keep track of this transaction.
    -authId can be any string, but should try to be unique.  I recommend 32 random alphanumeric characters.
    -Should be responded to by "Challenge".
VerifyRequest: "Verify Me",[username],[authId],[hash]
    -Used to get verification that this username / password can access the account.
    -username is the client's username.
    -authId is the authId created for AuthRequest.
    -hash is the MD5 hash of the username+password+challenge recieved from server.
    -Should be responded by "AuthResponse".
Deposit: "Deposit",[money],[authId]
    -Used to add the specified amount of money to user's account
    -money is a long that represends the number of pennies to add
    -authId is the authId created for AuthRequest.
    -Should be responded by "New Balance".
Withdraw: "Withdraw",[money],[authId]
    -Used to subtract the specified amount of money to user's account
    -money is a long that represends the number of pennies to remove
    -authId is the authId created for AuthRequest.
    -Should be responded by "New Balance".
   
Server commands:
Challenge: [challenge]
    -Sent in response to "AuthRequest"
    -Challenge is generated from alphanumeric characters
    -Used to encode the username and password for comparison purposes
    -Will need to be accessed later.
AuthResponse: "Success" OR "Failure"
    -Sent in response to "VerifyRequest"
    -"Success" if the calculated MD5 hash matches the one sent.
    -"Failure" if the calculated MD5 hash doesn't match the one sent.
New Balance: "New balance",money
    -Sent in response to "Withdraw" or "Deposit".
    -money is the amount of money remaining in the user's account
    -Can also send "Failure" in place of money if something went wrong.
    -Negative money is allowed.  Who doesn't love debt.

Known bugs:
-You can have negative money (Feature?)
-If TCP fails to connect, doesn't retry automatically.