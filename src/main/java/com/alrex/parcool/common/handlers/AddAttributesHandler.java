package com.alrex.parcool.common.handlers;


import com.alrex.parcool.api.Attributes;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

public class AddAttributesHandler {
    @SubscribeEvent
    public static void onAddAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityTypes.PLAYER, Attributes.MAX_STAMINA);
        event.add(EntityTypes.PLAYER, Attributes.STAMINA_RECOVERY);
    }
}
