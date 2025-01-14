package util;

import java.security.SecureRandom;

public class PeerIdGenerator {

    // Define the prefix for the client (e.g., "-UT3100-" for uTorrent 3.1.0.0)
    private static final String CLIENT_PREFIX = "-UT3100-";
    private static final int RANDOM_PART_LENGTH = 12;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String generatePeerId() {
        SecureRandom random = new SecureRandom();
        StringBuilder randomPart = new StringBuilder(RANDOM_PART_LENGTH);

        // Generate a random alphanumeric string of the required length
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            randomPart.append(CHARACTERS.charAt(index));
        }

        // Combine the prefix with the random part
        return CLIENT_PREFIX + randomPart;
    }

    public static void main(String[] args) {
        // Generate and print a random peer_id
        String peerId = generatePeerId();
        System.out.println("Generated Peer ID: " + peerId);
    }
}
