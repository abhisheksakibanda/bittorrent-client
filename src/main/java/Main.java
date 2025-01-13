import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: decode <bencodedValue> | info <torrentFile> | peers <torrentFile>");
            return;
        }

        String command = args[0];

        try {
            switch (command) {
                case "decode" -> decode(args[1].getBytes());
                case "info" -> System.out.println(parseTorrentFile(args));
                case "peers" -> {
                    Torrent torrent = parseTorrentFile(args);

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

    private static void decode(byte[] bencodedValue) {
        try {
            Object decoded = BencodeCodec.decodeBencode(bencodedValue, new int[]{0});
            System.out.println(gson.toJson(decoded));
        } catch (RuntimeException e) {
            System.err.println("Decoding failed: " + e.getMessage());
        }
    }
}
