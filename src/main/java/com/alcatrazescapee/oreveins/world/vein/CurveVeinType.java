/*
 * Part of the Realistic Ore Veins Mod by AlcatrazEscapee
 * Work under Copyright. See the project LICENSE.md for details.
 */

package com.alcatrazescapee.oreveins.world.vein;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import static com.alcatrazescapee.oreveins.world.vein.CurveVeinType.VeinCurve;

public class CurveVeinType extends SingleVeinType<VeinCurve>
{
    private final float radius;
    private final float angle;

    public CurveVeinType(JsonObject obj, JsonDeserializationContext context) throws JsonParseException
    {
        super(obj, context);
        radius = JSONUtils.getAsFloat(obj, "radius", 5);
        if (radius <= 0)
        {
            throw new JsonParseException("Radius must be > 0");
        }
        angle = JSONUtils.getAsFloat(obj, "angle", 45f);
        if (angle < 0 || angle > 360)
        {
            throw new JsonParseException("Angle must be >= 0 and <= 360");
        }
    }

    @Override
    public boolean inRange(VeinCurve vein, int xOffset, int zOffset)
    {
        return (xOffset < horizontalSize) && (zOffset < horizontalSize);
    }

    @Override
    public float getChanceToGenerate(VeinCurve vein, BlockPos pos)
    {
        for (CurveSegment segment : vein.getSegmentList())
        {
            Vector3d blockPos = new Vector3d(pos.getX(), pos.getY(), pos.getZ());
            Vector3d centeredPos = blockPos.subtract(segment.begin);

            // rotate block pos around Y axis
            double yaw = segment.yaw;
            Vector3d posX = new Vector3d(Math.cos(yaw) * centeredPos.x + Math.sin(yaw) * centeredPos.z, centeredPos.y, -Math.sin(yaw) * centeredPos.x + Math.cos(yaw) * centeredPos.z);

            // rotate block pos around Z axis
            double pitch = segment.pitch;
            Vector3d posY = new Vector3d(Math.cos(pitch) * posX.x - Math.sin(pitch) * posX.y, Math.sin(pitch) * posX.x + Math.cos(pitch) * posX.y, posX.z);

            double rad = Math.sqrt(posY.x * posY.x + posY.z * posY.z);
            double length = segment.length;

            if (((posY.y >= 0 && posY.y <= length) || (posY.y < 0 && posY.y >= length)) && rad < this.radius)
            {
                return 0.005f * density * (1f - 0.9f * (float) rad / this.radius);
            }
        }
        return 0.0f;
    }

    @Override
    public VeinCurve createVein(int chunkX, int chunkZ, Random rand)
    {
        int maxOffY = getMaxY() - getMinY() - verticalSize;
        int posY = getMinY() + verticalSize / 2 + ((maxOffY > 0) ? rand.nextInt(maxOffY) : 0);
        BlockPos pos = new BlockPos(chunkX * 16 + rand.nextInt(16), posY, chunkZ * 16 + rand.nextInt(16));
        return new VeinCurve(this, pos, rand);
    }

    static class VeinCurve extends Vein<CurveVeinType>
    {
        private final Random rand;
        private final List<CurveSegment> segmentList;
        private boolean isInitialized = false;

        VeinCurve(CurveVeinType type, BlockPos pos, Random random)
        {
            super(type, pos);
            this.rand = new Random(random.nextLong());
            this.segmentList = new ArrayList<>();
        }

        @Override
        public boolean inRange(int x, int z)
        {
            return getType().inRange(this, getPos().getX() - x, getPos().getZ() - z);
        }

        @Override
        public float getChanceToGenerate(BlockPos pos)
        {
            if (!isInitialized)
            {
                initialize(getType().horizontalSize, getType().verticalSize, getType().angle);
            }
            return getType().getChanceToGenerate(this, pos);
        }

        List<CurveSegment> getSegmentList()
        {
            return segmentList;
        }

