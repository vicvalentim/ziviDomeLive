package com.victorvalentim.zividomelive.core.projection;

/** Host-independent final-view projection identity. */
public enum ProjectionType {
    /** Conventional perspective view independent from spherical capture. */
    STANDARD,
    /** Circular fulldome projection. */
    DOMEMASTER,
    /** Latitude/longitude equirectangular projection. */
    EQUIRECTANGULAR,
    /** Equi-angular cubemap cross projection. */
    SKYBOX
}
