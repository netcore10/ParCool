package com.alrex.parcool.client.renderer;

import net.minecraft.client.renderer.rendertype.RenderType;

public class RenderTypes {
    // ponytail: 26.2 rewrote the pipeline API; the old custom pipelines were copies of vanilla's leash,
    // so reuse it. Add a custom pipeline back if 3D/2D rope needs different culling.
    public static final RenderType ZIPLINE_3D = net.minecraft.client.renderer.rendertype.RenderTypes.leash();
    public static final RenderType ZIPLINE_2D = ZIPLINE_3D;
}
