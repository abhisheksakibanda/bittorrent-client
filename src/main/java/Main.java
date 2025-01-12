import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: decode <bencodedValue> | info <torrentFile>");
            return;
        }

        String command = args[0];

        try {
            switch (command) {
                case "decode" -> decode(args[1]);
                case "info" -> parseTorrentFile(args);
                default -> System.out.println("Unknown command: " + command);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static void parseTorrentFile(String[] args) throws IOException {
        Path path = Paths.get(args[1]);
        Torrent torrent = new Torrent(Files.readString(path, StandardCharsets.ISO_8859_1));
        System.out.println(torrent);
    }

    private static void decode(String bencodedValue) {
        try {
            Object decoded = Torrent.decodeBencode(bencodedValue, new int[]{0});
            System.out.println(gson.toJson(decoded));
        } catch (RuntimeException e) {
            System.err.println("Decoding failed: " + e.getMessage());
        }
    }
}
