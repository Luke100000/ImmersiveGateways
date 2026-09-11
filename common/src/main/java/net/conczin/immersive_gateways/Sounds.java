package net.conczin.immersive_gateways;

import net.minecraft.sounds.SoundEvent;

public interface Sounds {
    SoundEvent ASSEMBLE = register("assemble");
    SoundEvent DISASSEMBLE = register("disassemble");
    SoundEvent GATEWAY = register("gateway");

    private static SoundEvent register(String id) {
        return SoundEvent.createVariableRangeEvent(Common.locate(id));
    }

    static void registerSounds(Common.RegisterHelper<SoundEvent> helper) {
        helper.register(ASSEMBLE.location(), ASSEMBLE);
        helper.register(DISASSEMBLE.location(), DISASSEMBLE);
        helper.register(GATEWAY.location(), GATEWAY);
    }
}
