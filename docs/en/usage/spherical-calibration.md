# Spherical Calibration

![Spherical calibration](../../img/spherical-calibration.png)

The spherical controls solve different problems and should not be collapsed into one generic “zoom/orientation” concept.

## Pitch / Yaw / Roll

Pitch, Yaw and Roll orient the **shared spherical domain**. They affect spherical representations consistently.

They are not the Scene Camera.

## FOV

FOV is a Domemaster control. It defines the angular field represented by the fisheye output.

## Size%

Size% scales the physical Domemaster circle within its output target so an installation can match projector/lens geometry.

Size% is **not scene zoom**. To move through or reframe scene content, use the scene/camera model instead.

## Calibration workflow

1. choose Domemaster;
2. establish Pitch/Yaw/Roll orientation;
3. set the required Domemaster FOV;
4. adjust Size% to the optical/projector geometry;
5. verify against `CalibrationTool` on the target system;
6. preserve the resulting settings while changing output resolution unless the installation itself requires recalibration.
