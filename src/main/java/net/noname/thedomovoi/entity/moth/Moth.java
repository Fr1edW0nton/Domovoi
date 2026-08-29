package net.noname.thedomovoi.entity.moth;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.noname.thedomovoi.block.hearth.DomovoiHearthBlockEntity;
import net.noname.thedomovoi.entity.ModEntitySounds;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public class Moth extends PathfinderMob implements GeoEntity {

    public static final EntityDataAccessor<Integer> VARIANT
            = SynchedEntityData.defineId( Moth.class, EntityDataSerializers.INT );

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache( this );

    public BlockPos hearth;

    public enum MothBehaviorState { WANDER, HOVER }
    private MothBehaviorState mothBehaviorState;

    public Moth( EntityType<? extends PathfinderMob> type, Level level ) {
        super( type, level );

        this.setVariant( this.getRandom().nextInt( 0, 3 ) );

        this.moveControl = new FlyingMoveControl<Moth>( this, 20, true );
        this.lookControl = new LookControl( this );
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
    protected @NonNull PathNavigation createNavigation(@NonNull Level level ) {
        FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation( this, level );

        flyingPathNavigation.setCanOpenDoors( false );
        flyingPathNavigation.setCanFloat( false );
        flyingPathNavigation.setRequiredPathLength( 48.0F );

        return flyingPathNavigation;
    }


    @Override
    public void registerControllers( AnimatableManager.ControllerRegistrar controllers ) {
        controllers.add( new AnimationController<>(
                state -> state.setAndContinue(
                        RawAnimation.begin().thenLoop( "fly" )
                )
        ) );
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal( 0, new MothWanderGoal() );
        this.goalSelector.addGoal( 0, new MothHoverGoal() );
    }



    public int getVariant() { return this.entityData.get( VARIANT ); }
    public void setVariant( int pVariant) { this.entityData.set( VARIANT, pVariant); }

    @Override
    public @NonNull AnimatableInstanceCache getAnimatableInstanceCache() { return this.geoCache; }

    public boolean hasHearth() { return this.hearth != null; }
    public BlockPos getHearth() { return this.hearth; }
    public void setHearth( BlockPos hearth ) { this.hearth = hearth; }

    public MothBehaviorState getMothBehaviorState() { return this.mothBehaviorState; }
    public void setMothBehaviorState( MothBehaviorState pBehaviorState ) { this.mothBehaviorState = pBehaviorState; }



    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModEntitySounds.MOTH_SOUND.value();
    }

    @Override
    public int getAmbientSoundInterval() {
        return this.random.nextInt( 100, 200 );
    }



    private boolean closerThan(BlockPos targetPos, int distance ) {
        return targetPos.closerThan( this.blockPosition(), ( double )distance );
    }



    @Override
    public void die( @NonNull DamageSource source ) {
        super.die(source);

        if ( this.level() instanceof ServerLevel serverLevel ) {
            BlockPos blockPos = this.getHearth();

            if ( serverLevel.getBlockEntity( blockPos ) instanceof DomovoiHearthBlockEntity domovoiHearthBlockEntity) {
                domovoiHearthBlockEntity.removeDecayMob( this.getUUID() );
            }
        }
    }




    private class MothWanderGoal extends Goal {

        public MothWanderGoal() { this.setFlags( EnumSet.of( Flag.MOVE ) ); }



        private Vec3 findPos() {
            int wanderThreshold = 100;
            Vec3 wanderDirection;

            if (
                    Moth.this.hasHearth()
                    && !Moth.this.closerThan( Moth.this.getHearth(), wanderThreshold )
            ) {

                Vec3 hearthPos = Vec3.atCenterOf( Moth.this.getHearth() );

                wanderDirection = hearthPos
                        .subtract(Moth.this.position())
                        .normalize();

            } else { wanderDirection = Moth.this.getViewVector( 0.0F ); }


            return HoverRandomPos.getPos(
                    Moth.this,
                    8,
                    7,
                    wanderDirection.x,
                    wanderDirection.z,
                    ( float ) Math.PI / 2F,
                    3,
                    1
            );
        }



        @Override
        public boolean canUse() { return Moth.this.navigation.isDone() && Moth.this.random.nextInt( 10 ) == 0; }

        @Override
        public boolean canContinueToUse() { return Moth.this.navigation.isInProgress(); }



        @Override
        public void start() {
            Vec3 targetPos = this.findPos();
            if ( targetPos != null ) {
                Moth.this.navigation.moveTo(
                        Moth.this.navigation.createPath(
                                BlockPos.containing( targetPos ),
                                1
                        ),
                        1.0
                );
            }

            Moth.this.setMothBehaviorState( MothBehaviorState.WANDER );
        }

        @Override
        public void stop() {
            Moth.this.setMothBehaviorState( null );
        }
    }

    private class MothHoverGoal extends Goal {
        private Vec3 hoverOrigin;
        private int hoverTicks;

        public MothHoverGoal() { this.setFlags( EnumSet.of( Flag.MOVE ) ); }


        private void chooseHoverPosition() {
            double x = this.hoverOrigin.x
                    + ( Moth.this.random.nextDouble() * 2.0 - 1.0 ) * 3.0;

            double y = this.hoverOrigin.y
                    + ( Moth.this.random.nextDouble() * 2.0 - 1.0 ) * 2.0;

            double z = this.hoverOrigin.z
                    + ( Moth.this.random.nextDouble() * 2.0 - 1.0 ) * 3.0;

            Vec3 target = new Vec3( x, y, z );

            Moth.this.navigation.moveTo(
                    Moth.this.navigation.createPath(
                            BlockPos.containing( target ),
                            1
                    ),
                    0.6
            );
        }



        @Override
        public boolean canUse() { return Moth.this.navigation.isDone(); }

        @Override
        public boolean canContinueToUse() {
            return hoverTicks > 0;
        }



        @Override
        public void start() {
            this.hoverOrigin = Moth.this.position();
            this.hoverTicks = Moth.this.random.nextInt( 40, 140 );

            this.chooseHoverPosition();

            Moth.this.setMothBehaviorState( MothBehaviorState.HOVER );
        }

        @Override
        public void tick() {
            if ( this.hoverTicks-- <= 0 ) {
                Moth.this.navigation.stop();
            } else if ( Moth.this.navigation.isDone() ) {
                this.chooseHoverPosition();
            }
        }

        @Override
        public void stop() {
            Moth.this.setMothBehaviorState( null );
        }
    }
}
