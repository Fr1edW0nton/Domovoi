package net.noname.thedomovoi.block.hearth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.noname.thedomovoi.TheDomovoi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DomovoiHearthSavedData extends SavedData {

    public static final SavedDataType<DomovoiHearthSavedData> DOMOVOI_HEARTH_DATA_ID = new SavedDataType<>(
            Identifier.fromNamespaceAndPath( TheDomovoi.MOD_ID, "domovoi_hearths" ),
            DomovoiHearthSavedData::new,
            RecordCodecBuilder.create( instance -> instance.group(
                    Codec.unboundedMap(
                            UUIDUtil.STRING_CODEC,
                            BlockPos.CODEC
                    ).fieldOf( "hearths" ).forGetter( data -> data.hearths )
            ).apply( instance, DomovoiHearthSavedData::new ) )
    );

    private Map<UUID, BlockPos> hearths = new HashMap<>();

    public DomovoiHearthSavedData() { this.hearths = new HashMap<>(); }
    public DomovoiHearthSavedData(Map<UUID, BlockPos> hearths ) { this.hearths = new HashMap<>( hearths ); }



    public static DomovoiHearthSavedData get(ServerLevel pServerLevel )
    { return pServerLevel.getDataStorage().computeIfAbsent( DOMOVOI_HEARTH_DATA_ID ); }



    public BlockPos getHearth( UUID playerUUID ) { return hearths.get( playerUUID ); }

    public void setHearth( UUID playerUUID, BlockPos pos ) { hearths.put( playerUUID, pos ); setDirty(); }

    public void removeHearth( UUID playerUUID ) { hearths.remove( playerUUID ); setDirty(); }

    public boolean hasHearth( ServerLevel pLevel, UUID playerUUID ) {
        BlockPos blockPos = this.hearths.get( playerUUID );
        if ( blockPos == null ) { return false; }

        if ( pLevel.getBlockState( blockPos ).getBlock() instanceof DomovoiHearthBlock ) { return true; }

        this.removeHearth( playerUUID );
        setDirty();

        return false;
    }
}
