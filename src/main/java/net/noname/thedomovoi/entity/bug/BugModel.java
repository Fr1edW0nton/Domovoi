package net.noname.thedomovoi.entity.bug;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.noname.thedomovoi.TheDomovoi;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class BugModel extends DefaultedGeoModel<Bug> {

    public static final DataTicket<Integer> BUG_VARIANT
            = DataTicket.create( "bug_variant", Integer.class );

    private static final Identifier BUG_BROWN = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/bug/bug_brown.png"
    );
    private static final Identifier BUG_GRAY = Identifier.fromNamespaceAndPath(
            TheDomovoi.MOD_ID,
            "textures/entity/bug/bug_gray.png"
    );

    public BugModel( Identifier identifier ) { super( identifier ); }



    @Override
    protected @NonNull String subtype() {
        return "entity";
    }


    @Override
    public void addAdditionalStateData(
            @NonNull Bug animatable,
            @Nullable Object relatedObject,
            @NonNull GeoRenderState renderState
    ) {
        renderState.addGeckolibData( BUG_VARIANT, animatable.getVariant() );
    }


    @Override
    public @NonNull Identifier getTextureResource( GeoRenderState renderState ) {
        int variant = Objects.requireNonNullElse( renderState.getGeckolibData( BUG_VARIANT ), 0 );

        return variant == 0 ? BUG_BROWN : BUG_GRAY;
    }
}
