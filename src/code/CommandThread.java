package code;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Created by keecha4 on 6/2/2017.
 */
public class CommandThread extends Thread {

    String _command;
    SuccessHandler _handler;

    public CommandThread(String command, SuccessHandler handler){
        _handler = handler;
        _command = command;
    }

    public void run(){
        System.out.println(Thread.currentThread()+" MyThread "+_command);
        StringBuffer output = new StringBuffer();

        //Process p;
        try {
            ProcessBuilder builder  = new ProcessBuilder("cmd.exe ","/c", _command);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            p.waitFor(2, TimeUnit.SECONDS);

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";

            while ((line = reader.readLine())!= null) {

                output.append(line + "\n");
            }
            System.out.println(output);
        } catch (Exception e) {
            e.printStackTrace();
        }

        _handler.onComplete(output.toString());
    }
}
