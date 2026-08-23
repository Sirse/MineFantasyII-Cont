package minefantasy.mf2.client.model.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Vec3;

/**
 * Kitchen bench geometry ported 1:1 from the 1.12 fork's Blockbench model, including its per-face UV mapping onto the
 * original 64x64 texture.
 *
 * Built on raw TexturedQuads because ModelRenderer boxes only support the standard auto-generated UV layout, while the
 * source model uses arbitrary per-face regions of a shared atlas.
 */
public class ModelKitchenBench {

    private static final double TEX_SIZE = 64D;

    private final List<Box> boxes = new ArrayList<Box>();

    private Box currentBox;

    public ModelKitchenBench() {
        makeBox(0F, 12F, 0F, 16F, 16F, 16F);
        face(0, 0, 4, 4, 5);
        face(1, 8, 4, 12, 5);
        face(2, 12, 0, 8, 4);
        face(3, 16, 16, 12, 12);
        face(4, 4, 4, 8, 5);
        face(5, 12, 4, 16, 5);
        makeBox(1.5F, 3F, 1.5F, 14.5F, 5F, 14.5F);
        face(0, 0, 8.25, 3.25, 8.75);
        face(1, 6.5, 8.25, 9.75, 8.75);
        face(2, 9.75, 5, 6.5, 8.25);
        face(3, 6.5, 8.25, 3.25, 5);
        face(4, 3.25, 8.25, 6.5, 8.75);
        face(5, 9.75, 8.25, 13, 8.75);
        makeBox(11F, 0F, 1F, 15F, 12F, 5F);
        face(0, 3, 1, 2, 4);
        face(1, 1, 1, 0, 4);
        face(2, 2, 0, 3, 1);
        face(3, 1, 1, 2, 0);
        face(4, 2, 1, 1, 4);
        face(5, 4, 1, 3, 4);
        makeBox(1F, 0F, 1F, 5F, 12F, 5F);
        face(0, 3, 1, 2, 4);
        face(1, 1, 1, 0, 4);
        face(2, 2, 0, 3, 1);
        face(3, 1, 1, 2, 0);
        face(4, 2, 1, 1, 4);
        face(5, 4, 1, 3, 4);
        makeBox(11F, 0F, 11F, 15F, 12F, 15F);
        face(0, 3, 1, 2, 4);
        face(1, 1, 1, 0, 4);
        face(2, 2, 0, 3, 1);
        face(3, 1, 1, 2, 0);
        face(4, 2, 1, 1, 4);
        face(5, 4, 1, 3, 4);
        makeBox(1F, 0F, 11F, 5F, 12F, 15F);
        face(0, 0, 1, 1, 4);
        face(1, 2, 1, 3, 4);
        face(2, 3, 0, 2, 1);
        face(3, 2, 1, 1, 0);
        face(4, 1, 1, 2, 4);
        face(5, 3, 1, 4, 4);
        makeBox(6F, 5F, 5F, 10F, 9F, 9F);
        face(0, 0, 9.75, 1, 10.75);
        face(1, 2, 9.75, 3, 10.75);
        face(2, 3, 8.75, 2, 9.75);
        face(3, 2, 9.75, 1, 8.75);
        face(4, 1, 9.75, 2, 10.75);
        face(5, 3, 9.75, 4, 10.75);
        makeBox(1.75F, 15.5F, 2.5F, 2.75F, 16.499F, 7.5F);
        face(0, 0, 12.25, 1.5, 12.5);
        face(1, 2, 12.25, 3.25, 12.5);
        face(2, 2, 11, 1.75, 12.5);
        face(3, 1.75, 12.5, 1.5, 11);
        face(4, 1.5, 12.5, 1.75, 12.75);
        face(5, 1.75, 12.5, 2, 12.75);
        makeBox(14F, 15.5F, 9F, 17F, 16.5F, 10F);
        face(0, 0.25, 14, 0.5, 14.25);
        face(1, 1.25, 14, 1.5, 14.25);
        face(2, 2, 13.75, 1.25, 14);
        face(3, 1.25, 14, 0.5, 13.75);
        face(4, 0.5, 14, 1.25, 14.25);
        face(5, 1.5, 14, 2.25, 14.25);
        makeBox(10F, 15.5F, 9F, 14F, 16.5F, 11F);
        face(0, 0, 13.5, 0.5, 13.75);
        face(1, 1.5, 13.5, 2, 13.75);
        face(2, 2.5, 13, 1.5, 13.5);
        face(3, 1.5, 13.5, 0.5, 13);
        face(4, 0.5, 13.5, 1.5, 13.75);
        face(5, 2, 13.5, 3, 13.75);
        makeBox(7.2748F, 15.5F, 12.32685F, 8.2748F, 16.499F, 14.32685F);
        face(0, 1.25, 15, 1.5, 15);
        face(1, 0.75, 15, 1, 15);
        face(2, 1.5, 14.75, 1.25, 15);
        face(3, 1.25, 15, 1, 14.75);
        face(4, 1.5, 15, 1.75, 15);
        face(5, 1, 15, 1.25, 15);
        makeBox(1.25F, 15.5F, 7.5F, 3.25F, 16.5F, 9.5F);
        face(0, 2.25, 11.75, 2.75, 12);
        face(1, 3.25, 11.75, 3.75, 12);
        face(2, 3.75, 11.25, 3.25, 11.75);
        face(3, 3.25, 11.75, 2.75, 11.25);
        face(4, 2.75, 11.75, 3.25, 12);
        face(5, 3.75, 11.75, 4.25, 12);
        build();
    }

