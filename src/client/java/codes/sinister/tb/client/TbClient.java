package codes.sinister.tb.client;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TbClient implements ClientModInitializer {
    public static final String MOD_ID = "tb";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static boolean isModEnabled = true;
    private static boolean isTriggerActive = false;
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private boolean wasPressed = false;
    private boolean canAttack = true;
    private long lastAttackTime = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Trigger Mod");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("tb")
                    .then(ClientCommandManager.literal("enable")
                            .executes(context -> {
                                isModEnabled = true;
                                sendMessage("§aTriggerbot enabled");
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(ClientCommandManager.literal("disable")
                            .executes(context -> {
                                isModEnabled = false;
                                isTriggerActive = false;
                                sendMessage("§cTriggerbot disabled");
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(ClientCommandManager.literal("status")
                            .executes(context -> {
                                String status = isModEnabled ? "§aenabled" : "§cdisabled";
                                sendMessage("§eTriggerbot is currently " + status);
                                return Command.SINGLE_SUCCESS;
                            }))
                    .executes(context -> {
                        sendMessage("§eUsage: /tb <enable|disable|status>");
                        return Command.SINGLE_SUCCESS;
                    }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isModEnabled) return;

            boolean isPressed = GLFW.glfwGetMouseButton(CLIENT.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_5) == GLFW.GLFW_PRESS;

            if (isPressed && !wasPressed) {
                LOGGER.info("Trigger activated");
                isTriggerActive = true;
            } else if (!isPressed && wasPressed) {
                LOGGER.info("Trigger deactivated");
                isTriggerActive = false;
            }

            wasPressed = isPressed;

            if (isTriggerActive && canAttack) {
                checkAndAttack();
            }
        });
    }

    private void checkAndAttack() {
        if (!isTriggerActive || CLIENT.player == null || CLIENT.interactionManager == null) return;
        if (CLIENT.crosshairTarget instanceof EntityHitResult hitResult) {
            if (hitResult.getEntity() instanceof PlayerEntity) {
                float cooldown = CLIENT.player.getAttackCooldownProgress(0.0f);
                long currentTime = System.currentTimeMillis();
                if (cooldown >= 1.0f && currentTime - lastAttackTime >= 625) {

                    CLIENT.interactionManager.attackEntity(CLIENT.player, hitResult.getEntity());
                    CLIENT.player.swingHand(CLIENT.player.getActiveHand());

                    lastAttackTime = currentTime;
                    canAttack = false;

                    new Thread(() -> {
                        try {
                            Thread.sleep(625);
                            canAttack = true;
                        } catch (InterruptedException e) {
                            LOGGER.error("Attack cooldown interrupted", e);
                        }
                    }).start();
                }
            }
        }
    }

    private void sendMessage(String message) {
        if (CLIENT.player != null) {
            CLIENT.player.sendMessage(Text.literal(message), false);
        }
    }
}
