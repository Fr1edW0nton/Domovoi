package net.noname.thedomovoi.entity.domovoi.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.noname.thedomovoi.TheDomovoi;
import net.noname.thedomovoi.entity.domovoi.Domovoi;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class DomovoiModel extends DefaultedGeoModel<Domovoi> {

    public static final DataTicket<Boolean> IS_SLEEPING
            = DataTicket.create( "is_sleeping", Boolean.class );

    private static final Identifier DOMOVOI_AWAKE = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/domovoi/domovoi_awake.png"
    );
    private static final Identifier DOMOVOI_SLEEPING = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/domovoi/domovoi_asleep.png"
    );

    public DomovoiModel( Identifier identifier ) { super( identifier ); }



    @Override
    protected @NonNull String subtype() {
        return "entity";
    }



    @Override
    public void addAdditionalStateData(
            @NonNull Domovoi animatable,
            @Nullable Object relatedObject,
            @NonNull GeoRenderState renderState
    ) {
        renderState.addGeckolibData( IS_SLEEPING, animatable.isSleeping() );
    }



    @Override
    public @NonNull Identifier getTextureResource( @NonNull GeoRenderState renderState ) {
        boolean isSleeping = Objects.requireNonNullElse( renderState.getGeckolibData( IS_SLEEPING ), false );

        return isSleeping ? DOMOVOI_SLEEPING : DOMOVOI_AWAKE;
    }
}
