package net.ozanarchy.brooksclient.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.renderer.debug.GameTestBlockHighlightRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.ozanarchy.brooksclient.client.module.modules.ChestESPModule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

public final class ChestEspWorldRenderer {
    private final GameTestBlockHighlightRenderer highlights = new GameTestBlockHighlightRenderer();
    private final Map<BlockPos, Object> markerMap;
    private final Constructor<?> markerConstructor;

    @SuppressWarnings("unchecked")
    public ChestEspWorldRenderer() {
        Map<BlockPos, Object> resolvedMap = null;
        Constructor<?> resolvedCtor = null;
        try {
            Field markersField = GameTestBlockHighlightRenderer.class.getDeclaredField("markers");
            markersField.setAccessible(true);
            Object value = markersField.get(highlights);
            if (value instanceof Map<?, ?> map) {
                resolvedMap = (Map<BlockPos, Object>) map;
            }

            Class<?> markerClass = Class
                    .forName("net.minecraft.client.renderer.debug.GameTestBlockHighlightRenderer$Marker");
            resolvedCtor = markerClass.getDeclaredConstructor(int.class, String.class, long.class);
            resolvedCtor.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
        }
        markerMap = resolvedMap;
        markerConstructor = resolvedCtor;
    }

    public void update(Minecraft mc, ChestESPModule module) {
        clear();
        if (mc == null || mc.level == null || mc.player == null || module == null || !module.isEnabled()) {
            return;
        }

        double maxDistanceSq = module.getRange() * module.getRange();
        int playerChunkX = mc.player.chunkPosition().x();
        int playerChunkZ = mc.player.chunkPosition().z();
        int chunkRadius = Math.max(1, (int) Math.ceil(module.getRange() / 16.0D) + 1);

        ClientChunkCache chunkSource = mc.level.getChunkSource();
        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                LevelChunk chunk = chunkSource.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }

                Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
                for (BlockEntity blockEntity : blockEntities.values()) {
                    if (!shouldRender(blockEntity, module)) {
                        continue;
                    }

                    BlockPos pos = blockEntity.getBlockPos();
                    double dx = mc.player.getX() - (pos.getX() + 0.5D);
                    double dy = mc.player.getY() - (pos.getY() + 0.5D);
                    double dz = mc.player.getZ() - (pos.getZ() + 0.5D);
                    if ((dx * dx) + (dy * dy) + (dz * dz) > maxDistanceSq) {
                        continue;
                    }

                    int color = resolveColor(blockEntity, module);
                    if (!putSilentMarker(pos, color)) {
                        highlights.highlightPos(pos, null);
                    }
                }
            }
        }
    }

    public void render() {
        highlights.emitGizmos();
    }

    public void clear() {
        if (markerMap != null) {
            markerMap.clear();
        } else {
            highlights.clear();
        }
    }

    private boolean putSilentMarker(BlockPos pos, int color) {
        if (markerMap == null || markerConstructor == null || pos == null) {
            return false;
        }
        try {
            Object marker = markerConstructor.newInstance(color, "", System.currentTimeMillis() + 250L);
            markerMap.put(pos.immutable(), marker);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private boolean shouldRender(BlockEntity blockEntity, ChestESPModule module) {
        if (blockEntity instanceof TrappedChestBlockEntity) {
            return module.showTrappedChests();
        }
        if (blockEntity instanceof EnderChestBlockEntity) {
            return module.showEnderChests();
        }
        if (blockEntity instanceof ChestBlockEntity) {
            return module.showChests();
        }
        if (blockEntity instanceof BarrelBlockEntity) {
            return module.showBarrels();
        }
        if (blockEntity instanceof ShulkerBoxBlockEntity) {
            return module.showShulkers();
        }
        return false;
    }

    private int resolveColor(BlockEntity blockEntity, ChestESPModule module) {
    if (blockEntity instanceof TrappedChestBlockEntity) {
        return module.getTrappedChestColor();
    }
    if (blockEntity instanceof EnderChestBlockEntity) {
        return module.getEnderChestColor();
    }
    if (blockEntity instanceof ChestBlockEntity) {
        return module.getChestColor();
    }
    if (blockEntity instanceof BarrelBlockEntity) {
        return module.getBarrelColor();
    }
    if (blockEntity instanceof ShulkerBoxBlockEntity) {
        return module.getShulkerColor();
    }
    return 0x90FFFFFF;
}
}
