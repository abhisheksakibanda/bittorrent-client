import util.BencodeCodec;
import util.PeerIdGenerator;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Torrent {
    private final String announce;
    private final Map<String, Object> info;
    private final String infoHash;
    private final String peerId = PeerIdGenerator.generatePeerId();

    public Torrent(byte[] bencodedValue) throws NoSuchAlgorithmException, IOException {
        Object decoded = BencodeCodec.decodeBencode(bencodedValue, new int[]{0});

        if (!(decoded instanceof Map)) {
            throw new IllegalArgumentException("Invalid torrent file format: Expected a dictionary");
        }

        Map<String, Object> map = (Map<String, Object>) decoded;
        announce = map.getOrDefault("announce", "Unknown").toString();
        info = Collections.unmodifiableMap((Map<String, Object>) map.getOrDefault("info", Collections.emptyMap()));

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        infoHash = HexFormat.of().formatHex(digest.digest(BencodeCodec.encodeBencode(info)));
    }

    public List<String> getPiecesHashes() {
        List<String> piecesHashes = new ArrayList<>();
        for (int i = 0; i < ((byte[]) info.get("pieces")).length; i += 20) {
            piecesHashes.add(HexFormat.of().formatHex(Arrays.copyOfRange((byte[]) info.get("pieces"), i, i + 20)));
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

    public String getPeerId() {
        return peerId;
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
