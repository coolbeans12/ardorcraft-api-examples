
package com.ardorcraft.player;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardorcraft.collision.IntersectionResult;
import com.ardorcraft.data.Pos;
import com.ardorcraft.world.BlockWorld;

/**
 * Player implementation with collision against the world
 */
public class PlayerWithCollision extends PlayerBase {
    protected final Vector3 oldLocation = new Vector3();
    protected final Vector3 currentLocation = new Vector3();
    protected final Vector3 moveDir = new Vector3();
    protected final Vector3 penetrationVec = new Vector3();
    protected final Vector3 normal = new Vector3();
    protected final Vector3 mult = new Vector3();

    protected final Vector3 testVec = new Vector3();
    protected final Vector3 testVecs[];

    protected boolean isOnGround = false;

    public PlayerWithCollision() {
        testVecs = new Vector3[12];
        testVecs[0] = new Vector3(-0.4, -1.5, -0.4);
        testVecs[1] = new Vector3(0.4, -1.5, -0.4);
        testVecs[2] = new Vector3(0.4, -1.5, 0.4);
        testVecs[3] = new Vector3(-0.4, -1.5, 0.4);
        testVecs[4] = new Vector3(-0.4, -0.6, -0.4);
        testVecs[5] = new Vector3(0.4, -0.6, -0.4);
        testVecs[6] = new Vector3(0.4, -0.6, 0.4);
        testVecs[7] = new Vector3(-0.4, -0.6, 0.4);
        testVecs[8] = new Vector3(-0.4, 0.3, -0.4);
        testVecs[9] = new Vector3(0.4, 0.3, -0.4);
        testVecs[10] = new Vector3(0.4, 0.3, 0.4);
        testVecs[11] = new Vector3(-0.4, 0.3, 0.4);
    }

    public boolean isPlayerSpace(final Pos pos) {
        final int X = (int) MathUtils.floor(position.getX());
        final int Z = (int) MathUtils.floor(position.getZ());

        if (testOnHeight(pos, X, (int) MathUtils.floor(position.getY() + 0.3), Z)) {
            return true;
        }
        if (testOnHeight(pos, X, (int) MathUtils.floor(position.getY() - 0.6), Z)) {
            return true;
        }
        if (testOnHeight(pos, X, (int) MathUtils.floor(position.getY() - 1.4), Z)) {
            return true;
        }
        return false;
    }

    private boolean testOnHeight(final Pos pos, final int X, final int Y, final int Z) {
        if (pos.x == X && pos.z == Z && pos.y == Y) {
            return true;
        }

        final double fractionX = position.getX() - X - 0.5;
        final double fractionZ = position.getZ() - Z - 0.5;

        int testX = 0;
        int testZ = 0;
        if (fractionX < -0.1) {
            testX = -1;
        } else if (fractionX > 0.1) {
            testX = 1;
        }
        if (fractionZ < -0.1) {
            testZ = -1;
        } else if (fractionZ > 0.1) {
            testZ = 1;
        }

        if (pos.x == X + testX && pos.z == Z && pos.y == Y) {
            return true;
        }
        if (pos.x == X && pos.z == Z + testZ && pos.y == Y) {
            return true;
        }
        if (pos.x == X + testX && pos.z == Z + testZ && pos.y == Y) {
            return true;
        }

        return false;
    }

    public void update(final BlockWorld blockScene, final ReadOnlyTimer timer) {
        currentLocation.set(position);
        moveDir.set(currentLocation).subtractLocal(oldLocation);
        double moveLength = moveDir.length();
        if (moveLength == 0) {
            return;
        }
        moveDir.normalizeLocal();

        isOnGround = false;

        final IntersectionResult bestIntersection = new IntersectionResult();
        final IntersectionResult intersectionResult = new IntersectionResult();
        bestIntersection.length = Double.MAX_VALUE;
        for (int i = 0; i < testVecs.length; i++) {
            testVec.set(oldLocation).addLocal(testVecs[i]);
            blockScene.traceCollision(testVec, moveDir, 10, intersectionResult);
            if (intersectionResult.hit && intersectionResult.length < moveLength) {
                if (intersectionResult.length < bestIntersection.length) {
                    bestIntersection.set(intersectionResult);
                }
            }
        }

        if (bestIntersection.length < moveLength) {
            mult.set(moveDir).multiplyLocal(bestIntersection.length).addLocal(oldLocation);

            for (int j = 0; j < 3; j++) {
                final double penetration = moveLength - bestIntersection.length;
                penetrationVec.set(moveDir).multiplyLocal(penetration);

                final int yDir = bestIntersection.oldPos.y - bestIntersection.pos.y;
                if (yDir != 0) {
                    velocity.setY(0);
                    if (yDir > 0) {
                        isOnGround = true;
                    }
                }
                final int xDir = bestIntersection.oldPos.x - bestIntersection.pos.x;
                if (xDir != 0) {
                    velocity.setX(0);
                }
                final int zDir = bestIntersection.oldPos.z - bestIntersection.pos.z;
                if (zDir != 0) {
                    velocity.setZ(0);
                }

                normal.set(bestIntersection.oldPos.x - bestIntersection.pos.x, bestIntersection.oldPos.y
                        - bestIntersection.pos.y, bestIntersection.oldPos.z - bestIntersection.pos.z);
                final double dot = penetrationVec.dot(normal);
                normal.multiplyLocal(dot);
                penetrationVec.subtractLocal(normal);

                moveDir.set(penetrationVec).normalizeLocal();
                bestIntersection.length = penetrationVec.length();
                moveLength = bestIntersection.length;
                for (int i = 0; i < testVecs.length; i++) {
                    testVec.set(mult).addLocal(testVecs[i]);
                    blockScene.traceCollision(testVec, moveDir, 10, intersectionResult);
                    if (intersectionResult.hit && intersectionResult.length < moveLength) {
                        if (intersectionResult.length < bestIntersection.length) {
                            bestIntersection.set(intersectionResult);
                        }
                    }
                }
                if (!bestIntersection.hit) {
                    break;
                }

                moveDir.multiplyLocal(bestIntersection.length);
                mult.addLocal(moveDir);
                moveDir.set(penetrationVec).normalizeLocal();
            }

            currentLocation.set(mult);
        }

        if (currentLocation.getY() < 0.5) {
            currentLocation.setY(0.5);
        }

        position.set(currentLocation);
        oldLocation.set(position);
    }

    public boolean isOnGround() {
        return isOnGround;
    }
}
