import util.PeerIdGenerator;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.HexFormat;

public class Handshake {
    private final Torrent torrent;
    private final String peerIp;
    private final int peerPort;
    private final String peerId;

    public Handshake(Torrent torrent, String peerIp, int peerPort) {
        this.torrent = torrent;
        this.peerIp = peerIp;
        this.peerPort = peerPort;
        peerId = torrent.getPeerId();
    }

    public String sendHandshake() {
        try (Socket socket = new Socket(peerIp, peerPort)) {
            socket.getOutputStream().write(createHandshake());
            // Read handshake response
            InputStream input = socket.getInputStream();
            byte[] response = new byte[68];
            int bytesRead = input.read(response);
            if (bytesRead != 68) {
                throw new IOException("Invalid handshake response");
            } else {
                if (response[0] != 19 && new String(response, 1, 19).equals("BitTorrent protocol")) {
                    throw new IOException("Invalid handshake protocol identifier");
                } else {
                    // Return hexadecimal representation peer id in response
                    return HexFormat.of().formatHex(response, 48, response.length);
                }
            }
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
        outputStream.write(peerId.getBytes()); // Random peer ID
        return outputStream.toByteArray();
    }
}
