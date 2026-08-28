package net.noname.thedomovoi.entity.domovoi;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.noname.thedomovoi.TheDomovoi;
import net.noname.thedomovoi.block.dust.DustBlock;
import net.noname.thedomovoi.block.hearth.DomovoiHearthBlockEntity;
import net.noname.thedomovoi.block.offeringcup.OfferingCupBlock;
import net.noname.thedomovoi.entity.ModEntitySounds;
import net.noname.thedomovoi.entity.bug.Bug;
import net.noname.thedomovoi.entity.moth.Moth;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class Domovoi extends PathfinderMob implements GeoEntity {

    public enum AnimationState {
        IDLE( RawAnimation.begin().thenLoop( "idle" ) ),
        SLEEPING( RawAnimation.begin().thenLoop( "sleep" ) ),
        CONSUMING( RawAnimation.begin().thenLoop( "consume" ) ),
        SWEEPING( RawAnimation.begin().thenLoop( "sweep" ) ),
        DUSTING( RawAnimation.begin().thenLoop( "dust" ) ),
        NONE( RawAnimation.begin().thenLoop( "none" ) );

        private final RawAnimation rawAnimation;

        AnimationState( RawAnimation pRawAnimation ) { this.rawAnimation = pRawAnimation; }


        public RawAnimation getAnimation() { return this.rawAnimation; }
    }
    public static final EntityDataAccessor<Integer> ANIMATION_STATE
            = SynchedEntityData.defineId( Domovoi.class, EntityDataSerializers.INT );

    public enum ConsumeType {
        NONE,
        MOTH,
        BUG,
        MILK,
        BREAD
    }
    public static final EntityDataAccessor<Integer> CONSUME_TYPE
            = SynchedEntityData.defineId( Domovoi.class, EntityDataSerializers.INT );
    public static final EntityDataAccessor<Integer> CONSUME_MOB_VARIANT
            = SynchedEntityData.defineId( Domovoi.class, EntityDataSerializers.INT );

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache( this );

    private BlockPos hearthBlockPos;

    public enum InitialGoalIntent {
        HUNTING,
        CLEANING,
        RECEIVE_OFFERINGS
    }
    public InitialGoalIntent initialGoalIntent;

    private float respect;
    private float comfort;

    public enum RevealState {
        VISIBLE,
        FADING_OUT,
        INVISIBLE,
        FADING_IN
    }
    public static final EntityDataAccessor<Integer> REVEAL_STATE
            = SynchedEntityData.defineId( Domovoi.class, EntityDataSerializers.INT );
    private float previousRevealProgress = 0.0F;
    private float revealProgress = 0.0F;
    private final float REVEAL_SPEED = 0.05F;

    public Domovoi( EntityType<? extends PathfinderMob> type, Level level ) {
        super( type, level );
    }



    @Override
    protected void defineSynchedData( SynchedEntityData.@NonNull Builder entityData ) {
        super.defineSynchedData( entityData );

        entityData.define( ANIMATION_STATE, AnimationState.IDLE.ordinal() );
        entityData.define( REVEAL_STATE, RevealState.INVISIBLE.ordinal() );
        entityData.define( CONSUME_TYPE, ConsumeType.NONE.ordinal() );
        entityData.define( CONSUME_MOB_VARIANT, 0 );
    }


    @Override
    protected void addAdditionalSaveData( @NonNull ValueOutput output ) {
        super.addAdditionalSaveData( output );

        if ( this.hearthBlockPos != null ) { output.store( "hearth_pos", BlockPos.CODEC, this.hearthBlockPos ); }

        output.putFloat( "respect", this.getRespect() );
        output.putFloat( "comfort", this.getComfort() );
    }

    @Override
    protected void readAdditionalSaveData( @NonNull ValueInput input ) {
        super.readAdditionalSaveData( input );

        this.hearthBlockPos = input.read( "hearth_pos", BlockPos.CODEC ).orElse( null );

        this.respect = input.getFloatOr( "respect", 0.1F );
        this.comfort = input.getFloatOr( "comfort", 0.0F );
    }



    @Override
    public void registerControllers( AnimatableManager.@NonNull ControllerRegistrar controllers ) {
        controllers.add( new AnimationController<>(
                state -> {
                    return state.setAndContinue( this.getAnimationState().getAnimation() );
                }
        ).setSoundKeyframeHandler( event -> {

        } ).setParticleKeyframeHandler( event -> {
            if ( !this.level().isClientSide() ) { return; }

            switch ( this.getAnimationState() ) {
                case SWEEPING -> {
                    this.spawnCleaningParticles( Blocks.CONCRETE.gray().defaultBlockState() );
                }
                case DUSTING -> {
                    this.spawnCleaningParticles( Blocks.WOOL.white().defaultBlockState() );
                }
            }
        } ) );
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal( 1, new HuntIntruderGoal() );
        this.goalSelector.addGoal( 2, new CleanHomeGoal() );
        this.goalSelector.addGoal( 3, new CheckForOfferingsGoal() );
        this.goalSelector.addGoal( 4, new SleepGoal() );
        this.goalSelector.addGoal( 5, new ReturnHomeGoal() );
    }



    public AnimationState getAnimationState()
    { return AnimationState.values()[ this.entityData.get( ANIMATION_STATE ) ]; }
    public void setAnimationState( AnimationState animationState )
    { this.entityData.set( ANIMATION_STATE, animationState.ordinal() ); }

    public ConsumeType getConsumeType()
    { return ConsumeType.values()[ this.entityData.get( CONSUME_TYPE ) ]; }
    public void setConsumeType( ConsumeType pConsumeType )
    { this.entityData.set( CONSUME_TYPE, pConsumeType.ordinal() ); }

    public int getConsumeMobVariant() { return this.entityData.get( CONSUME_MOB_VARIANT ); }
    public void setConsumeMobVariant( int pVariant ) { this.entityData.set( CONSUME_MOB_VARIANT, pVariant ); }

    @Override
    public @NonNull AnimatableInstanceCache getAnimatableInstanceCache() { return this.geoCache; }



    public DomovoiHearthBlockEntity getHearthBlock() {
        BlockEntity blockEntity = this.level().getBlockEntity( this.hearthBlockPos);
        if ( blockEntity instanceof DomovoiHearthBlockEntity domovoiHearthBlockEntity ) {
            return domovoiHearthBlockEntity;
        } else { return null; }
    }
    public void setHearthBlockPos( BlockPos pBlockPos ) { this.hearthBlockPos = pBlockPos; }


    public float getRespect() { return this.respect; }
    public void setRespect( float respect ) { this.respect = respect; }

    public float getComfort() { return this.comfort; }
    public void setComfort( float comfort ) { this.comfort = comfort; }

    public void setInitialGoalIntent( InitialGoalIntent pInitialGoalIntent )
    { this.initialGoalIntent = pInitialGoalIntent; }


    public float getRevealProgress()
    { return this.revealProgress; }
    public float getRevealProgressWithPartialTick( float pPartialTick )
    { return Mth.lerp( pPartialTick, this.previousRevealProgress, this.revealProgress ); }

    public RevealState getRevealState()
    { return RevealState.values()[ this.entityData.get( REVEAL_STATE ) ]; }
    public void setRevealState( RevealState pRevealState )
    { this.entityData.set( REVEAL_STATE, pRevealState.ordinal() ); }



    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModEntitySounds.DOMOVOI_SOUND.value();
    }

    @Override
    public int getAmbientSoundInterval() {
        return this.random.nextInt( 100, 200 );
    }



    private Vec3 getCleaningParticlePos( double forwardOffset, double sideOffset ) {
        float yawRadians = Domovoi.this.getYRot() * Mth.DEG_TO_RAD;

        Vec3 forward = new Vec3( -Mth.sin( yawRadians ), 0.0, Mth.cos( yawRadians ) );
        Vec3 right = new Vec3( forward.z, 0.0, -forward.x );

        return Domovoi.this.position()
                .add( 0.0, 0.05, 0.0 )
                .add( forward.scale( forwardOffset ) )
                .add( right.scale( sideOffset ) );
    }

    private void spawnCleaningParticles( BlockState blockState ) {
        Level level = this.level();

        BlockParticleOption blockParticleOption = new BlockParticleOption(
                ParticleTypes.BLOCK,
                blockState
        );

        double sideOffset = ( this.random.nextDouble() - 0.5 ) * 0.5;
        Vec3 particlePos = this.getCleaningParticlePos( 0.6, sideOffset );

        this.level().addParticle(
                blockParticleOption,
                particlePos.x,
                particlePos.y,
                particlePos.z,
                ( this.random.nextDouble() - 0.5 ) * 0.8,
                0.04 + this.random.nextDouble() * 0.05,
                ( this.random.nextDouble() - 0.5 ) * 0.8
        );
    }



    private boolean canTeleportTo( Vec3 pTarget ) {
        Vec3 movement = pTarget.subtract( Domovoi.this.position() );

        AABB targetBox = Domovoi.this.getBoundingBox().move( movement );

        return Domovoi.this.level().noCollision( Domovoi.this, targetBox );
    }

    @Override
    public boolean canBeCollidedWith( @Nullable Entity other ) {
        return this.getRevealProgress() >= 1.0F;
    }



    private Vec3 getMovePos( BlockPos pTargetBlockPos, double pOffsetMax, double pOffsetMin ) {
        Vec3 center = Vec3.atCenterOf( pTargetBlockPos );

        double offset = pOffsetMin + Domovoi.this.random.nextDouble() * ( pOffsetMax - pOffsetMin );

        List<Vec3> candidateTargets = new ArrayList<>( List.of(
                center.add( 0, 0, -offset ),
                center.add( 0, 0, offset ),
                center.add( -offset, 0, 0 ),
                center.add( offset, 0, 0 ),
                center
        ) );
        Collections.shuffle( candidateTargets );

        for ( Vec3 target : candidateTargets ) {
            if ( this.canTeleportTo( target ) ) { return target; }
        }
        return null;
    }



    private void updateReveal() {
        switch ( this.getRevealState() ) {
            case VISIBLE -> this.revealProgress = 1.0F;

            case FADING_OUT -> {
                this.previousRevealProgress = this.revealProgress;
                this.revealProgress -= this.REVEAL_SPEED;

                if ( this.revealProgress <= 0.0F ) {
                    this.previousRevealProgress = 0.0F;
                    this.revealProgress = 0.0F;

                    this.setRevealState( RevealState.INVISIBLE );
                }
            }

            case INVISIBLE -> this.revealProgress = 0.0F;

            case FADING_IN -> {
                this.previousRevealProgress = this.revealProgress;
                this.revealProgress += this.REVEAL_SPEED;

                if ( this.revealProgress >= 1.0F ) {
                    this.previousRevealProgress = 1.0F;
                    this.revealProgress = 1.0F;

                    this.setRevealState( RevealState.VISIBLE );
                }
            }
        }
    }



    public boolean isSleeping() { return this.getAnimationState() == AnimationState.SLEEPING; }

    @Override
    public boolean isInvulnerableTo( @NonNull ServerLevel level, @NonNull DamageSource source ) { return true; }



    @Override
    public void tick() {
        super.tick();

        this.updateReveal();
    }




    private class HuntIntruderGoal extends Goal {
        private enum HuntingState { NONE, MOTH, BUG }

        private DomovoiHearthBlockEntity domovoiHearthBlockEntity;

        private HuntingState huntingState;

        private UUID decayEntityUUID;
        private Moth moth;
        private Bug bug;

        private boolean hasCaughtIntruder;

        private int eatingTicks;

        private int revealTicks;
        private boolean willReveal;
        private boolean hasRevealed;

        public HuntIntruderGoal() { this.setFlags( EnumSet.of( Flag.MOVE ) ); }


        @Override
        public boolean canUse() {
            if ( Domovoi.this.navigation.isInProgress() ) { return false; }

            DomovoiHearthBlockEntity domovoiHearthBlockEntity = Domovoi.this.getHearthBlock();
            if (
                    domovoiHearthBlockEntity != null
                            && !domovoiHearthBlockEntity.decayMobs.isEmpty()
                            && (
                                    Domovoi.this.initialGoalIntent == InitialGoalIntent.HUNTING
                                            || Domovoi.this.random.nextFloat() < Domovoi.this.getRespect()
                    )
            ) {
                this.domovoiHearthBlockEntity = domovoiHearthBlockEntity;

                this.decayEntityUUID = this.domovoiHearthBlockEntity.decayMobs.get(
                        Domovoi.this.random.nextInt( this.domovoiHearthBlockEntity.decayMobs.size() )
                );

                Entity entity = Domovoi.this.level().getEntity( this.decayEntityUUID );
                if ( entity instanceof Moth mothIntruder ) {
                    this.huntingState = HuntingState.MOTH;
                    this.moth = mothIntruder;

                    return true;
                } else if ( entity instanceof Bug bugIntruder ) {
                    this.huntingState = HuntingState.BUG;
                    this.bug = bugIntruder;

                    return true;
                }
            } else { return false; }
            return false;
        }

        @Override
        public boolean canContinueToUse() { return this.huntingState != HuntingState.NONE; }



        @Override
        public void start() {
            if ( this.huntingState == HuntingState.MOTH ) {
                Domovoi.this.setNoGravity( true );
                Domovoi.this.setDeltaMovement( Vec3.ZERO );
            }

            this.eatingTicks = Domovoi.this.random.nextInt( 500, 700 );

            if ( Domovoi.this.getRevealState() == RevealState.VISIBLE )
            { Domovoi.this.setRevealState( RevealState.FADING_OUT ); }

            if (
                    Domovoi.this.getRevealState() == RevealState.INVISIBLE
                            && Domovoi.this.random.nextFloat() < Domovoi.this.getComfort()
            ) {
                this.willReveal = true;
                this.revealTicks = Domovoi.this.random.nextInt( 20, 40 );
            }
        }

        @Override
        public void tick() {
            if ( this.huntingState == HuntingState.MOTH ) {
                if (
                        this.moth.getMothBehaviorState() == Moth.MothBehaviorState.HOVER
                                && !this.hasCaughtIntruder
                ) {
                    Domovoi.this.teleportTo(
                            this.moth.getBlockX(),
                            this.moth.getBlockY(),
                            this.moth.getBlockZ()
                    );
                    Domovoi.this.setDeltaMovement( Vec3.ZERO );

                    this.moth.discard();

                    Domovoi.this.setAnimationState( AnimationState.CONSUMING );

                    Domovoi.this.setConsumeType( ConsumeType.MOTH );
                    Domovoi.this.setConsumeMobVariant( this.moth.getVariant() );


                    this.hasCaughtIntruder = true;
                } else if ( this.hasCaughtIntruder ) {
                    if ( this.willReveal ) {
                        if ( this.revealTicks <= 0 && !this.hasRevealed ) {
                            Domovoi.this.setRevealState( RevealState.FADING_IN );

                            this.hasRevealed = true;
                        } else { this.revealTicks--; }
                    }

                    if ( this.eatingTicks <= 0 ) {
                        Domovoi.this.setAnimationState( AnimationState.IDLE );

                        this.huntingState = HuntingState.NONE;
                    } else { this.eatingTicks--; }
                }
            } else if ( this.huntingState == HuntingState.BUG ) {
                if (
                        this.bug.getBugBehaviorState() == Bug.BugBehaviorState.CONTEMPLATE_EXISTENCE
                                && !this.hasCaughtIntruder
                ) {
                    Domovoi.this.teleportTo(
                            this.bug.getBlockX(),
                            this.bug.getBlockY(),
                            this.bug.getBlockZ()
                    );
                    Domovoi.this.setDeltaMovement( Vec3.ZERO );

                    this.bug.discard();

                    Domovoi.this.setAnimationState( AnimationState.CONSUMING );

                    Domovoi.this.setConsumeType( ConsumeType.BUG );
                    Domovoi.this.setConsumeMobVariant( this.bug.getVariant() );


                    this.hasCaughtIntruder = true;
                } else if ( this.hasCaughtIntruder ) {
                    if ( this.willReveal ) {
                        if ( this.revealTicks <= 0 && !this.hasRevealed ) {
                            Domovoi.this.setRevealState( RevealState.FADING_IN );

                            this.hasRevealed = true;
                        } else { this.revealTicks--; }
                    }

                    if ( this.eatingTicks <= 0 ) {
                        Domovoi.this.setAnimationState( AnimationState.IDLE );

                        this.huntingState = HuntingState.NONE;
                    } else { this.eatingTicks--; }
                }
            }
        }

        @Override
        public void stop() {
            if ( Domovoi.this.getHearthBlock() != null ) {
                this.domovoiHearthBlockEntity.removeMob(
                        this.domovoiHearthBlockEntity.decayMobsIndexOf,
                        this.domovoiHearthBlockEntity.decayMobs,
                        this.decayEntityUUID
                );
            }

            if ( this.huntingState == HuntingState.MOTH ) { Domovoi.this.setNoGravity( false ); }

            Domovoi.this.setConsumeType( ConsumeType.NONE );

            this.domovoiHearthBlockEntity = null;

            this.huntingState = null;

            this.decayEntityUUID = null;
            this.moth = null;
            this.bug = null;

            this.hasCaughtIntruder = false;

            this.willReveal = false;
            this.hasRevealed = false;
        }
    }

    private class CleanHomeGoal extends Goal {
        private enum CleanType {
            DUST( 0.7, 0.5 ),
            COBWEB( 0.7, 0.5 );


            public final double maxMoveOffset;
            public final double minMoveOffset;

            CleanType( double maxMoveOffset, double minMoveOffset ) {
                this.maxMoveOffset = maxMoveOffset;
                this.minMoveOffset = minMoveOffset;
            }
        }

        private DomovoiHearthBlockEntity domovoiHearthBlockEntity;

        private BlockPos decayBlockPos;
        private Vec3 decayBlockTarget;

        private CleanType cleanType;
        private int cleanTicks;
        private boolean cleaningStarted;

        private int revealTicks;
        private boolean willReveal;
        private boolean willHide;
        private boolean hasRevealed;

        public CleanHomeGoal() { this.setFlags( EnumSet.of( Flag.MOVE, Flag.LOOK ) ); }



        private void calculateCleanType() {
            this.cleanType = null;

            BlockState blockState = Domovoi.this.level().getBlockState( this.decayBlockPos );

            if ( blockState.getBlock() instanceof DustBlock ) { this.cleanType = CleanType.DUST; }
            else if ( blockState.is( Blocks.COBWEB ) ) { this.cleanType = CleanType.COBWEB; }
            else { this.domovoiHearthBlockEntity.removeDecayBlock( this.decayBlockPos ); }
        }



        @Override
        public boolean canUse() {
            if ( Domovoi.this.navigation.isInProgress() ) { return false; }

            DomovoiHearthBlockEntity domovoiHearthBlockEntity = Domovoi.this.getHearthBlock();
            if (
                    domovoiHearthBlockEntity != null
                            && !domovoiHearthBlockEntity.decayBlocks.isEmpty()
                            && (
                                    Domovoi.this.initialGoalIntent == InitialGoalIntent.CLEANING
                                            || Domovoi.this.random.nextFloat() < Domovoi.this.getRespect()
                    )
            ) {
                this.domovoiHearthBlockEntity = domovoiHearthBlockEntity;

                this.decayBlockPos = this.domovoiHearthBlockEntity.decayBlocks.get(
                        Domovoi.this.random.nextInt( this.domovoiHearthBlockEntity.decayBlocks.size() )
                );
                this.calculateCleanType();

                return this.cleanType != null;
            } else { return false; }
        }

        @Override
        public boolean canContinueToUse() { return this.cleanTicks > 0; }



        @Override
        public void start() {
            Domovoi.this.initialGoalIntent = null;

            this.decayBlockTarget = Domovoi.this.getMovePos(
                    this.decayBlockPos,
                    this.cleanType.maxMoveOffset,
                    this.cleanType.minMoveOffset
            );

            if (this.decayBlockTarget != null) {

                Domovoi.this.teleportTo(
                        this.decayBlockTarget.x,
                        this.decayBlockTarget.y,
                        this.decayBlockTarget.z
                );

                Domovoi.this.getLookControl().setLookAt(
                        this.decayBlockPos.getX() + 0.5,
                        this.decayBlockPos.getY() + 0.5,
                        this.decayBlockPos.getZ() + 0.5,
                        30.0F, 30.0F
                );

                if ( this.cleanType == CleanType.COBWEB ) {
                    Domovoi.this.setNoGravity( true );
                    Domovoi.this.setDeltaMovement( Vec3.ZERO );

                    Domovoi.this.setAnimationState( AnimationState.DUSTING );
                } else { Domovoi.this.setAnimationState( AnimationState.SWEEPING ); }


                this.cleanTicks = Domovoi.this.random.nextInt( 100, 200 );
                this.cleaningStarted = true;


                if (
                        Domovoi.this.getRevealState() == RevealState.INVISIBLE
                                && Domovoi.this.random.nextFloat() < Domovoi.this.getComfort()
                ) {
                    TheDomovoi.LOGGER.info( "reveal" );
                    this.willReveal = true;
                    this.revealTicks = Domovoi.this.random.nextInt( 20, 50 );
                } else if (
                        Domovoi.this.getRevealState() == RevealState.VISIBLE
                                && Domovoi.this.random.nextFloat() > Domovoi.this.getComfort()
                ) {
                    TheDomovoi.LOGGER.info( "hide" );
                    this.willHide = true;
                    this.revealTicks = Domovoi.this.random.nextInt( 20, 50 );
                }
            }
        }

        @Override
        public void tick() {
            this.cleanTicks--;

            Domovoi.this.getLookControl().setLookAt(
                    this.decayBlockPos.getX() + 0.5,
                    this.decayBlockPos.getY() + 0.5,
                    this.decayBlockPos.getZ() + 0.5,
                    30.0F, 30.0F
            );

            if ( this.cleanType == CleanType.COBWEB ) { Domovoi.this.setDeltaMovement( Vec3.ZERO ); }

            if ( this.willReveal ) {
                if ( this.revealTicks <= 0 && !this.hasRevealed ) {
                    Domovoi.this.setRevealState( RevealState.FADING_IN );

                    this.hasRevealed = true;
                } else { this.revealTicks--; }
            } else if ( this.willHide ) {
                if ( this.revealTicks <= 0 && !this.hasRevealed ) {
                    Domovoi.this.setRevealState( RevealState.FADING_OUT );

                    this.hasRevealed = true;
                } else { this.revealTicks--; }
            }
        }

        @Override
        public void stop() {
            if ( this.cleaningStarted && Domovoi.this.getHearthBlock() != null ) {
                Domovoi.this.level().destroyBlock( this.decayBlockPos, false );
                this.domovoiHearthBlockEntity.removeDecayBlock( this.decayBlockPos );
            }

            Domovoi.this.setAnimationState( AnimationState.IDLE );

            if ( this.cleanType == CleanType.COBWEB ) { Domovoi.this.setNoGravity( false ); }

            this.domovoiHearthBlockEntity = null;

            this.decayBlockPos = null;
            this.decayBlockTarget = null;

            this.cleanType = null;
            this.cleaningStarted = false;

            this.hasRevealed = false;
            this.willReveal = false;
            this.willHide = false;
        }
    }

    private class CheckForOfferingsGoal extends Goal {
        private enum OfferingCupGoal { DRINK_MILK, EAT_BREAD }

        private DomovoiHearthBlockEntity domovoiHearthBlockEntity;

        private BlockPos offeringCupBlockPos;
        private Vec3 offeringCupBlockTarget;

        private OfferingCupGoal offeringCupGoal;

        private boolean cupHasMilk;
        private int drinkMilkTicks;
        private boolean hasStartedDrinkingMilk;

        private boolean cupHasBread;
        private int eatBreadTicks;
        private boolean hasStartedEatingBread;

        private int revealTicks;
        private boolean hasRevealed;


        public CheckForOfferingsGoal() { this.setFlags( EnumSet.of( Flag.MOVE ) ); }



        @Override
        public boolean canUse() {
            if ( Domovoi.this.navigation.isInProgress() ) { return false; }

            DomovoiHearthBlockEntity domovoiHearthBlockEntity = Domovoi.this.getHearthBlock();
            if (
                    domovoiHearthBlockEntity != null
                            && !domovoiHearthBlockEntity.decayBlocks.isEmpty()
                            && (
                            Domovoi.this.initialGoalIntent == InitialGoalIntent.RECEIVE_OFFERINGS
                                    || Domovoi.this.random.nextFloat() < Domovoi.this.getRespect()
                    )
            ) {
                this.domovoiHearthBlockEntity = domovoiHearthBlockEntity;

                for ( BlockPos blockPos : this.domovoiHearthBlockEntity.offeringCupBlocks ) {
                    boolean hasMilk = OfferingCupBlock.getHasMilk( Domovoi.this.level(), blockPos );
                    boolean hasBread = OfferingCupBlock.getHasBread( Domovoi.this.level(), blockPos );

                    if ( hasMilk || hasBread ) {
                        this.offeringCupBlockPos = blockPos;

                        this.cupHasMilk = hasMilk;
                        this.cupHasBread = hasBread;

                        return true;
                    }
                }
            } else { return false; }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return
                    this.offeringCupBlockTarget != null
                            && this.offeringCupGoal != null;
        }



        @Override
        public void start() {
            Domovoi.this.initialGoalIntent = null;

            this.offeringCupBlockTarget = Domovoi.this.getMovePos(
                    this.offeringCupBlockPos,
                    0.7D,
                    0.5D
            );

            if ( this.offeringCupBlockTarget != null ) {
                Domovoi.this.teleportTo(
                        this.offeringCupBlockTarget.x,
                        this.offeringCupBlockTarget.y,
                        this.offeringCupBlockTarget.z
                );

                Domovoi.this.getLookControl().setLookAt(
                        this.offeringCupBlockPos.getX() + 0.5,
                        this.offeringCupBlockPos.getY() + 0.5,
                        this.offeringCupBlockPos.getZ() + 0.5
                );

                if ( this.cupHasMilk ) { this.offeringCupGoal = OfferingCupGoal.DRINK_MILK; }
                else if ( this.cupHasBread ) { this.offeringCupGoal = OfferingCupGoal.EAT_BREAD; }

                if ( this.cupHasMilk ) { this.drinkMilkTicks = Domovoi.this.random.nextInt( 60, 120 ); }
                if ( this.cupHasBread ) { this.eatBreadTicks = Domovoi.this.random.nextInt( 60, 120 ); }

                this.revealTicks = Domovoi.this.random.nextInt( 20, 50 );
            }
        }

        @Override
        public void tick() {
            if ( this.offeringCupGoal == OfferingCupGoal.DRINK_MILK ) {
                if ( !this.hasStartedDrinkingMilk ) {
                    Domovoi.this.setConsumeType( ConsumeType.MILK );
                    Domovoi.this.setAnimationState( AnimationState.CONSUMING );
                }

                if ( this.drinkMilkTicks <= 0 ) {
                    OfferingCupBlock.setHasMilk( Domovoi.this.level(), this.offeringCupBlockPos, false );

                    Domovoi.this.setAnimationState( AnimationState.IDLE );

                    if ( this.cupHasBread ) { this.offeringCupGoal = OfferingCupGoal.EAT_BREAD; }
                } else { this.drinkMilkTicks--; }

            } else if ( this.offeringCupGoal == OfferingCupGoal.EAT_BREAD ) {
                    if ( !this.hasStartedEatingBread ) {
                        Domovoi.this.setConsumeType( ConsumeType.BREAD );
                        Domovoi.this.setAnimationState( AnimationState.CONSUMING );
                    }

                    if ( this.eatBreadTicks <= 0 ) {
                        OfferingCupBlock.setHasBread( Domovoi.this.level(), this.offeringCupBlockPos, false );

                        Domovoi.this.setAnimationState( AnimationState.IDLE );

                        this.offeringCupGoal = null;
                    } else { this.eatBreadTicks--; }
            }

            if ( this.revealTicks <= 0 && !this.hasRevealed ) {
                Domovoi.this.setRevealState( RevealState.FADING_IN );

                this.hasRevealed = true;
            } else { this.revealTicks--; }
        }

        @Override
        public void stop() {
            Domovoi.this.setRevealState( RevealState.FADING_OUT );

            this.domovoiHearthBlockEntity = null;

            this.offeringCupBlockPos = null;

            this.cupHasMilk = false;
            this.drinkMilkTicks = 0;
            this.hasStartedDrinkingMilk = false;

            this.cupHasBread = false;
            this.eatBreadTicks = 0;
            this.hasStartedEatingBread = false;

            this.hasRevealed = false;
        }
    }

    private class SleepGoal extends Goal {
        private int sleepTicks;

        private int revealTicks;
        private boolean hasRevealed;
        private int unRevealTicks;
        private boolean hasUnrevealed;

        public SleepGoal() { this.setFlags( EnumSet.of( Flag.MOVE ) ); }



        @Override
        public boolean canUse() {
            if ( Domovoi.this.navigation.isInProgress() ) { return false; }

            return Domovoi.this.random.nextFloat() < Domovoi.this.getComfort()
                    && Domovoi.this.random.nextInt( 10 ) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.unRevealTicks > 0;
        }



        @Override
        public void start() {
            Vec3 sleepBlockPos = DefaultRandomPos.getPos( Domovoi.this, 10, 7 );
            if ( sleepBlockPos != null ) {
                Domovoi.this.teleportTo(
                        sleepBlockPos.x,
                        sleepBlockPos.y,
                        sleepBlockPos.z
                );

                this.sleepTicks = Domovoi.this.random.nextInt( 6000, 18000 );
                Domovoi.this.setAnimationState( AnimationState.SLEEPING );

                this.revealTicks    = Domovoi.this.random.nextInt( 20, 50 );
                this.hasRevealed    = false;
                this.unRevealTicks  = Domovoi.this.random.nextInt( 20, 50 );
                this.hasUnrevealed  = false;
            }
        }

        @Override
        public void tick() {
            if ( this.revealTicks <= 0 && !this.hasRevealed ) {
                Domovoi.this.setRevealState( RevealState.FADING_IN );

                this.hasRevealed = true;
            } else { this.revealTicks--; }

            if ( this.sleepTicks <= 0 ) {
                if ( !hasUnrevealed ) {
                    Domovoi.this.setRevealState( RevealState.FADING_OUT );

                    this.hasUnrevealed = true;
                }

                if ( this.unRevealTicks > 0 ) { this.unRevealTicks--; }
            } else { this.sleepTicks--; }
        }

        @Override
        public void stop() { Domovoi.this.setAnimationState( AnimationState.IDLE ); }


    }

    private class ReturnHomeGoal extends Goal {

        public ReturnHomeGoal() { this.setFlags( EnumSet.of( Flag.MOVE ) ); }



        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean canContinueToUse() { return false; }


        @Override
        public void start() {
            DomovoiHearthBlockEntity domovoiHearthBlockEntity = Domovoi.this.getHearthBlock();
            if ( domovoiHearthBlockEntity == null ) { Domovoi.this.discard(); return; }

            domovoiHearthBlockEntity.handleDomovoiReturn( Domovoi.this );
        }
    }
}
