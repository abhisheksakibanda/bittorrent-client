import java.util.*;

public class BencodeCodec {
    public static Object decodeBencode(byte[] bencodedString, int[] index) {
        char current = (char) bencodedString[index[0]];  // casting byte to char for easy comparison
        switch (current) {
            case 'i' -> {
                int endIndex = findEndIndex(bencodedString, index);
                long value = Long.parseLong(new String(bencodedString, index[0] + 1, endIndex - index[0] - 1));
                index[0] = endIndex + 1;
                return value;
            }
            case 'l' -> {
                index[0]++;
                List<Object> list = new ArrayList<>();
                while (bencodedString[index[0]] != 'e') {
                    list.add(decodeBencode(bencodedString, index));
                }
                index[0]++;
                return list;
            }
            case 'd' -> {
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
                    int colonPos = findIndexOf(bencodedString, ':', index[0]);
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
        int endIndex = findIndexOf(bencodedString, 'e', index[0]);
        if (endIndex == -1) {
            throw new RuntimeException("Invalid format at index " + index[0]);
        }
        return endIndex;
    }

    private static int findIndexOf(byte[] array, char target, int startIndex) {
        for (int i = startIndex; i < array.length; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }

    public static byte[] encodeBencode(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot encode null value.");
        }

        return switch (value) {
            case Long num -> // Encoding an integer: i<value>e
                    ("i" + num + "e").getBytes();
            case String str -> // Encoding a string: <length>:<string>
                    (str.length() + ":" + str).getBytes();
            case byte[] bytes -> { // Encoding raw bytes: <length>:<bytes>
                String lengthPrefix = bytes.length + ":";
                byte[] lengthBytes = lengthPrefix.getBytes();
                byte[] result = new byte[lengthBytes.length + bytes.length];
                System.arraycopy(lengthBytes, 0, result, 0, lengthBytes.length);
                System.arraycopy(bytes, 0, result, lengthBytes.length, bytes.length);
                yield result;
            }
            case List<?> list -> { // Encoding a list: l<encoded values>e
                List<byte[]> encodedItems = new ArrayList<>();
                int totalLength = 0;
                for (Object item : list) {
                    byte[] encodedItem = encodeBencode(item);
                    encodedItems.add(encodedItem);
                    totalLength += encodedItem.length;
                }
                byte[] result = new byte[totalLength + 2]; // 2 for 'l' and 'e'
                result[0] = 'l';
                int offset = 1;
                for (byte[] encodedItem : encodedItems) {
                    System.arraycopy(encodedItem, 0, result, offset, encodedItem.length);
                    offset += encodedItem.length;
                }
                result[offset] = 'e';
                yield result;
            }
            case Map<?, ?> map -> { // Encoding a dictionary: d<encoded key-value pairs>e
                Map<String, Object> stringKeyMap = (Map<String, Object>) map;
                List<byte[]> encodedEntries = new ArrayList<>();
                int totalLength = 0;
                for (Map.Entry<String, Object> entry : stringKeyMap.entrySet()) {
                    byte[] encodedKey = encodeBencode(entry.getKey());
                    byte[] encodedValue = encodeBencode(entry.getValue());
                    encodedEntries.add(encodedKey);
                    encodedEntries.add(encodedValue);
                    totalLength += encodedKey.length + encodedValue.length;
                }
                byte[] result = new byte[totalLength + 2]; // 2 for 'd' and 'e'
                result[0] = 'd';
                int offset = 1;
                for (byte[] entry : encodedEntries) {
                    System.arraycopy(entry, 0, result, offset, entry.length);
                    offset += entry.length;
                }
                result[offset] = 'e';
                yield result;
            }
            default ->
                    throw new IllegalArgumentException("Unsupported data type for encoding: " + value.getClass().getName());
        };
    }
}
