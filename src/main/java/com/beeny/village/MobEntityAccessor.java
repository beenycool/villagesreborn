package com.beeny.village;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.entity.mob.MobEntity;

/**
 * Accessor interface for MobEntity to access protected/private methods.
 * This is used with the Mixin system to provide access to the setTarget method.
 */
@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    
    /**
     * Invokes the setTarget method from MobEntity
     * @param target The target entity
     */
    @Invoker("setTarget")
    void callSetTarget(LivingEntity target);
}
