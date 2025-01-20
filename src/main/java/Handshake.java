import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HexFormat;

public class Handshake {
    private final Torrent torrent;
    private final String peerIp;
    private final int peerPort;

    public Handshake(Torrent torrent, String peerIp, int peerPort) {
        this.torrent = torrent;
        this.peerIp = peerIp;
        this.peerPort = peerPort;
    }

    public static void handleHandshake(Torrent torrent, String peerIp, int peerPort) throws IOException {
        Handshake handshake = new Handshake(torrent, peerIp, peerPort);
        try(Socket socket = handshake.initiateHandshake()) {
            // Read handshake response
            InputStream input = socket.getInputStream();
            byte[] response = new byte[68];
            int bytesRead = input.read(response);
            if (bytesRead != 68) {
                throw new IOException("Invalid handshake response");
            } else {
                if (!verifyHandshake(response)) {
                    throw new IOException("Invalid handshake protocol identifier");
                } else {
                    // Return the socket if the handshake is successful
                    System.out.println("Peer ID: " + HexFormat.of().formatHex(response, 48, response.length));
                }
            }
        }
    }

    public Socket initiateHandshake() {
        try {
            Socket socket = new Socket(peerIp, peerPort);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(createHandshake());
            outputStream.flush();
            socket.setSoTimeout(10000);
            return socket;
        } catch (SocketException e) {
            throw new RuntimeException("Socket Exception: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Error sending handshake: " + e.getMessage());
        }
    }

    private byte[] createHandshake() throws IOException {
        // Create the Peer handshake message
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(19); // Length of the protocol identifier
        outputStream.write("BitTorrent protocol".getBytes());
        outputStream.write(new byte[8]); // 8 reserved bytes
        outputStream.write(HexFormat.of().parseHex(torrent.getInfoHash()));
        outputStream.write(torrent.getPeerId().getBytes()); // Random peer ID
        return outputStream.toByteArray();
    }

    public static boolean verifyHandshake(byte[] handshake) {
        return handshake.length != 0 && handshake[0] == 19 && new String(handshake, 1, 19).equals("BitTorrent protocol");
    }
}
