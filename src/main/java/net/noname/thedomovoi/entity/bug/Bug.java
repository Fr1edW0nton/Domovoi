package net.noname.thedomovoi.entity.bug;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.noname.thedomovoi.block.hearth.DomovoiHearthBlockEntity;
import net.noname.thedomovoi.entity.ModEntitySounds;
import net.noname.thedomovoi.entity.moth.Moth;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public class Bug extends PathfinderMob implements GeoEntity {

    public static final EntityDataAccessor<Integer> VARIANT
            = SynchedEntityData.defineId( Bug.class, EntityDataSerializers.INT );

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache( this );

    public BlockPos hearth;

    public enum BugBehaviorState { WANDER, CONTEMPLATE_EXISTENCE }
    private BugBehaviorState bugBehaviorState;

    public Bug( EntityType<? extends PathfinderMob> type, Level level ) {
        super( type, level );

        this.setVariant( this.getRandom().nextInt( 0, 2 ) );

        this.setPathfindingMalus( PathType.FIRE, -1.0F );
        this.setPathfindingMalus( PathType.WATER, -1.0F );
    }



    @Override
    protected void defineSynchedData( SynchedEntityData.@NonNull Builder entityData ) {
        super.defineSynchedData( entityData );

        entityData.define( VARIANT, 0 );
    }


    @Override
    protected void addAdditionalSaveData( @NonNull ValueOutput output ) {
        super.addAdditionalSaveData( output );

        output.putInt( "variant", this.getVariant() );
        if ( this.hearth != null ) { output.storeNullable( "hearth_pos", BlockPos.CODEC, this.hearth ); }
    }

    @Override
    protected void readAdditionalSaveData( @NonNull ValueInput input ) {
        super.readAdditionalSaveData( input );

        int variant = input.getInt( "variant" ).orElse( 0 );
        this.setVariant( variant );

        this.hearth = input.read( "hearth_pos", BlockPos.CODEC ).orElse( null );
    }



    @Override
    public void registerControllers( AnimatableManager.ControllerRegistrar controllers ) {
        controllers.add( new AnimationController<>(
                state -> {
                    if ( state.isMoving() ) {
                        return state.setAndContinue(
                                RawAnimation.begin().thenLoop( "walk" )
                        );
                    }
                    return PlayState.STOP;
                }
        ) );
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal( 0, new BugWanderGoal() );
        this.goalSelector.addGoal( 0, new BugContemplateExistenceGoal() );
    }



    public int getVariant() { return this.entityData.get( VARIANT ); }
    public void setVariant( int pVariant) { this.entityData.set( VARIANT, pVariant); }

    @Override
    public @NonNull AnimatableInstanceCache getAnimatableInstanceCache() { return this.geoCache; }

    public boolean hasHearth() { return this.hearth != null; }
    public BlockPos getHearth() { return this.hearth; }
    public void setHearth( BlockPos hearth ) { this.hearth = hearth; }

    public BugBehaviorState getBugBehaviorState() { return this.bugBehaviorState; }
    public void setBugBehaviorState( BugBehaviorState pBehaviorState ) { this.bugBehaviorState = pBehaviorState; }




    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModEntitySounds.BUG_SOUND.value();
    }

    @Override
    public int getAmbientSoundInterval() {
        return this.random.nextInt( 100, 200 );
    }



    @Override
    public void die( @NonNull DamageSource source ) {
        super.die(source);

        if ( this.level() instanceof ServerLevel serverLevel ) {
            BlockPos blockPos = this.getHearth();

            if ( serverLevel.getBlockEntity( blockPos ) instanceof DomovoiHearthBlockEntity domovoiHearthBlockEntity) {
                domovoiHearthBlockEntity.removeMobDecay( this.getUUID() );
            }
        }
    }




    private class BugWanderGoal extends Goal {
        public BugWanderGoal() { this.setFlags( EnumSet.of( Flag.MOVE ) ); }



        private Vec3 findPos() {
            int wanderThreshold = 100;

            if ( Bug.this.hasHearth() ) {
                Vec3 hearthPos = Vec3.atCenterOf( Bug.this.getHearth() );

                double distance = Bug.this.position().distanceTo( hearthPos );

                if ( distance > wanderThreshold ) {
                    return DefaultRandomPos.getPosTowards(
                            Bug.this,
                            10,
                            7,
                            hearthPos,
                            Math.PI / 2
                    );
                }
            }

            return DefaultRandomPos.getPos( Bug.this, 10, 7 );
        }



        @Override
        public boolean canUse() { return Bug.this.navigation.isDone() && Bug.this.random.nextInt( 5 ) == 0; }

        @Override
        public boolean canContinueToUse() { return Bug.this.navigation.isInProgress(); }



        @Override
        public void start() {
            Vec3 targetPos = this.findPos();
            if ( targetPos != null ) {
                Bug.this.navigation.moveTo(
                        Bug.this.navigation.createPath(
                                BlockPos.containing( targetPos ),
                                1
                        ),
                        1.0
                );
            }

            Bug.this.setBugBehaviorState( BugBehaviorState.WANDER );
        }

        @Override
        public void stop() {
            Bug.this.setBugBehaviorState( null );
        }
    }

    private class BugContemplateExistenceGoal extends Goal {
        private int contemplationTicks;

        @Override
        public boolean canUse() {
            return Bug.this.navigation.isDone();
        }

        @Override
        public boolean canContinueToUse() {
            return this.contemplationTicks > 0;
        }



        @Override
        public void start() {
            this.contemplationTicks = Bug.this.random.nextInt( 60, 200 );

            Bug.this.setBugBehaviorState( BugBehaviorState.CONTEMPLATE_EXISTENCE );
        }

        @Override
        public void tick() {
            if ( this.contemplationTicks > 0 ) { this.contemplationTicks--; }
        }

        @Override
        public void stop() {
            Bug.this.setBugBehaviorState( null );
        }
    }
}
