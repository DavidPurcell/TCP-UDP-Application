/**
* A REALLY basic server debugger.
*/
public class ServerDebugger{
    public static boolean isEnabled = true;

    public static void log(Object o){
        if(isEnabled){
            System.out.println(o.toString());
        }
    }
}