package com.minelittlepony.unicopia;

import com.minelittlepony.unicopia.particle.Particles;
import com.minelittlepony.unicopia.particle.client.EntityMagicFX;
import com.minelittlepony.unicopia.particle.client.EntityRaindropFX;

public class UParticles {

    public static int MAGIC_PARTICLE;
    public static int RAIN_PARTICLE;

    static void init() {
        MAGIC_PARTICLE = Particles.instance().registerParticle(EntityMagicFX::new);
        RAIN_PARTICLE = Particles.instance().registerParticle(EntityRaindropFX::new);
    }
}
