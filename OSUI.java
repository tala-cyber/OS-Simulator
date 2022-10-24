import java.io.IOException;

public class OSUI{

    public static void main(String args[]){
        Runtime rt = Runtime.getRuntime();
    try {
        rt.exec(new String[]{"cmd.exe","/c","javac OS.java"});
        rt.exec(new String[]{"cmd.exe","/c","START /wait java OS"});
        //rt.exec(new String[]{"cmd.exe","/c","pause"});

    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    } 
}