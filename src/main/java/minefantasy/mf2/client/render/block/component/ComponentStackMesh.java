package minefantasy.mf2.client.render.block.component;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The geometry of a placed component stack, made once for each kind and size and shared by every pile of it: the parts'
 * own turns and offsets are worked into the vertices, and all of them go into one batch in one display list. A pile
 * then costs one call a frame instead of a call, a translation and a turn for each of up to 64 parts. Texture, colour,
 * light and the pile's facing stay outside the list, so piles of any material share it.
 */
@SideOnly(Side.CLIENT)
final class ComponentStackMesh {

    /** The list of each kind of stack at each size, 0 where it is not made yet. */
    private final int[][] lists;

    /** For stacks of {@code kinds} kinds, each at most {@code maxSize} parts. */
    ComponentStackMesh(int kinds, int maxSize) {
        lists = new int[kinds][maxSize + 1];
    }

    /** Draws {@code count} parts of a kind of stack, compiling its list the first time. */
    void draw(int kind, ModelRenderer[] parts, int count, float scale) {
        int size = Math.max(0, Math.min(count, Math.min(parts.length, lists[kind].length - 1)));
        if (lists[kind][size] == 0) lists[kind][size] = compile(parts, size, scale);
        GL11.glCallList(lists[kind][size]);
    }

    /** Gives the display lists back to OpenGL; they are made again as piles are drawn. */
    void release() {
        for (int[] kind : lists) {
            for (int size = 0; size < kind.length; size++) {
                if (kind[size] != 0) GL11.glDeleteLists(kind[size], 1);
                kind[size] = 0;
            }
        }
    }

    private static int compile(ModelRenderer[] parts, int count, float scale) {
        int list = GL11.glGenLists(1);
        GL11.glNewList(list, GL11.GL_COMPILE);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        try {
            for (int i = 0; i < count; i++) {
                ModelRenderer part = parts[i];
                if (part == null || part.isHidden || !part.showModel) continue;
                for (Object box : part.cubeList) {
                    TexturedQuad[] quads = ReflectionHelper
                            .getPrivateValue(ModelBox.class, (ModelBox) box, "quadList", "field_78254_i");
                    for (TexturedQuad quad : quads) {
                        add(tessellator, part, quad, scale);
                    }
                }
            }
        } finally {
            tessellator.draw();
            GL11.glEndList();
        }
        return list;
    }

    /**
     * Adds one face, placed as {@link ModelRenderer#render} would place it: turned about X, then Y, then Z, then moved
     * to the part's pivot, its normal worked out as {@link TexturedQuad#draw} does.
     */
    private static void add(Tessellator tessellator, ModelRenderer part, TexturedQuad quad, float scale) {
        Vec3[] at = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            at[i] = place(part, quad.vertexPositions[i].vector3D, scale);
        }
        Vec3 a = at[1].subtract(at[0]);
        Vec3 b = at[1].subtract(at[2]);
        Vec3 normal = b.crossProduct(a).normalize();
        tessellator.setNormal((float) normal.xCoord, (float) normal.yCoord, (float) normal.zCoord);
        for (int i = 0; i < 4; i++) {
            PositionTextureVertex vertex = quad.vertexPositions[i];
            tessellator.addVertexWithUV(
                    at[i].xCoord,
                    at[i].yCoord,
                    at[i].zCoord,
                    vertex.texturePositionX,
                    vertex.texturePositionY);
        }
    }

    private static Vec3 place(ModelRenderer part, Vec3 vertex, float scale) {
        double x = (float) vertex.xCoord * scale, y = (float) vertex.yCoord * scale, z = (float) vertex.zCoord * scale;
        if (part.rotateAngleX != 0.0F) {
            double cos = Math.cos(part.rotateAngleX), sin = Math.sin(part.rotateAngleX);
            double ny = y * cos - z * sin, nz = y * sin + z * cos;
            y = ny;
            z = nz;
        }
        if (part.rotateAngleY != 0.0F) {
            double cos = Math.cos(part.rotateAngleY), sin = Math.sin(part.rotateAngleY);
            double nx = x * cos + z * sin, nz = -x * sin + z * cos;
            x = nx;
            z = nz;
        }
        if (part.rotateAngleZ != 0.0F) {
            double cos = Math.cos(part.rotateAngleZ), sin = Math.sin(part.rotateAngleZ);
            double nx = x * cos - y * sin, ny = x * sin + y * cos;
            x = nx;
            y = ny;
        }
        return Vec3.createVectorHelper(
                x + part.rotationPointX * scale + part.offsetX,
                y + part.rotationPointY * scale + part.offsetY,
                z + part.rotationPointZ * scale + part.offsetZ);
    }
}
