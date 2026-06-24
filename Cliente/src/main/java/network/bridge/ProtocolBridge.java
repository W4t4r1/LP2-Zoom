package network.bridge;

import model.MensajeSocket;

public interface ProtocolBridge {
    String serialize(MensajeSocket message);
    MensajeSocket deserialize(String data);
}
