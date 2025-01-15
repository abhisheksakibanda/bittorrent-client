import util.BencodeCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: decode <bencodedValue> | info <torrentFile> | peers <torrentFile> | handshake <torrentFile> <peer_ip>:<peer_port>");
            return;
        }

        String command = args[0];

        try {
            switch (command) {
                case "decode" -> BencodeCodec.decode(args[1].getBytes());
                case "info" -> System.out.println(parseTorrentFile(args));
                case "peers" -> {
                    Torrent torrent = parseTorrentFile(args);
                    new PeerDiscovery(torrent).getPeers().forEach(System.out::println);
                }
                case "handshake" -> {
                    Torrent torrent = parseTorrentFile(args);
                    String[] parts = args[2].split(":");
                    System.out.println("Peer ID: " + new Handshake(torrent, parts[0], Integer.parseInt(parts[1])).sendHandshake());
                }
                default -> System.out.println("Unknown command: " + command);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error creating SHA-1 digest: " + e.getMessage());
        }
    }

    private static Torrent parseTorrentFile(String[] args) throws IOException, NoSuchAlgorithmException {
        Path path = Paths.get(args[1]);
        byte[] bencodedValue = Files.readAllBytes(path);
        return new Torrent(bencodedValue);
    }
}
