import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: decode <bencodedValue> | info <torrentFile>");
            return;
        }

        String command = args[0];
        switch (command) {
            case "decode" -> decode(args[1]);
            case "info" -> {
                Path path = Paths.get(args[1]);
                String bencodedValue = Files.readString(path, StandardCharsets.ISO_8859_1);
                decode(bencodedValue);
            }
            default -> System.out.println("Unknown command: " + command);
        }
    }

    private static void decode(String bencodedValue) {
        try {
            Object decoded = decodeBencode(bencodedValue, new int[]{0});
            if (decoded instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) decoded;
                if (map.containsKey("announce")) {
                    System.out.println("Tracker URL: " + map.get("announce"));
                    if (map.containsKey("info")) {
                        Map<String, Object> info = (Map<String, Object>) map.get("info");
                        System.out.println("Length: " + info.get("length"));
                    }
                } else {
                    System.out.println(gson.toJson(decoded));
                }
            } else {
                System.out.println(decoded);
            }
        } catch (RuntimeException e) {
            System.err.println("Decoding failed: " + e.getMessage());
        }
    }

    static Object decodeBencode(String bencodedString, int[] index) {
        char current = bencodedString.charAt(index[0]);
        if (Character.isDigit(current)) {
            int colonPos = bencodedString.indexOf(':', index[0]);
            if (colonPos == -1) {
                throw new RuntimeException("Invalid string format at index " + index[0]);
            }
            int length = Integer.parseInt(bencodedString.substring(index[0], colonPos));
            int startIndex = colonPos + 1;
            index[0] = startIndex + length;
            return bencodedString.substring(startIndex, index[0]);
        } else if (current == 'i') {
            int endIndex = bencodedString.indexOf('e', index[0]);
            if (endIndex == -1) {
                throw new RuntimeException("Invalid integer format at index " + index[0]);
            }
            long value = Long.parseLong(bencodedString.substring(index[0] + 1, endIndex));
            index[0] = endIndex + 1;
            return value;
        } else if (current == 'l') {
            index[0]++;
            List<Object> list = new ArrayList<>();
            while (bencodedString.charAt(index[0]) != 'e') {
                list.add(decodeBencode(bencodedString, index));
            }
            index[0]++;
            return list;
        } else if (current == 'd') {
            index[0]++;
            Map<String, Object> map = new HashMap<>();
            while (bencodedString.charAt(index[0]) != 'e') {
                String key = decodeBencode(bencodedString, index).toString();
                Object value = decodeBencode(bencodedString, index);
                map.put(key, value);
            }
            index[0]++;
            return map;
        } else {
            throw new RuntimeException("Unsupported format at index " + index[0]);
        }
    }
}
