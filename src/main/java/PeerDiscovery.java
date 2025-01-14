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
import java.util.List;
import java.util.Map;

public class PeerDiscovery {
    private final String trackerUrl;
    private final String infoHash;
    private final String peerId = PeerIdGenerator.generatePeerId();
    private final int port = 6881;
    private int uploaded = 0;
    private int downloaded = 0;
    private final long left;
    private int compact = 1;

    PeerDiscovery(Torrent torrent) {
        trackerUrl = torrent.getAnnounce();
        infoHash = createInfoHash(torrent.getInfoHash());
        left = (long) torrent.getInfo().get("length");
    }

    private String createInfoHash(String infoHash) {
        StringBuilder encodedString = new StringBuilder();

        for (int i = 0; i < infoHash.length(); i += 2) {
            // Get the two characters from the info hash string
            String hexPair = infoHash.substring(i, i + 2);

            // Convert the hex pair to its byte value
            int byteValue = Integer.parseInt(hexPair, 16);

            // Check if the byte is an ASCII letter
            if ((byteValue >= 0x41 && byteValue <= 0x5A) || (byteValue >= 0x61 && byteValue <= 0x7A)) {
                encodedString.append((char) byteValue);
            } else {
                // Encode the byte as %XX
                encodedString.append(String.format("%%%02x", byteValue));
            }
        }
        return encodedString.toString();
    }


    public List<String> getPeers() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            // Build the request URL
            String requestUrl = (new URIBuilder(trackerUrl).setParameters(
                    new BasicNameValuePair("peer_id", peerId),
                    new BasicNameValuePair("port", String.valueOf(port)),
                    new BasicNameValuePair("uploaded", String.valueOf(uploaded)),
                    new BasicNameValuePair("downloaded", String.valueOf(downloaded)),
                    new BasicNameValuePair("left", String.valueOf(left)),
                    new BasicNameValuePair("compact", String.valueOf(compact))
            ).build()) + "&info_hash=" + infoHash;

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