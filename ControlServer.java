/**
 * This program runs as a server and controls the force to be applied to balance the Inverted Pendulum system running on the clients.
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class ControlServer {

    private static ServerSocket serverSocket;
    private static final int port = 25533;

    /**
     * Main method that creates new socket and PoleServer instance and runs it.
     */
    public static void main(String[] args) throws IOException {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            System.out.println("Unable to set up port:" + ioe);
            System.exit(1);
        }
        System.out.println("Waiting for connection");
        do {
            Socket client = serverSocket.accept();
            System.out.println("\nnew client accepted.\n");
            PoleServer_handler handler = new PoleServer_handler(client);
        } while (true);
    }
}

/**
 * This class sends control messages to balance the pendulum on client side.
 */
class PoleServer_handler implements Runnable {
    // Set the number of poles
    private static final int NUM_POLES = 2;

    static ServerSocket providerSocket;
    Socket connection = null;
    ObjectOutputStream out;
    ObjectInputStream in;
    String message = "abc";
    static Socket clientSocket;
    Thread t;

    /**
     * Class Constructor
     */
    public PoleServer_handler(Socket socket) {
        t = new Thread(this);
        clientSocket = socket;

        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        t.start();
    }
    double angle, angleDot, pos, posDot, action = 0, i = 0;

    //Data used for part three
    int current_pole = 0;
    double last_angle = 0;
    double last_angleDot = 0;
    double last_pos = 0;
    double last_posDot = 0;

    /**
     * This method receives the pole positions and calculates the updated value
     * and sends them across to the client.
     * It also sends the amount of force to be applied to balance the pendulum.
     * @throws ioException
     */
    void control_pendulum(ObjectOutputStream out, ObjectInputStream in) {
        try {
            while(true){
                System.out.println("-----------------");

                // read data from client
                Object obj = in.readObject();

                // Do not process string data unless it is "bye", in which case,
                // we close the server
                if(obj instanceof String){
                    System.out.println("STRING RECEIVED: "+(String) obj);
                    if(obj.equals("bye")){
                        break;
                    }
                    continue;
                }

                double[] data= (double[])(obj);
                assert(data.length == NUM_POLES * 4);
                double[] actions = new double[NUM_POLES];

                // Get sensor data of each pole and calculate the action to be
                // applied to each inverted pendulum
                // TODO: Current implementation assumes that each pole is
                // controlled independently. This part needs to be changed if
                // the control of one pendulum needs sensing data from other
                // pendulums.
                for (int i = 0; i < NUM_POLES; i++) {
                  angle = data[i*4+0];
                  angleDot = data[i*4+1];
                  pos = data[i*4+2];
                  posDot = data[i*4+3];

                  System.out.println("server < pole["+i+"]: "+angle+"  "
                      +angleDot+"  "+pos+"  "+posDot);
                    current_pole = i;
                    if(current_pole == 0){
                        last_angle = angle;
                        last_angleDot = angleDot;
                        last_pos = pos;
                        last_posDot = posDot;
                    }
                  actions[i] = calculate_action(angle, angleDot, pos, posDot);
                }

                sendMessage_doubleArray(actions);

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            if (clientSocket != null) {
                System.out.println("closing down connection ...");
                out.writeObject("bye");
                out.flush();
                in.close();
                out.close();
                clientSocket.close();
            }
        } catch (IOException ioe) {
            System.out.println("unable to disconnect");
        }

        System.out.println("Session closed. Waiting for new connection...");

    }

    /**
     * This method calls the controller method to balance the pendulum.
     * @throws ioException
     */
    public void run() {

        try {
            control_pendulum(out, in);

        } catch (Exception ioException) {
            ioException.printStackTrace();
        } finally {
        }

    }

    double output = 0;
    double target_position = 0;

    //THIS WAS TWO CHANGE IT
    int p_gain = 10;
    int d_gain = 2;

    double p_gain_trans = .1;

    double angle_p_gain = .225;
    double angle_d_gain = .225;

    // Calculate the actions to be applied to the inverted pendulum from the
    // sensing data.
    double calculate_action(double angle, double angleDot, double pos, double posDot) {

        // // This chunk of code is purely for task 3
        // if(NUM_POLES > 1){
        //     if(current_pole == 1){
        //         //Set target position of follower pole, based on position and speed of guide pole
        //         if(pos < last_pos){
        //             target_position = last_pos - 1;
        //         }else{
        //             target_position = last_pos + (last_posDot * 2) + 1;
        //         }
        //     }else{
        //         //Set target position of guide pole
        //         target_position = 0;
        //     }
        // }

        // //Figure out how far we are from the target
        // double separation = target_position - pos;

        // //The angle offset (how far we want the pendulum tilted) is proportional to separation
        // double target_angle = separation * angle_offset_constant;

        // //Target angle is dampened based on velocity
        // target_angle = target_angle - (posDot*.1);
        
        // //We never want to tip too far
        // if(target_angle > .23){
        //     if((current_pole == 1) && last_posDot > 0){
        //         System.out.println("Target Angle: " + target_angle);
        //     }else{
        //         target_angle = .23;
        //     }
        // }else if(target_angle < -.23){
        //     target_angle = -.23;
        // }


        // //Calculate output based on angle error 
        // double angle_error = angle - target_angle;
        // double output = angle_error * p_gain;

        // //Basic d_parameter mechanism
        // if(output > 0 && angleDot < -.4){
        //     output = 0;
        // }else if(output < 0 && angleDot > .4){
        //     output = 0;
        // }

        //When you come back, try to add an adjustment based off of velocities...
        if(NUM_POLES > 1){
            if(current_pole == 1){
                //Set target position of follower pole, based on position of guide pole
                if(pos < last_pos){
                    target_position = last_pos - 1;
                }else{
                    target_position = last_pos  + 1;
                }
            }else{
                //Set target position of guide pole
                target_position = 0;
            }
        }

        double separation = target_position - pos;
        double target_angle = separation * angle_p_gain;
        target_angle = target_angle - posDot * angle_d_gain;
        double angle_error = angle - target_angle;

        output = angle_error * p_gain;
        output = output + (angleDot* d_gain);
        return output;
   }

    /**
     * This method sends the Double message on the object output stream.
     * @throws ioException
     */
    void sendMessage_double(double msg) {
        try {
            out.writeDouble(msg);
            out.flush();
            System.out.println("server>" + msg);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * This method sends the Double message on the object output stream.
     */
    void sendMessage_doubleArray(double[] data) {
        try {
            out.writeObject(data);
            out.flush();

            System.out.print("server> ");
            for(int i=0; i< data.length; i++){
                System.out.print(data[i] + "  ");
            }
            System.out.println();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }


}
