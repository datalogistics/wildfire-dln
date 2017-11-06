/**
 * ATAK External GPS Simulation CoT message producer.
 * 
 * Produced for use under the ATAK program.
 *
 */

import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;

public class ExternalGPSSim {
    String address;
    int port;
    final int period = 50;
    BufferedReader br;

    /**
     * Publish the message to a given address and port.
     * 
     * @param p is the payload to be published
     * @param address is the address to publish the payload to
     * @param port is the port to publish the payload to
     */
    static public void publish(final MulticastSocket socket, final String p,
            final InetAddress inetAddr, final int port) throws IOException {
        if (p.length() == 0)
            return;

        final byte[] buf = p.getBytes("UTF-8");
        DatagramPacket packet = new DatagramPacket(buf, buf.length,
                inetAddr, port);

        socket.send(packet);
    }

    public ExternalGPSSim(final String address, String file) throws IOException {
        this.address = address;
        this.port = 31976;
        FileInputStream fis = new FileInputStream(file);
        br = new BufferedReader(new InputStreamReader(fis));
    }

    public void start() {

        try {
            MulticastSocket socket = new MulticastSocket();
            socket.setTimeToLive(12);
            InetAddress inetAddr = InetAddress.getByName(address);

            while (true) {
                try {
                    Thread.sleep(period);
                } catch (Exception e) {
                }

                String p = br.readLine();
                System.out.println(p);
                if (p == null)
                    return;
                publish(socket, p, inetAddr, port);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Prints the usage parameters for the External GPS Simulator.
     */
    static private void printUsage() {
        System.out
                .println("java ExternalGPSSim");
        System.out.println("             address - address of device");
        System.out
                .println("               latitude - initial seed value for the latitude (def: 42)");
        System.out
                .println("               longitude - initial seed value for the longitude (def: -74)");
    }

    public static void main(String[] args) throws IOException {
        String file = null;
        String address = null;

        for (int i = 0; i < args.length; ++i) {
            boolean hasMore = (i + 1 < args.length);
            if (args[i].equalsIgnoreCase("--address") && hasMore) {
                try {
                    address = args[++i];
                } catch (Exception e) {
                }
            } else if (args[i].equalsIgnoreCase("--file") && hasMore) {
                try {
                    file = args[++i];
                } catch (Exception e) {
                }
            } else {
                System.err.println("unknown parameter: " + args[i]);
            }
        }

        if (address == null) {
            System.out.println("specify --address for the device to spoof");
            return;
        }

        if (file == null) {
            System.out
                    .println("specify --file for the device spoof information");
            return;
        }

        ExternalGPSSim wrm = new ExternalGPSSim(address, file);
        wrm.start();

    }

}
