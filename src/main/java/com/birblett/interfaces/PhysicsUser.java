package com.birblett.interfaces;

public interface PhysicsUser {

    void setProjectile(PhysicsProjectile p);
    boolean hasProjectile();
    PhysicsProjectile getProjectile();

}
