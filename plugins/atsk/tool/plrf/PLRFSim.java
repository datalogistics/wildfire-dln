import java.lang.*;
import java.net.*;
import java.util.*;
import java.io.*;

import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;

/**
 * Because this sim is meant to run on other systems, the ttl is set to a number higher than 0. For
 * standard PLRF operation, the service will only transmit multicast packets with a ttl of 0 (local
 * device).
 */
public class PLRFSim {

    /**
     * Publish the message to a given address and port.
     * 
     * @param p is the payload to be published
     * @param address is the address to publish the payload to
     * @param port is the port to publish the payload to
     * @param ttl 0 for local device
     */
    static public void publish(String p, String address, int port, int ttl) {
        if (p.length() == 0)
            return;

        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress inetAddr = InetAddress.getByName(address);

            byte[] buf = p.getBytes("UTF8");
            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    inetAddr, port);

            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public void main(String[] args) {
        if (args.length == 4) {
            publish(args[1] + "," + args[2] + "," + args[3], args[0], 31977, 1);
        } else {

            System.out
                    .println("PLRF <address> <distance m> <angle azimuth> <angle inclintation>");
        }

    }

}
