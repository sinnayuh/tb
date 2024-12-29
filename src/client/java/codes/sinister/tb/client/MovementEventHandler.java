package codes.sinister.tb.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class MovementEventHandler {
    private static double lastX, lastY, lastZ;

    public static void registerMovementEvent() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                double currentX = client.player.getX();
                double currentY = client.player.getY();
                double currentZ = client.player.getZ();

                if (currentX != lastX || currentY != lastY || currentZ != lastZ) {
                    System.out.println("Player moved to X: " + currentX + ", Y: " + currentY + ", Z: " + currentZ);
                    lastX = currentX;
                    lastY = currentY;
                    lastZ = currentZ;
                }
            }
        });
    }
}