package com.example;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.HashMap;
import java.util.UUID;

public class ExampleMod implements ModInitializer {
    public static final HashMap<UUID, String> playerNinjaMap = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player.getPersistentData().contains("NinjagoClass")) {
                playerNinjaMap.put(player.getUuid(), player.getPersistentData().getString("NinjagoClass"));
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("ninja").then(CommandManager.argument("name", StringArgumentType.string()).executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                if (player == null || playerNinjaMap.containsKey(player.getUuid())) return 0;
                String name = StringArgumentType.getString(context, "name").toLowerCase();
                if (name.equals("kai") || name.equals("jay") || name.equals("cole") || name.equals("nya") || name.equals("zane") || name.equals("morro")) {
                    String fmt = name.substring(0, 1).toUpperCase() + name.substring(1);
                    playerNinjaMap.put(player.getUuid(), fmt);
                    player.getPersistentData().putString("NinjagoClass", fmt);
                    player.sendMessage(Text.literal("§6[Ninjago] §aDu bist jetzt Meister von: §e" + fmt), false);
                }
                return 1;
            })));
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isEmpty() || !playerNinjaMap.containsKey(player.getUuid())) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }
            String ninja = playerNinjaMap.get(player.getUuid()).toLowerCase();
            Vec3d pos = player.getPos(); Box box = new Box(pos.add(-5, -2, -5), pos.add(5, 2, 5));

            if (ninja.equals("kai")) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0));
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.0f, 1.2f);
                for (Entity t : world.getOtherEntities(player, box)) { if (t instanceof LivingEntity) t.setOnFireFor(5); }
            }
            if (ninja.equals("jay")) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 2));
                LightningEntity l = EntityType.LIGHTNING_BOLT.create(world);
                if (l != null) { l.refreshPositionAfterTeleport(player.getX() + 5, player.getY(), player.getZ()); world.spawnEntity(l); }
            }
            if (ninja.equals("cole")) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 150, 3));
                for (Entity t : world.getOtherEntities(player, box)) { if (t instanceof LivingEntity) { t.damage(world.getDamageSources().magic(), 6.0f); t.setVelocity(t.getVelocity().add(0, 0.9, 0)); } }
            }
            if (ninja.equals("nya")) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 300, 0));
                for (Entity t : world.getOtherEntities(player, box)) { if (t instanceof LivingEntity) { Vec3d p = t.getPos().subtract(player.getPos()).normalize().multiply(1.8); t.setVelocity(p.x, 0.4, p.z); } }
            }
            if (ninja.equals("zane")) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
                for (Entity t : world.getOtherEntities(player, box)) { if (t instanceof LivingEntity) ((LivingEntity)t).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 9)); }
            }
            if (ninja.equals("morro")) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PHANTOM_BITE, SoundCategory.PLAYERS, 1.5f, 0.8f);
                for (Entity t : world.getOtherEntities(player, box)) { if (t instanceof LivingEntity) t.setVelocity(0, 2.2, 0); }
            }
            return TypedActionResult.success(player.getStackInHand(hand));
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!playerNinjaMap.containsKey(player.getUuid())) continue;
                String ninja = playerNinjaMap.get(player.getUuid()).toLowerCase();
                if (ninja.equals("morro")) player.fallDistance = 0;
                if (ninja.equals("zane") && player.isSneaking()) {
                    BlockPos bp = player.getBlockPos().down();
                    if (player.getServerWorld().getBlockState(bp).isOf(Blocks.WATER)) player.getServerWorld().setBlockState(bp, Blocks.FROSTED_ICE.getDefaultState());
                }
            }
        });
    }
}
