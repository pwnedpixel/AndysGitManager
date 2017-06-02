package code;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by keecha4 on 6/2/2017.
 */
public class CommandThread extends Thread {

    String _command;
    SuccessHandler _handler;

    public synchronized void start(String command, SuccessHandler handler) {
        super.start();
        _handler = handler;
        _command = command;
        System.out.println("MyThread "+command);
    }

    public void run(){
        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(_command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        _handler.onComplete(output.toString());
    }
}
