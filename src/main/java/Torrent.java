import java.util.*;

public class Torrent {
    private final String announce;
    private final Map<String, Object> info;

    public Torrent(String bencodedValue) {
        Object decoded = decodeBencode(bencodedValue, new int[]{0});

        if (!(decoded instanceof Map)) {
            throw new IllegalArgumentException("Invalid torrent file format: Expected a dictionary");
        }

        Map<String, Object> map = (Map<String, Object>) decoded;
        announce = map.getOrDefault("announce", "Unknown").toString();
        info = Collections.unmodifiableMap((Map<String, Object>) map.getOrDefault("info", Collections.emptyMap()));
    }

    public static Object decodeBencode(String bencodedString, int[] index) {
        char current = bencodedString.charAt(index[0]);
        switch (current) {
            case 'i' -> {
                int endIndex = bencodedString.indexOf('e', index[0]);
                if (endIndex == -1) {
                    throw new RuntimeException("Invalid integer format at index " + index[0]);
                }
                long value = Long.parseLong(bencodedString.substring(index[0] + 1, endIndex));
                index[0] = endIndex + 1;
                return value;
            }
            case 'l' -> {
                index[0]++;
                List<Object> list = new ArrayList<>();
                while (bencodedString.charAt(index[0]) != 'e') {
                    list.add(decodeBencode(bencodedString, index));
                }
                index[0]++;
                return list;
            }
            case 'd' -> {
                index[0]++;
                Map<String, Object> map = new HashMap<>();
                while (bencodedString.charAt(index[0]) != 'e') {
                    String key = decodeBencode(bencodedString, index).toString();
                    Object value = decodeBencode(bencodedString, index);
                    map.put(key, value);
                }
                index[0]++;
                return map;
            }
            default -> {
                if (Character.isDigit(current)) {
                    int colonPos = bencodedString.indexOf(':', index[0]);
                    if (colonPos == -1) {
                        throw new RuntimeException("Invalid string format at index " + index[0]);
                    }
                    int length = Integer.parseInt(bencodedString.substring(index[0], colonPos));
                    int startIndex = colonPos + 1;
                    index[0] = startIndex + length;
                    return bencodedString.substring(startIndex, index[0]);
                } else {
                    throw new RuntimeException("Unsupported format at index " + index[0]);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Tracker URL: " + announce + "\nLength: " + info.getOrDefault("length", "N/A");
    }
}
