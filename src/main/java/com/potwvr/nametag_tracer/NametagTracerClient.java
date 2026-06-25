package com.potwvr.nametag_tracer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import static net.minecraft.client.render.GameRenderer.getPositionColorShader;

@Environment(EnvType.CLIENT)
public class NametagTracerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;

            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            var camera = context.camera();
            var camPos = camera.getPos();
            var matrices = context.matrixStack();

            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof LivingEntity living && entity != client.player) {
                    if (living.hasCustomName()) {
                        renderTracerAndBox(matrices, living, camPos);
                    }
                }
            }

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        });
    }

    private void renderTracerAndBox(MatrixStack matrices, LivingEntity entity, Vec3d camPos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        var tessellator = net.minecraft.client.render.Tessellator.getInstance();
        var buffer = tessellator.getBuffer();

        matrices.push();

        // Get entity position (center)
        var entityPos = entity.getCameraPosVec(1.0f);
        var relativePos = entityPos.subtract(camPos);

        // Get crosshair position (from player camera)
        var playerEyePos = client.player.getCameraPosVec(1.0f).subtract(camPos);

        // Render tracer line
        matrices.push();
        var matrix = matrices.peek().getPositionMatrix();
        buffer.begin(net.minecraft.client.render.VertexFormat.DrawMode.LINE_STRIP, net.minecraft.client.render.VertexFormats.POSITION_COLOR);

        // Line from player to entity
        buffer.vertex(matrix, (float) playerEyePos.x, (float) playerEyePos.y, (float) playerEyePos.z)
                .color(255, 0, 0, 255)
                .next();
        buffer.vertex(matrix, (float) relativePos.x, (float) relativePos.y, (float) relativePos.z)
                .color(255, 0, 0, 255)
                .next();

        tessellator.draw();
        matrices.pop();

        // Render hitbox
        var box = entity.getBoundingBox().offset(-camPos.x, -camPos.y, -camPos.z);
        renderBox(matrices, box, 255, 0, 0, 255);

        matrices.pop();
    }

    private void renderBox(MatrixStack matrices, Box box, int r, int g, int b, int a) {
        var tessellator = net.minecraft.client.render.Tessellator.getInstance();
        var buffer = tessellator.getBuffer();
        var matrix = matrices.peek().getPositionMatrix();

        buffer.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        // Bottom face
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();

        // Top face
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();

        // Front face
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();

        // Back face
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();

        // Left face
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();

        // Right face
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();

        tessellator.draw();
    }
}
