package util;

import java.nio.ByteBuffer;

/**
 * Represents a protocol message exchanged between peers.
 */
public class Message {

    private final int length;
    private final MessageType type;
    private final byte[] payload;

    /**
     * Enum for message types.
     */
    public enum MessageType {
        CHOKE(0),
        UNCHOKE(1),
        INTERESTED(2),
        NOT_INTERESTED(3),
        HAVE(4),
        BITFIELD(5),
        REQUEST(6),
        PIECE(7),
        CANCEL(8),
        KEEP_ALIVE(-1); // Keep-alive messages have no ID or payload

        private final int id;

        MessageType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static MessageType fromId(int id) {
            for (MessageType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown message ID: " + id);
        }
    }

    public Message(int length, MessageType type, byte[] payload) {
        this.length = length;
        this.type = type;
        this.payload = payload;
    }

    /**
     * Serialize the message into a byte array for transmission.
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + (payload == null ? 0 : payload.length));
        buffer.putInt(length); // Length prefix
        buffer.put((byte) type.getId()); // Message ID
        if (payload != null) {
            buffer.put(payload); // Payload
        }
        return buffer.array();
    }

    /**
     * Parse a byte array into a Message object.
     */
    public static Message fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int length = buffer.getInt(); // Length prefix
        byte id = buffer.get();       // Message ID
        MessageType type = MessageType.fromId(id);

        byte[] payload = null;
        if (length > 1) {
            payload = new byte[length - 1];
            buffer.get(payload); // Remaining payload
        }

        return new Message(length, type, payload);
    }

    // Factory methods for specific message types

    public static Message createKeepAlive() {
        return new Message(0, MessageType.KEEP_ALIVE, null); // Keep-alive has no ID or payload
    }

    public static Message createChoke() {
        return new Message(1, MessageType.CHOKE, null);
    }

    public static Message createUnchoke() {
        return new Message(1, MessageType.UNCHOKE, null);
    }

    public static Message createInterested() {
        return new Message(1, MessageType.INTERESTED, null);
    }

    public static Message createNotInterested() {
        return new Message(1, MessageType.NOT_INTERESTED, null);
    }

    public static Message createHave(int pieceIndex) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(pieceIndex);
        return new Message(5, MessageType.HAVE, buffer.array());
    }

    public static Message createBitfield(boolean[] pieces) {
        int byteLength = (int) Math.ceil(pieces.length / 8.0);
        byte[] bitfield = new byte[byteLength];

        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i]) {
                bitfield[i / 8] |= (byte) (1 << (7 - (i % 8))); // Set the corresponding bit to 1
            }
        }

        return new Message(1 + bitfield.length, MessageType.BITFIELD, bitfield);
    }

    public static Message createRequest(int pieceIndex, int begin, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(pieceIndex);
        buffer.putInt(begin);
        buffer.putInt(length);
        return new Message(13, MessageType.REQUEST, buffer.array());
    }

    public static Message createPiece(int pieceIndex, int begin, byte[] block) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + block.length);
        buffer.putInt(pieceIndex);
        buffer.putInt(begin);
        buffer.put(block);
        return new Message(9 + block.length, MessageType.PIECE, buffer.array());
    }

    public static Message createCancel(int pieceIndex, int begin, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(pieceIndex);
        buffer.putInt(begin);
        buffer.putInt(length);
        return new Message(13, MessageType.CANCEL, buffer.array());
    }

    // Getters for length, type, and payload
    public int getLength() {
        return length;
    }

    public MessageType getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }
}