    public void makeBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        // JSON space is y-up pixels from the ground; the TESR pipeline translates
        // +1.25 blocks and flips Y, so model_y = 20 - json_y keeps geometry grounded
        currentBox = new Box(x1 - 8F, 20F - y2, z1 - 8F, x2 - 8F, 20F - y1, z2 - 8F);
        boxes.add(currentBox);
    }

    /**
     * quad indices: 0=+X east, 1=-X west, 2=-Y down, 3=+Y up, 4=-Z north, 5=+Z south. UVs are raw Blockbench values:
     * u1>u2 mirrors horizontally, v1>v2 mirrors vertically.
     */
    public void face(int quad, double a, double b, double c, double d) {
        double uL = Math.min(a, c);
        double uR = Math.max(a, c);
        double vT = Math.min(b, d);
        double vB = Math.max(b, d);
        boolean flipH = a > c;
        boolean flipV = b > d;

        // corner order in a TexturedQuad: 0=top-right, 1=top-left, 2=bottom-left, 3=bottom-right
        double[] xs = { uR, uL, uL, uR };
        double[] ys = { vT, vT, vB, vB };
        if (flipH) {
            double t = xs[0];
            xs[0] = xs[1];
            xs[1] = t;
            t = xs[3];
            xs[3] = xs[2];
            xs[2] = t;
        }
        if (flipV) {
            double t = ys[0];
            ys[0] = ys[3];
            ys[3] = t;
            t = ys[1];
            ys[1] = ys[2];
            ys[2] = t;
        }

        double[][] uv = new double[4][2];
        for (int i = 0; i < 4; i++) {
            uv[i][0] = xs[i] / TEX_SIZE;
            uv[i][1] = ys[i] / TEX_SIZE;
        }
        currentBox.uvs[quad] = new double[] { uv[0][0], uv[0][1], uv[1][0], uv[1][1], uv[2][0], uv[2][1], uv[3][0],
                uv[3][1] };
    }

    public void renderModel(float scale) {
        Tessellator tessellator = Tessellator.instance;
        for (Box box : boxes) {
            for (int i = 0; i < 6; i++) {
                if (box.quads[i] != null) {
                    box.quads[i].draw(tessellator, scale);
                }
            }
        }
    }

    public void build() {
        for (Box box : boxes) {
            box.build();
        }
    }

    private static class Box {

        final TexturedQuad[] quads = new TexturedQuad[6];
        final double[][] uvs = new double[6][8];

        final float x1;
        final float y1;
        final float z1;
        final float x2;
        final float y2;
        final float z2;

        Box(float x1, float y1, float z1, float x2, float y2, float z2) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
        }

        void build() {
            // corners follow the vanilla ModelBox vertex layout
            Vec3[] v = new Vec3[8];
            v[0] = Vec3.createVectorHelper(x1, y1, z1);
            v[1] = Vec3.createVectorHelper(x2, y1, z1);
            v[2] = Vec3.createVectorHelper(x2, y2, z1);
            v[3] = Vec3.createVectorHelper(x1, y2, z1);
            v[4] = Vec3.createVectorHelper(x1, y1, z2);
            v[5] = Vec3.createVectorHelper(x2, y1, z2);
            v[6] = Vec3.createVectorHelper(x2, y2, z2);
            v[7] = Vec3.createVectorHelper(x1, y2, z2);

            quads[0] = makeQuad(new Vec3[] { v[5], v[1], v[2], v[6] }, uvs[0]);
            quads[1] = makeQuad(new Vec3[] { v[0], v[4], v[7], v[3] }, uvs[1]);
            quads[2] = makeQuad(new Vec3[] { v[5], v[4], v[0], v[1] }, uvs[2]);
            quads[3] = makeQuad(new Vec3[] { v[2], v[3], v[7], v[6] }, uvs[3]);
            quads[4] = makeQuad(new Vec3[] { v[1], v[0], v[3], v[2] }, uvs[4]);
            quads[5] = makeQuad(new Vec3[] { v[4], v[5], v[6], v[7] }, uvs[5]);
        }

        private static TexturedQuad makeQuad(Vec3[] corners, double[] uv8) {
            net.minecraft.client.model.PositionTextureVertex[] verts = new net.minecraft.client.model.PositionTextureVertex[4];
            for (int i = 0; i < 4; i++) {
                verts[i] = new net.minecraft.client.model.PositionTextureVertex(
                        corners[i],
                        (float) uv8[i * 2],
                        (float) uv8[i * 2 + 1]);
            }
            return new TexturedQuad(verts);
        }
    }
}
