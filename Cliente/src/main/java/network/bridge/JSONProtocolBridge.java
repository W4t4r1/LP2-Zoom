package network.bridge;

import com.google.gson.Gson;
import model.MensajeSocket;

public class JSONProtocolBridge implements ProtocolBridge {
    private final Gson gson = new Gson();

    @Override
    public String serialize(MensajeSocket message) {
        return gson.toJson(message);
    }

    @Override
    public MensajeSocket deserialize(String data) {
        return gson.fromJson(data, MensajeSocket.class);
    }
}
