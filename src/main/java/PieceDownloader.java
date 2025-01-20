import util.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PieceDownloader {
    private final Torrent torrent;
    private final int pieceIndex;

    public PieceDownloader(Torrent torrent, int pieceIndex) {
        this.torrent = torrent;
        this.pieceIndex = pieceIndex;
    }

    public byte[] downloadPiece() throws IOException {
        // Discover peers
        PeerDiscovery peerDiscovery = new PeerDiscovery(torrent);
        List<String> peers = peerDiscovery.getPeers();

        for (String peer : peers) {
            // Split peer information (host:port)
            String[] peerUri = peer.split(":");
            Handshake handshake = new Handshake(torrent, peerUri[0], Integer.parseInt(peerUri[1]));

            try (
                    Socket peerSocket = handshake.initiateHandshake();
                    InputStream inputStream = peerSocket.getInputStream();
                    OutputStream outputStream = peerSocket.getOutputStream()
            ) {
                // Validate handshake
                int pieceLength = Long.valueOf((long) torrent.getInfo().getOrDefault("piece length", 0)).intValue();
                byte[] response = new byte[pieceLength];
                int bytesRead = inputStream.read(response);

                if (bytesRead > 0 && Handshake.verifyHandshake(response)) {
                    System.out.println("Handshake verified. Checking for bitfield message...");

                    Thread.sleep(3000);  // Wait for the peer to send the bitfield message
                    // Check for a bitfield message
                    if (checkForBitFieldMessage(inputStream)) {
                        System.out.println("Bitfield message received. Sending interested message...");
                        outputStream.write(Message.createInterested().toBytes());
                        outputStream.flush();

                        // Handle additional communication here
                        System.out.println("Communication established with peer: " + peer);
                        break;
                    } else {
                        throw new IOException("No valid bitfield message received");
                    }
                } else {
                    throw new IOException("Invalid handshake protocol identifier");
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to download piece from peer: " + peer + " - " + e.getMessage());
            }
        }
        return new byte[0];
    }

    private boolean checkForBitFieldMessage(InputStream inputStream) throws IOException {
        // Read the message length (4 bytes)
        byte[] lengthBuffer = new byte[4];
        readFully(inputStream, lengthBuffer);

        // Convert the byte array to an integer using ByteBuffer
        int messageLength = ByteBuffer.wrap(lengthBuffer).getInt();

        if (messageLength == 0) {
            System.out.println("Keep-alive message received.");
            return false;
        }

        // Read the message ID (1 byte)
        int messageID = inputStream.read();
        if (messageID == -1) {
            throw new IOException("Stream closed unexpectedly while reading message ID");
        }

        // Check if the message ID corresponds to a bitfield (5)
        return messageID == 5;
    }

    private void readFully(InputStream inputStream, byte[] buffer) throws IOException {
        int bytesRead = 0;
        while (bytesRead < buffer.length) {
            int result = inputStream.read(buffer, bytesRead, buffer.length - bytesRead);
            if (result == -1) {
                throw new IOException("End of stream reached before buffer was fully read");
            }
            bytesRead += result;
        }
    }

    public void savePiece(String outputLocation, byte[] pieceData) throws IOException {
        // Save the piece data to the specified output location
        Path path = Paths.get(outputLocation);
        Files.write(path, pieceData);
    }
}