        private Vector3d getRandomPointInCuboid(Random rand, Vector3d bottomLeft, Vector3d topRight)
        {
            final double x = bottomLeft.x + ((topRight.x - bottomLeft.x) * rand.nextDouble());
            final double y = bottomLeft.y + ((topRight.y - bottomLeft.y) * rand.nextDouble());
            final double z = bottomLeft.z + ((topRight.z - bottomLeft.z) * rand.nextDouble());

            return new Vector3d(x, y, z);
        }

        private void initialize(int hSize, int vSize, float angle)
        {
            double kxy = Math.tan(angle * (1.0f - 2.0f * rand.nextFloat()));
            double kyz = Math.tan(angle * (1.0f - 2.0f * rand.nextFloat()));

            final double h2Size = hSize / 2d;
            final double v2Size = vSize / 2d;

            // four points for cubic Bezier curve
            // p1 and p4 placed on (hSize; hSize; vSize) box with center in vein position
            Vector3d p1, p2, p3, p4;
            double x1, y1, z1, x2, y2, z2;

            if (v2Size >= h2Size * Math.abs(kyz))
            {
                z1 = -h2Size;
                y1 = h2Size * kyz;
            }
            else
            {
                z1 = -v2Size * Math.abs(kyz);
                y1 = v2Size * Math.signum(kyz);
            }

            x1 = (1 >= Math.abs(kxy)) ? h2Size : h2Size * kxy;

            x2 = -x1;
            y2 = -y1;
            z2 = -z1;

            p1 = new Vector3d(x1 + getPos().getX(), y1 + getPos().getY(), z1 + getPos().getZ());
            p4 = new Vector3d(x2 + getPos().getX(), y2 + getPos().getY(), z2 + getPos().getZ());

            Vector3d bottomLeft = new Vector3d(Math.min(p1.x, p4.x), Math.min(p1.y, p4.y), Math.min(p1.z, p4.z));
            Vector3d topRight = new Vector3d(Math.max(p1.x, p4.x), Math.max(p1.y, p4.y), Math.max(p1.z, p4.z));

            p2 = getRandomPointInCuboid(rand, bottomLeft, topRight);
            p3 = getRandomPointInCuboid(rand, bottomLeft, topRight);

            // curve segmentation setup
            double step = 5.0 / h2Size;
            double t = 0.0;
            Vector3d pb, pe = new Vector3d(0.0, 0.0, 0.0);

            // curve segmentation
            while (t < 1.0)
            {
                pb = (t == 0.0) ? p1 : pe;

                t += step;
                if (t > 1.0) t = 1.0;

                double t11 = 1 - t;
                double t12 = t11 * t11;
                double t13 = t12 * t11;

                double t31 = 3 * t;
                double t32 = 3 * t * t;
                double t3 = t * t * t;

                pe = p1.scale(t13).add(p2.scale(t31 * t12).add(p3.scale(t32 * t11).add(p4.scale(t3))));

                Vector3d axis = pe.subtract(pb);

                // align segment axis with axis X
                double yaw = Math.atan(axis.z / axis.x);
                Vector3d axisX = new Vector3d(Math.cos(yaw) * axis.x + Math.sin(yaw) * axis.z,
                        axis.y,
                        -Math.sin(yaw) * axis.x + Math.cos(yaw) * axis.z);

                // align segment axis with axis Y
                double pitch = Math.atan(axisX.x / axisX.y);
                Vector3d axisY = new Vector3d(Math.cos(pitch) * axisX.x - Math.sin(pitch) * axisX.y,
                        Math.sin(pitch) * axisX.x + Math.cos(pitch) * axisX.y,
                        axisX.z);

                segmentList.add(new CurveSegment(pb, axisY.y, yaw, pitch));
            }

            isInitialized = true;
        }
    }

    private static class CurveSegment
    {
        final Vector3d begin;
        final double length;
        final double yaw;
        final double pitch;

        CurveSegment(Vector3d begin, double length, double yaw, double pitch)
        {
            this.begin = begin;
            this.length = length;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
