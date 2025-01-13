import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
                    Object decodedValue;
                    if (value instanceof byte[]) {
                        decodedValue = "pieces".equals(key) ? value : new String((byte[]) value);
                    } else {
                        decodedValue = value;
                    }
                    map.put(key, decodedValue);
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

    public static byte[] encodeBencode(Object value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot encode null value.");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        switch (value) {
            case Long num -> outputStream.write(('i' + String.valueOf(num) + 'e').getBytes());
            case String str -> {
                String encodedStr = str.length() + ":" + str;
                outputStream.write(encodedStr.getBytes());
            }
            case byte[] bytes -> {
                String lengthPrefix = bytes.length + ":";
                outputStream.write(lengthPrefix.getBytes());
                outputStream.write(bytes);
            }
            case List<?> list -> {
                outputStream.write('l');
                for (Object item : list) {
                    outputStream.write(encodeBencode(item));
                }
                outputStream.write('e');
            }
            case Map<?, ?> map -> {
                outputStream.write('d');
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    outputStream.write(encodeBencode(entry.getKey()));
                    outputStream.write(encodeBencode(entry.getValue()));
                }
                outputStream.write('e');
            }
            default ->
                    throw new IllegalArgumentException("Unsupported data type for encoding: " + value.getClass().getName());
        }

        return outputStream.toByteArray();
    }
}
