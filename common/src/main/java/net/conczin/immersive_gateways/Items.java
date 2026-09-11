package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.item.GatewayItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public interface Items {
    Item GATEWAY = new GatewayItem(baseProps());

    static Item.Properties baseProps() {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Common.locate("gateway")));
    }

    static void registerItems(Common.RegisterHelper<Item> helper) {
        helper.register(Common.locate("gateway"), GATEWAY);
    }
}
