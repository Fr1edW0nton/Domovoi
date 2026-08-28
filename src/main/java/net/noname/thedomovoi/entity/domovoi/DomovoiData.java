package net.noname.thedomovoi.entity.domovoi;

import net.minecraft.core.UUIDUtil;
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



    public void setDomovoiUUID( UUID pUUID ) { this.domovoiUUID = pUUID; }
    public UUID getDomovoiUUID() { return this.domovoiUUID; }

    public void setRespect( float respect ) { this.respect = respect; }
    public float getRespect() { return this.respect; }

    public void setComfort( float comfort ) { this.comfort = comfort; }
    public float getComfort() { return this.comfort; }
}
