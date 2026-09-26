package com.scoder.jusic.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Local WebSocket connection limits for one application node.
 *
 * <p>The limits are deliberately local. A multi-node deployment applies the
 * node limit independently on every node, while the room and IP limits protect
 * the sessions admitted by that node.</p>
 */
@Component
@ConfigurationProperties(prefix = "jusic.websocket.capacity")
public class ConnectionCapacityProperties {

    private int node = 2000;
    private int room = 1000;
    private int ip = 100;

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = requirePositive("node", node);
    }

    public int getRoom() {
        return room;
    }

    public void setRoom(int room) {
        this.room = requirePositive("room", room);
    }

    public int getIp() {
        return ip;
    }

    public void setIp(int ip) {
        this.ip = requirePositive("ip", ip);
    }

    private int requirePositive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("jusic.websocket.capacity." + name + " must be greater than zero");
        }
        return value;
    }
}
