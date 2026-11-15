package com.alrex.parcool.client.renderer;

import com.alrex.parcool.ParCool;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

public class RenderPipelines {
    private static final RenderPipeline.Snippet ZIPLINE_SNIPPET = RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.MATRICES_FOG_SNIPPET)
        .withVertexShader("core/rendertype_leash")
        .withFragmentShader("core/rendertype_leash")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        // 1.21.x builders depth-tested by default; 26.x needs it explicit (vanilla LEASH does the same)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .buildSnippet();

    public static final RenderPipeline ZIPLINE_3D = RenderPipeline.builder(ZIPLINE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ParCool.MOD_ID, "zipline3d"))
        .withCull(true)
        .build();
    public static final RenderPipeline ZIPLINE_2D = RenderPipeline.builder(ZIPLINE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ParCool.MOD_ID, "zipline2d"))
        .withCull(false)
        .build();
}
