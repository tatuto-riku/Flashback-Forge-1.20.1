package com.moulberry.flashback.editor.ui;

import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.impl.CameraKeyframe;
import com.moulberry.flashback.keyframe.impl.CameraOrbitKeyframe;
import net.minecraft.util.Mth;
import org.joml.Vector3d;

/** Applies the relative camera-copy transform introduced in Flashback 0.39.7. */
public class KeyframeRelativeOffsets {

    public Vector3d oldOrigin = new Vector3d();
    public Vector3d newOrigin = new Vector3d();
    public float oldYaw;
    public float newYaw;
    public float oldPitch;
    public float newPitch;

    public void apply(Keyframe keyframe) {
        if (keyframe instanceof CameraKeyframe cameraKeyframe) {
            Vector3d position = cameraKeyframe.position();
            transform(position);
            cameraKeyframe.yaw(cameraKeyframe.yaw() + Mth.wrapDegrees(this.newYaw - this.oldYaw));
            cameraKeyframe.pitch(cameraKeyframe.pitch() + Mth.wrapDegrees(this.newPitch - this.oldPitch));
        } else if (keyframe instanceof CameraOrbitKeyframe cameraOrbitKeyframe) {
            Vector3d center = cameraOrbitKeyframe.center;
            transform(center);

            if (this.newPitch != this.oldPitch) {
                float oldPitchRadians = (float) Math.toRadians(cameraOrbitKeyframe.pitch);
                float oldYawRadians = (float) Math.toRadians(-cameraOrbitKeyframe.yaw);
                float oldCosYaw = Mth.cos(oldYawRadians);
                float oldSinYaw = Mth.sin(oldYawRadians);
                float oldCosPitch = Mth.cos(oldPitchRadians);
                float oldSinPitch = Mth.sin(oldPitchRadians);
                Vector3d oldLook = new Vector3d(oldSinYaw * oldCosPitch, -oldSinPitch, oldCosYaw * oldCosPitch);

                Vector3d camera = new Vector3d(cameraOrbitKeyframe.center)
                    .sub(oldLook.mul(cameraOrbitKeyframe.distance));
                transform(camera);

                double dx = center.x - camera.x;
                double dy = center.y - camera.y;
                double dz = center.z - camera.z;
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
                cameraOrbitKeyframe.pitch = Mth.wrapDegrees((float) (-(Mth.atan2(dy, horizontalDistance) * 180.0F / Math.PI)));
                cameraOrbitKeyframe.yaw = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * 180.0F / Math.PI) - 90.0F);
            } else if (this.newYaw != this.oldYaw) {
                cameraOrbitKeyframe.yaw += Mth.wrapDegrees(this.newYaw - this.oldYaw);
            }
        }
    }

    private void transform(Vector3d position) {
        position.sub(this.oldOrigin);
        position.rotateY(Math.toRadians(this.oldYaw));
        position.rotateX(Math.toRadians(this.newPitch - this.oldPitch));
        position.rotateY(Math.toRadians(-this.newYaw));
        position.add(this.newOrigin);
    }

}
