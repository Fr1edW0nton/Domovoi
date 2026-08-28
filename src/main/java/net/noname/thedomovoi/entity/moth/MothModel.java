package net.noname.thedomovoi.entity.moth;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.noname.thedomovoi.TheDomovoi;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class MothModel extends DefaultedGeoModel<Moth> {

    public static final DataTicket<Integer> MOTH_VARIANT
            = DataTicket.create( "moth_variant", Integer.class );

    private static final Identifier MOTH_BROWN = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/moth/moth_brown.png"
    );
    private static final Identifier MOTH_GRAY = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/moth/moth_gray.png"
    );
    private static final Identifier MOTH_WHITE = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/moth/moth_white.png"
    );

    public MothModel( Identifier identifier ) { super( identifier ); }



    @Override
    protected @NonNull String subtype() { return "entity"; }


    @Override
    public void addAdditionalStateData(
            @NonNull Moth animatable,
            @Nullable Object relatedObject,
            @NonNull GeoRenderState renderState
    ) {
        renderState.addGeckolibData( MOTH_VARIANT, animatable.getVariant() );
    }


    @Override
    public @NonNull Identifier getTextureResource( @NonNull GeoRenderState renderState ) {
        int variant = Objects.requireNonNullElse( renderState.getGeckolibData( MOTH_VARIANT ), 0 );

        return switch ( variant ) {
            case 1 -> MOTH_GRAY;
            case 2 -> MOTH_WHITE;

            default -> MOTH_BROWN;
        };
    }
}
