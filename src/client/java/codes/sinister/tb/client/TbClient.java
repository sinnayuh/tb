package codes.sinister.tb.client;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
        registerTickHandler();

        // Register event handlers to track movement and attacks
//        AttackEventHandler.registerAttackEvent();
//        MovementEventHandler.registerMovementEvent();

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

    private double getDistanceToBoundingBoxEdge(PlayerEntity player, Entity targetEntity) {
        Box boundingBox = targetEntity.getBoundingBox();
        Vec3d eyePos = player.getEyePos();

        double clampedX = MathHelper.clamp(eyePos.x, boundingBox.minX, boundingBox.maxX);
        double clampedY = MathHelper.clamp(eyePos.y, boundingBox.minY, boundingBox.maxY);
        double clampedZ = MathHelper.clamp(eyePos.z, boundingBox.minZ, boundingBox.maxZ);

        Vec3d closestPoint = new Vec3d(clampedX, clampedY, clampedZ);

        return eyePos.squaredDistanceTo(closestPoint);
    }

    private int cooldownTicks = 0; // Number of ticks left for cooldown (20 ticks = 1 second)
    private boolean hasLoggedCooldown = false; // Tracks if cooldown message has already been logged

    private void checkAndAttack() {
        if (!isTriggerActive || CLIENT.player == null || CLIENT.interactionManager == null) return;

        if (CLIENT.crosshairTarget instanceof EntityHitResult hitResult) {
            Entity targetEntity = hitResult.getEntity();
            if (targetEntity instanceof PlayerEntity) {
                // Immediately block further checks if cooldown is active
                if (cooldownTicks > 0) {
                    if (!hasLoggedCooldown) {
                        LOGGER.info("Attack skipped due to active cooldown (ticks left: {}).", cooldownTicks);
                        hasLoggedCooldown = true; // Log the message only once
                    }
                    return;
                }

                // Reset logging flag once cooldown expires
                hasLoggedCooldown = false;

                // Calculate distance to the bounding box edge
                double distance = getDistanceToBoundingBoxEdge(CLIENT.player, targetEntity);
                double maxReachSquared = 3.0 * 3.0; // Melee reach squared
                if (distance > maxReachSquared) {
                    LOGGER.warn("Target out of reach (distance: {}). Attack aborted.", Math.sqrt(distance));
                    return;
                }

                // Ensure player is on stable ground
                if (!CLIENT.player.isOnGround()) {
                    LOGGER.info("Player not on stable ground. Attack skipped.");
                    return;
                }

                // Check attack cooldown progress
                float cooldown = CLIENT.player.getAttackCooldownProgress(0.0f);
                if (cooldown < 1.0f) {
                    LOGGER.info("Cooldown not met (progress: {}). Attack skipped.", cooldown);
                    return;
                }

                // Perform attack
                CLIENT.interactionManager.attackEntity(CLIENT.player, targetEntity);
                CLIENT.player.swingHand(CLIENT.player.getActiveHand());
                LOGGER.info("Attack successfully executed at distance {}.", Math.sqrt(distance));

                // Immediately set cooldownTicks to start cooldown
                cooldownTicks = 13; // ~625ms in ticks
            }
        }
    }

    // Tick event listener to decrement cooldown ticks
    public void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (cooldownTicks > 0) {
                cooldownTicks--; // Decrease cooldown each tick
                if (cooldownTicks == 0) {
                    LOGGER.info("Cooldown expired. Ready to attack.");
                }
            }
        });
    }

    private void sendMessage(String message) {
        if (CLIENT.player != null) {
            CLIENT.player.sendMessage(Text.literal(message), false);
        }
    }
}
