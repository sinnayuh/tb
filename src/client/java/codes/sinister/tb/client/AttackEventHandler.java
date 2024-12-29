package codes.sinister.tb.client;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;

public class AttackEventHandler {
    public static void registerAttackEvent() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity != null) {
                System.out.println("Player attacked entity: " + entity.getName().getString());
            }
            return ActionResult.PASS; // Let the attack continue as normal.
        });
    }
}

