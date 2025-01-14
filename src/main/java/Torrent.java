import util.BencodeCodec;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Torrent {
    private final String announce;
    private final Map<String, Object> info;
    private final String infoHash;

    public Torrent(byte[] bencodedValue) throws NoSuchAlgorithmException, IOException {
        Object decoded = BencodeCodec.decodeBencode(bencodedValue, new int[]{0});

        if (!(decoded instanceof Map)) {
            throw new IllegalArgumentException("Invalid torrent file format: Expected a dictionary");
        }

        Map<String, Object> map = (Map<String, Object>) decoded;
        announce = map.getOrDefault("announce", "Unknown").toString();
        info = Collections.unmodifiableMap((Map<String, Object>) map.getOrDefault("info", Collections.emptyMap()));

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        infoHash = bytesToHex(digest.digest(BencodeCodec.encodeBencode(info)));
    }

    private static final String[] HEX_LOOKUP_TABLE = createHexLookupTable();

    private static String[] createHexLookupTable() {
        String[] lookupTable = new String[256];
        for (int i = 0; i < 256; i++) {
            lookupTable[i] = String.format("%02x", i);
        }
        return lookupTable;
    }

    private String bytesToHex(byte[] digest) {
        StringBuilder hexString = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hexString.append(HEX_LOOKUP_TABLE[b & 0xFF]);
        }
        return hexString.toString();
    }

    public List<String> getPiecesHashes() {
        List<String> piecesHashes = new ArrayList<>();
        for (int i = 0; i < ((byte[]) info.get("pieces")).length; i += 20) {
            piecesHashes.add(bytesToHex(Arrays.copyOfRange((byte[]) info.get("pieces"), i, i + 20)));
        }
        return piecesHashes;
    }

    public String getAnnounce() {
        return announce;
    }

    public Map<String, Object> getInfo() {
        return info;
    }

    public String getInfoHash() {
        return infoHash;
    }

    @Override
    public String toString() {
        String piecesHashes = String.join("\n", getPiecesHashes());

        return "Tracker URL: " + announce +
                "\nLength: " + info.getOrDefault("length", "N/A") +
                "\nInfo Hash: " + infoHash +
                "\nPiece Length: " + info.getOrDefault("piece length", "N/A") +
                "\nPieces Hashes:\n" + piecesHashes;
    }
}
