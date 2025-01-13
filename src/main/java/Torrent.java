import com.dampcake.bencode.Bencode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Torrent {
    private final String announce;
    private final Map<String, Object> info;
    private final String infoHash;

    public Torrent(byte[] bencodedValue) throws NoSuchAlgorithmException {
        Object decoded = decodeBencode(bencodedValue, new int[]{0});

        if (!(decoded instanceof Map)) {
            throw new IllegalArgumentException("Invalid torrent file format: Expected a dictionary");
        }

        Map<String, Object> map = (Map<String, Object>) decoded;
        announce = map.getOrDefault("announce", "Unknown").toString();
        info = Collections.unmodifiableMap((Map<String, Object>) map.getOrDefault("info", Collections.emptyMap()));

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        Bencode bencode = new Bencode();
        infoHash = bytesToHex(digest.digest(bencode.encode(info)));
    }

    private String bytesToHex(byte[] digest) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static Object decodeBencode(byte[] bencodedString, int[] index) {
        char current = (char) bencodedString[index[0]];  // casting byte to char for easy comparison
        switch (current) {
            case 'i' -> {
                // Decode integer
                int endIndex = findEndIndex(bencodedString, index);
                long value = Long.parseLong(new String(bencodedString, index[0] + 1, endIndex - index[0] - 1));
                index[0] = endIndex + 1;
                return value;
            }
            case 'l' -> {
                // Decode list
                index[0]++;
                List<Object> list = new ArrayList<>();
                while (bencodedString[index[0]] != 'e') {
                    list.add(decodeBencode(bencodedString, index));
                }
                index[0]++;
                return list;
            }
            case 'd' -> {
                // Decode dictionary
                index[0]++;
                Map<String, Object> map = new TreeMap<>();  // Using TreeMap to ensure alphabetical order
                while ((char) bencodedString[index[0]] != 'e') {
                    String key = new String((byte[]) decodeBencode(bencodedString, index));
                    Object value = decodeBencode(bencodedString, index);

                    if (value instanceof byte[]) {
                        // Check if the key is "pieces" and handle accordingly
                        if ("pieces".equals(key)) {
                            map.put(key, value);  // Store "pieces" as raw byte[] data
                        } else {
                            map.put(key, new String((byte[]) value));  // Convert other values to String
                        }
                    } else {
                        map.put(key, value);
                    }
                }
                index[0]++;
                return map;
            }
            default -> {
                if (Character.isDigit(current)) {
                    // Decode string
                    int colonPos = indexOfColon(bencodedString, index[0]);
                    int length = Integer.parseInt(new String(bencodedString, index[0], colonPos - index[0]));
                    int startIndex = colonPos + 1;
                    index[0] = startIndex + length;
                    return Arrays.copyOfRange(bencodedString, startIndex, index[0]);  // Returning raw bytes for strings
                } else {
                    throw new RuntimeException("Unsupported format at index " + index[0]);
                }
            }
        }
    }

    private static int findEndIndex(byte[] bencodedString, int[] index) {
        int endIndex = indexOf(bencodedString, 'e', index[0]);
        if (endIndex == -1) {
            throw new RuntimeException("Invalid format at index " + index[0]);
        }
        return endIndex;
    }

    private static int indexOf(byte[] array, char target, int startIndex) {
        for (int i = startIndex; i < array.length; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfColon(byte[] bencodedString, int startIndex) {
        return indexOf(bencodedString, ':', startIndex);
    }

    @Override
    public String toString() {
        return "Tracker URL: " + announce + "\nLength: " + info.getOrDefault("length", "N/A") + "\nInfo Hash: " + infoHash;
    }
}
