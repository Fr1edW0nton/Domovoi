package net.noname.thedomovoi.entity.domovoi;

import net.minecraft.core.UUIDUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class DomovoiData {
    private UUID domovoiUUID;

    private float respect;
    private float comfort;

    public DomovoiData() {}



    public void saveData( ValueOutput pOutput ) {
        if ( this.domovoiUUID != null ) {
            pOutput.store( "domovoi_uuid", UUIDUtil.CODEC, this.getDomovoiUUID() );
        }

        pOutput.putFloat( "respect", this.getRespect() );
        pOutput.putFloat( "comfort", this.getComfort() );
    }


    public UUID getDomovoiUUID() { return this.domovoiUUID; }
    public void setDomovoiUUID( UUID pUUID ) { this.domovoiUUID = pUUID; }

    public float getRespect() { return this.respect; }
    public void setRespect( float respect ) { this.respect = respect; }
    public void updateRespect( float amount )
    { this.respect = Mth.clamp( this.respect + amount, 0, 1.0F ); }

    public float getComfort() { return this.comfort; }
    public void setComfort( float comfort ) { this.comfort = comfort; }
    public void updateComfort( float amount )
    { this.comfort = Mth.clamp( this.comfort + amount, 0, 1.0F ); }
}
