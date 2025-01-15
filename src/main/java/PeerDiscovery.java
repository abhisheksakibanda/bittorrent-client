import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import util.BencodeCodec;
import util.PeerIdGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public class PeerDiscovery {
    private final String trackerUrl;
    private final String infoHash;
    private final String peerId;
    private final int port = 6881;
    private final long left;
    private final int uploaded = 0;
    private final int downloaded = 0;
    private final int compact = 1;

    PeerDiscovery(Torrent torrent) {
        trackerUrl = torrent.getAnnounce();
        left = (long) torrent.getInfo().get("length");
        peerId = torrent.getPeerId();
        infoHash = torrent.getInfoHash();
    }


    public List<String> getPeers() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            // Build the request URL
            URIBuilder requestUrlBuilder = new URIBuilder(trackerUrl).setParameters(
                    new BasicNameValuePair("peer_id", peerId),
                    new BasicNameValuePair("port", String.valueOf(port)),
                    new BasicNameValuePair("uploaded", String.valueOf(uploaded)),
                    new BasicNameValuePair("downloaded", String.valueOf(downloaded)),
                    new BasicNameValuePair("left", String.valueOf(left)),
                    new BasicNameValuePair("compact", String.valueOf(compact)),
                    new BasicNameValuePair("info_hash", "")
            );

            String requestUrl = requestUrlBuilder.build() + urlEncodeInfoHash(infoHash);

            HttpGet request = new HttpGet(requestUrl);

            // Handle the response
            HttpClientResponseHandler<byte[]> responseHandler = response -> {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    throw new HttpException("Tracker request failed: HTTP " + statusCode);
                }
                try (InputStream content = response.getEntity().getContent()) {
                    return content.readAllBytes();
                }
            };

            byte[] trackerResponse = httpClient.execute(request, responseHandler);
            return parsePeers(trackerResponse);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Error fetching peers: " + e.getMessage(), e);
        }
    }

    private String urlEncodeInfoHash(String hexInfoHash) {
        // Step 1: Convert hex string to binary data
        byte[] binaryInfoHash = HexFormat.of().parseHex(hexInfoHash);

        // Step 2: URL-encode the binary data
        StringBuilder encoded = new StringBuilder();
        for (byte b : binaryInfoHash) {
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9') || b == '-' || b == '_' || b == '.' || b == '~') {
                // Alphanumeric or safe character, add as-is
                encoded.append((char) b);
            } else {
                // Unsafe character, convert to %XX format
                encoded.append(String.format("%%%02x", b)); // Use %02x for lowercase if needed
            }
        }
        return encoded.toString();
    }

    private List<String> parsePeers(byte[] content) throws IOException {
        if (BencodeCodec.decodeBencode(content, new int[]{0}) instanceof Map<?, ?> decodedDictionary) {
            if (decodedDictionary.containsKey("peers")) {
                return getAddresses((byte[]) decodedDictionary.get("peers"));
            } else {
                throw new IllegalArgumentException("Invalid tracker response format: Missing 'peers' key");
            }
        } else {
            throw new IllegalArgumentException("Invalid tracker response format: Expected a dictionary");
        }
    }

    private List<String> getAddresses(byte[] peers) {
        List<String> addresses = new ArrayList<>();
        for (int i = 0; i < peers.length; i += 6) {
            String ip = String.format("%d.%d.%d.%d", peers[i] & 0xFF, peers[i + 1] & 0xFF, peers[i + 2] & 0xFF, peers[i + 3] & 0xFF);
            int port = ((peers[i + 4] & 0xFF) << 8) | (peers[i + 5] & 0xFF);
            addresses.add(ip + ":" + port);
        }
        return addresses;
    }
}
