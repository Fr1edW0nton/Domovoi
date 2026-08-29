package net.noname.thedomovoi.block.hearth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.noname.thedomovoi.TheDomovoi;
import net.noname.thedomovoi.block.ModBlocks;
import net.noname.thedomovoi.block.dust.DustBlock;
import net.noname.thedomovoi.block.offeringcup.OfferingCupBlock;
import net.noname.thedomovoi.entity.ModEntities;
import net.noname.thedomovoi.entity.bug.Bug;
import net.noname.thedomovoi.entity.domovoi.Domovoi;
import net.noname.thedomovoi.entity.domovoi.DomovoiData;
import net.noname.thedomovoi.entity.moth.Moth;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DomovoiHearthBlockEntity extends BlockEntity {

    private static final Codec<Map.Entry<BlockPos, Integer>> ENTRY_CODEC
            = RecordCodecBuilder.create(instance -> instance.group(
                    BlockPos.CODEC.fieldOf( "pos" ).forGetter( Map.Entry::getKey ),
                    Codec.INT.fieldOf( "value" ).forGetter( Map.Entry::getValue )
            ).apply( instance, Map::entry )
    );

    private static final Codec<Map<BlockPos, Integer>> BLOCK_MAP_CODEC =
            ENTRY_CODEC.listOf().xmap(
                    list -> list
                            .stream()
                            .collect(Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ),
                    map -> new ArrayList<>( map.entrySet() )
            );

    private static final Codec<Map<UUID, Integer>> MOB_MAP_CODEC =
            Codec.unboundedMap( UUIDUtil.STRING_CODEC, Codec.INT );


    private final RandomSource random;


    public enum Time {
        SECOND( 20 ),
        MINUTE( 1_200 ),
        HOUR( 72_000 ),
        DAY( 24_000 );

        private final int ticks;

        Time( int pTicks ) { this.ticks = pTicks; }


        public int getTicks() { return ticks; }
    }

    public enum HomeState {
        UNCOMPUTED,
        COMPUTING,
        COMPUTED
    }

    public record DecayConfig (
        float dustSpawnChance,
        float cobwebSpawnChance,
        int maxMobCount,
        float mothSpawnChance,
        float bugSpawnChance
    ) {}

    private UUID ownerUUID;

    private HomeState homeState = HomeState.UNCOMPUTED;
    private int homeTimer;

    private int monitorTimer;

    private Set<BlockPos> homeDataRecord = new HashSet<>();
    private Queue<BlockPos> homeDataQueue = new ArrayDeque<>();

    private List<BlockPos> floorBlocks = new ArrayList<>();
    private List<BlockPos> cornerBlocks = new ArrayList<>();

    public List<BlockPos> offeringCupBlocks = new ArrayList<>();
    public Map<BlockPos, Integer> offeringCupIndexOf = new HashMap<>();

    private DomovoiData domovoiData;
    private int domovoiTimer;

    private static final DecayConfig DECAY_CONFIG = new DecayConfig(
            0.01F,
            0.007F,
           10,
            1.0F,
            1.0F
    );

    public List<BlockPos> decayBlocks = new ArrayList<>();
    public Map<BlockPos, Integer> decayBlocksIndexOf = new HashMap<>();

    public List<UUID> decayMobs = new ArrayList<>();
    public Map<UUID, Integer> decayMobsIndexOf = new HashMap<>();

    private int dustTimer;
    private int cobwebTimer;
    private int mothTimer;
    private int bugTimer;

    public DomovoiHearthBlockEntity( BlockPos pWorldPosition, BlockState pBlockState ) {
        super( ModBlocks.DOMOVOI_HEARTH_BLOCK_ENTITY.get(), pWorldPosition, pBlockState );

        this.random         = RandomSource.create();

        this.homeTimer      = getHomeTimer();

        this.monitorTimer   = getMonitorTimer();

        this.domovoiData = new DomovoiData();
        this.domovoiData.setRespect( 0.0F );
        this.domovoiData.setComfort( 0.0F );

        this.domovoiTimer   = getDomovoiTimer();

        this.dustTimer      = getDustTimer();
        this.cobwebTimer    = getCobwebTimer();
        this.mothTimer      = getMothTimer();
        this.bugTimer       = getBugTimer();
    }



    @Override
    protected void saveAdditional( @NonNull ValueOutput pOutput ) {
        super.saveAdditional(pOutput);

        if ( this.getOwnerUUID() != null ) {
            pOutput.store( "owner", UUIDUtil.CODEC, this.getOwnerUUID() );
        }

        pOutput.putString( "home_state", this.homeState.name() );

        if ( this.homeState != HomeState.COMPUTED ) {
            pOutput.store( "home_record", BlockPos.CODEC.listOf(), new ArrayList<>( this.homeDataRecord ) );
            pOutput.store ( "home_queue", BlockPos.CODEC.listOf(), new ArrayList<>( this.homeDataQueue ) );
        }

        pOutput.store( "floor_blocks",                   BlockPos.CODEC.listOf(),    this.floorBlocks );
        pOutput.store( "corner_blocks",                  BlockPos.CODEC.listOf(),    this.cornerBlocks );
        pOutput.store( "offering_cup_blocks",            BlockPos.CODEC.listOf(),    this.offeringCupBlocks );
        pOutput.store( "decay_blocks",                   BlockPos.CODEC.listOf(),    this.decayBlocks );
        pOutput.store( "decay_mobs",                     UUIDUtil.CODEC.listOf(),    this.decayMobs);

        pOutput.store( "offering_cup_blocks_index_of",   BLOCK_MAP_CODEC,            this.offeringCupIndexOf );
        pOutput.store( "decay_blocks_index_of",          BLOCK_MAP_CODEC,            this.decayBlocksIndexOf );
        pOutput.store( "decay_mobs_index_of",            MOB_MAP_CODEC,              this.decayMobsIndexOf );


        this.domovoiData.saveData( pOutput );

        pOutput.putInt( "home_timer",    this.homeTimer );
        pOutput.putInt( "monitor_timer", this.monitorTimer );
        pOutput.putInt( "domovoi_timer", this.domovoiTimer );
        pOutput.putInt( "dust_timer",    this.dustTimer );
        pOutput.putInt( "cobweb_timer",  this.cobwebTimer );
        pOutput.putInt( "moth_timer",    this.mothTimer );
        pOutput.putInt( "bug_timer",     this.bugTimer );
    }

    @Override
    protected void loadAdditional( @NonNull ValueInput pInput ) {
        super.loadAdditional(pInput);

        this.ownerUUID = pInput.read( "owner", UUIDUtil.CODEC ).orElse( null );

        String homeState    = pInput.getStringOr( "home_state", HomeState.UNCOMPUTED.name() );
        this.homeState      = HomeState.valueOf( homeState );

        if ( this.homeState != HomeState.COMPUTED ) {
            this.homeDataRecord = new HashSet<>(
                    pInput.read( "home_record",  BlockPos.CODEC.listOf() ).orElse( List.of() )
            );

            this.homeDataQueue = new ArrayDeque<>(
                    pInput.read( "home_queue",   BlockPos.CODEC.listOf() ).orElse( List.of() )
            );
        }

        this.floorBlocks        = new ArrayList<>(
                                        pInput.read( "floor_blocks", BlockPos.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );
        this.cornerBlocks       = new ArrayList<>(
                                        pInput.read( "corner_blocks", BlockPos.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );
        this.offeringCupBlocks  = new ArrayList<>(
                                        pInput.read( "offering_cup_blocks", BlockPos.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );
        this.decayBlocks        = new ArrayList<>(
                                        pInput.read( "decay_blocks", BlockPos.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );
        this.decayMobs          = new ArrayList<>(
                                        pInput.read( "decay_mobs", UUIDUtil.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );

        this.offeringCupIndexOf = new HashMap<>(
                                        pInput.read( "offering_cup_blocks_index_of", BLOCK_MAP_CODEC )
                                                .orElse( Collections.emptyMap() )
                                );
        this.decayBlocksIndexOf = new HashMap<>(
                                        pInput.read( "decay_blocks_index_of", BLOCK_MAP_CODEC )
                                                .orElse( Collections.emptyMap() )
                                );
        this.decayMobsIndexOf   = new HashMap<>(
                                        pInput.read( "decay_mobs_index_of", MOB_MAP_CODEC )
                                                .orElse( Collections.emptyMap() )
                                );


        UUID domovoiUUID        = pInput.read( "domovoi_uuid", UUIDUtil.CODEC ).orElse( null );
        float respect           = pInput.getFloatOr( "respect", 0.1F );
        float comfort           = pInput.getFloatOr( "comfort", 0.0F );

        this.domovoiData = new DomovoiData();
        this.domovoiData.setDomovoiUUID( domovoiUUID );
        this.domovoiData.setRespect( respect );
        this.domovoiData.setComfort( comfort );

        this.homeTimer          = pInput.getIntOr( "home_timer",     this.getHomeTimer() );
        this.monitorTimer       = pInput.getIntOr( "monitor_timer",  this.getMonitorTimer() );
        this.domovoiTimer       = pInput.getIntOr( "domovoi_timer",  this.getDomovoiTimer() );
        this.dustTimer          = pInput.getIntOr( "dust_timer",     this.getDustTimer() );
        this.cobwebTimer        = pInput.getIntOr( "cobweb_timer",   this.getCobwebTimer() );
        this.mothTimer          = pInput.getIntOr( "moth_timer",     this.getMothTimer() );
        this.bugTimer           = pInput.getIntOr( "bug_timer",      this.getBugTimer() );
    }



    private void reset() {
        this.homeState = HomeState.UNCOMPUTED;

        this.homeDataRecord.clear();
        this.homeDataQueue.clear();

        this.floorBlocks.clear();
        this.cornerBlocks.clear();

        this.offeringCupBlocks.clear();
        this.offeringCupIndexOf.clear();
    }



    public UUID getOwnerUUID() { return this.ownerUUID; }
    public void setOwnerUUID( UUID pUUID ) { this.ownerUUID = pUUID; this.setChanged(); }



    private int getHomeTimer()
    { return random.nextInt( 20 * Time.DAY.getTicks(), 30 * Time.DAY.getTicks() ); }


    private int getMonitorTimer()
    { return random.nextInt( Time.DAY.getTicks(), 3 * Time.DAY.getTicks() ); }


    private int getDomovoiTimer()
    { return random.nextInt( 1 * Time.MINUTE.getTicks(), 2 * Time.MINUTE.getTicks() ); }


    private int getDustTimer()
    { return random.nextInt( 3 * Time.MINUTE.getTicks(), 5 * Time.MINUTE.getTicks() ); }

    private int getCobwebTimer()
    { return random.nextInt( 4 * Time.MINUTE.getTicks(), 8 * Time.MINUTE.getTicks() ); }

    private int getMothTimer()
    { return random.nextInt( 1 * Time.DAY.getTicks(), 2 * Time.DAY.getTicks() ); }

    private int getBugTimer()
    { return random.nextInt( 1 * Time.DAY.getTicks(), 2 * Time.DAY.getTicks() ); }



    private BlockPos getPos( Level pLevel, List<BlockPos> pPositions ) {
        Collections.shuffle( pPositions );

        for ( BlockPos blockPos : pPositions ) {
            if ( pLevel.getBlockState( blockPos ).getCollisionShape( pLevel, blockPos ).isEmpty() ) {
                return blockPos;
            }
        }
        return null;
    }



    private void addBlock( Map<BlockPos, Integer> pIndexOf, List<BlockPos> pBlocks, BlockPos pBlockPos ) {
        if ( pIndexOf.containsKey( pBlockPos ) ) { return; }

        pIndexOf.put( pBlockPos, pBlocks.size() );
        pBlocks.add( pBlockPos );
    }

    private void removeBlock( Map<BlockPos, Integer> pIndexOf, List<BlockPos> pBlocks, BlockPos pBlockPos ) {
        Integer idx = pIndexOf.remove( pBlockPos );
        if ( idx == null ) { return; }

        int lastIdx = pBlocks.size() - 1;
        BlockPos lastElement = pBlocks.get( lastIdx );

        pBlocks.set( idx, lastElement );
        pIndexOf.put( lastElement, idx );
        pBlocks.remove( lastIdx );
    }

    public void addDecayBlock( BlockPos pBlockPos )
    { this.addBlock( this.decayBlocksIndexOf, this.decayBlocks, pBlockPos ); }
    public void removeDecayBlock( BlockPos pBlockPos )
    { this.removeBlock( this.decayBlocksIndexOf, this.decayBlocks, pBlockPos ); }

    public void addOfferingBlock( BlockPos pBlockPos )
    { this.addBlock( this.offeringCupIndexOf, this.offeringCupBlocks, pBlockPos ); }
    public void removeOfferingBlock( BlockPos pBlockPos )
    { this.removeBlock( this.offeringCupIndexOf, this.offeringCupBlocks, pBlockPos ); }


    public void addMob( Map<UUID, Integer> pIndexOf, List<UUID> pUUIDs, UUID pUUID ) {
        if ( pIndexOf.containsKey( pUUID ) ) { return; }

        pIndexOf.put( pUUID, pUUIDs.size() );
        pUUIDs.add( pUUID );
    }

    public void removeMob( Map<UUID, Integer> pIndexOf, List<UUID> pUUIDs, UUID pUUID ) {
        Integer idx = pIndexOf.remove( pUUID );
        if ( idx == null ) { return; }

        int lastIdx = pUUIDs.size() - 1;
        UUID lastElement = pUUIDs.get( lastIdx );

        pUUIDs.set( idx, lastElement );
        pIndexOf.put( lastElement, idx );
        pUUIDs.remove( lastIdx );
    }

    public void addDecayMob( UUID pUUID ) { this.addMob( this.decayMobsIndexOf, this.decayMobs, pUUID ); }
    public void removeDecayMob( UUID pUUID ) { this.removeMob( this.decayMobsIndexOf, this.decayMobs, pUUID ); }



    private void handleDust( Level pLevel, BlockPos pBlockPos ) {
        BlockState blockState = pLevel.getBlockState( pBlockPos );

        if ( blockState.is( Blocks.AIR ) ) {
            BlockState dustBlockState = ModBlocks.DUST_BLOCK.get()
                    .defaultBlockState()
                    .setValue( DustBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection( random ) );

            pLevel.setBlock( pBlockPos, dustBlockState, Block.UPDATE_ALL );

            addDecayBlock( pBlockPos );
        } else if ( blockState.getBlock() instanceof DustBlock ) {
            BlockState newBlockState = DustBlock.addSegment( blockState );

            if ( newBlockState != blockState ) {
                pLevel.setBlock( pBlockPos, newBlockState, Block.UPDATE_ALL );
            }
        }
    }

    private void handleCobweb( Level pLevel, BlockPos pBlockPos ) {
        if ( !decayBlocksIndexOf.containsKey( pBlockPos ) ) {
            pLevel.setBlock( pBlockPos, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_ALL );

            addDecayBlock( pBlockPos );
        }
    }

    private void handleMoth( Level pLevel ) {
        if ( this.decayMobs.size() >= DECAY_CONFIG.maxMobCount() ) { return; }

        List<BlockPos> positions = new ArrayList<>();
        positions.addAll( this.floorBlocks );
        positions.addAll( this.cornerBlocks );

        BlockPos blockPos = getPos( pLevel, positions );
        if ( blockPos == null ) { return; }

        Moth moth = ModEntities.MOTH.get().create( pLevel, EntitySpawnReason.EVENT );
        if ( moth != null ) {
            moth.setPos( blockPos.getX(), blockPos.getY(), blockPos.getZ() );
            moth.setHearth( this.getBlockPos() );

            pLevel.addFreshEntity( moth );

            addDecayMob( moth.getUUID() );
        }
    }

    private void handleBug( Level pLevel ) {
        if ( this.decayMobs.size() >= DECAY_CONFIG.maxMobCount() ) { return; }

        BlockPos blockPos = getPos( pLevel, new ArrayList<>( this.floorBlocks ) );
        if ( blockPos == null ) { return; }

        Bug bug = ModEntities.BUG.get().create( pLevel, EntitySpawnReason.EVENT );
        if ( bug != null ) {
            bug.setPos( blockPos.getX(), blockPos.getY(), blockPos.getZ() );
            bug.setHearth( this.getBlockPos() );

            pLevel.addFreshEntity( bug );

            addDecayMob( bug.getUUID() );
        }
    }

    private void handleDecay( Level pLevel ) {
        RandomSource random = pLevel.getRandom();

        if ( this.dustTimer <= 0 ) {
            for ( BlockPos floorBlockPos : this.floorBlocks ) {
                if ( random.nextFloat() < DECAY_CONFIG.dustSpawnChance() ) { handleDust( pLevel, floorBlockPos ); }
            }

            this.dustTimer = getDustTimer();
        } else { this.dustTimer--; }

        if ( this.cobwebTimer <= 0 ) {
            for ( BlockPos cornerBlockPos : this.cornerBlocks ) {
                if ( random.nextFloat() < DECAY_CONFIG.cobwebSpawnChance() ) { handleCobweb( pLevel, cornerBlockPos ); }
            }

            this.cobwebTimer = getCobwebTimer();
        } else { this.cobwebTimer--; }

        if ( this.mothTimer <= 0 ) {
            if ( random.nextFloat() < DECAY_CONFIG.mothSpawnChance() ) { handleMoth( pLevel ); }

            this.mothTimer = getMothTimer();
        } else { this.mothTimer--; }

        if ( this.bugTimer <= 0 ) {
            if ( random.nextFloat() < DECAY_CONFIG.bugSpawnChance() ) { handleBug( pLevel ); }

            this.bugTimer = getBugTimer();
        } else { this.bugTimer--; }
    }



    private void handleMonitor( Level pLevel ) {
        for ( BlockPos blockPos : this.offeringCupBlocks ) {
            BlockState blockState = pLevel.getBlockState( blockPos );

            if ( blockState.getBlock() instanceof OfferingCupBlock ) { continue; }
            else { this.removeOfferingBlock( blockPos ); }
        }

        for ( BlockPos blockPos : this.decayBlocks ) {
            BlockState blockState = pLevel.getBlockState( blockPos );

            if ( blockState.is( Blocks.COBWEB ) || blockState.getBlock() instanceof DustBlock ) { continue; }
            else { removeBlock( this.decayBlocksIndexOf, this.decayBlocks, blockPos ); }
        }

        for ( UUID uuid : this.decayMobs ) {
            Entity entity = pLevel.getEntity( uuid );

            if ( entity instanceof Moth || entity instanceof Bug ) { continue; }
            else{ this.removeDecayMob( uuid ); }
        }

        float respect = 0.0F;
        float comfort = 0.0F;
        if ( this.decayBlocks.isEmpty() && this.decayMobs.isEmpty() ) {
            respect = 0.03F;
            comfort = 0.01F;
        } else {
            respect = -0.05F;
            comfort = -0.01F;
        }

        if ( this.domovoiData.getDomovoiUUID() == null ) {
            this.domovoiData.updateRespect( respect );
            this.domovoiData.updateComfort( comfort );
        } else {
            Entity entity = pLevel.getEntity( this.domovoiData.getDomovoiUUID() );

            if ( entity instanceof Domovoi domovoi ) {
                domovoi.updateRespect( respect );
                domovoi.updateComfort( comfort );
            }
        }
    }



    private boolean isNight( Level pLevel ) {
        long time = pLevel.getOverworldClockTime() % 24_000L;
        return time >= 13_000L && time <= 23_000;
    }

    private void protectHome( ServerLevel pLevel ) {
        double radius = 20.0D;

        AABB area = new AABB( this.getBlockPos() ).inflate( radius );

        List<Monster> monsters = pLevel.getEntitiesOfClass(
                Monster.class,
                area,
                Monster::isAlive
        );

        Vec3 hearthPos = Vec3.atCenterOf( this.getBlockPos() );

        for ( Monster monster : monsters ) {
            Vec3 fleePos = LandRandomPos.getPosAway(
                    monster,
                    16, 17,
                    hearthPos
            );
            if ( fleePos == null ) { continue; }
            TheDomovoi.LOGGER.info( "monster fleeing" );

            monster.setTarget( null );
            monster.getNavigation().moveTo(
                    fleePos.x, fleePos.y, fleePos.x,
                    1.35
            );
        }
    }



    private void createDomovoi( Level pLevel, Domovoi.InitialGoalIntent pInitialGoalIntent ) {
        Domovoi domovoi = ModEntities.DOMOVOI.get().create( pLevel, EntitySpawnReason.EVENT );
        if ( domovoi == null ) { return; }

        domovoi.setHearthBlockPos( this.getBlockPos() );
        domovoi.setRespect( this.domovoiData.getRespect() );
        domovoi.setComfort( this.domovoiData.getComfort() );
        domovoi.setInitialGoalIntent( pInitialGoalIntent );

        BlockPos blockPos = getPos( pLevel, new ArrayList<>( this.floorBlocks ) );
        if ( blockPos == null ) { return; }

        domovoi.setPos( blockPos.getX(), blockPos.getY(), blockPos.getZ() );

        this.domovoiData.setDomovoiUUID( domovoi.getUUID() );
        pLevel.addFreshEntity( domovoi );
        TheDomovoi.LOGGER.info( "created domovoi" );
    }


    public void handleDomovoiReturn( Domovoi pDomovoi ) {
        this.domovoiData.setDomovoiUUID( null );
        this.domovoiData.setRespect( pDomovoi.getRespect() );
        this.domovoiData.setComfort( pDomovoi.getComfort() );

        pDomovoi.discard();
    }

    private void handleDomovoi( Level pLevel ) {
        if ( this.domovoiData.getDomovoiUUID() != null ) { return; }

        if ( this.domovoiTimer <= 0 ) {
            for ( BlockPos blockPos : this.offeringCupBlocks ) {
                boolean hasMilk     = OfferingCupBlock.getHasMilk( pLevel, blockPos );
                boolean hasBread    = OfferingCupBlock.getHasBread( pLevel, blockPos );

                if ( hasMilk || hasBread ) {
                    createDomovoi( pLevel, Domovoi.InitialGoalIntent.RECEIVE_OFFERINGS );

                    this.domovoiTimer = getDomovoiTimer();
                    return;
                }
            }

            if ( random.nextFloat() < this.domovoiData.getRespect() ) {
                if ( this.isNight( pLevel ) && pLevel instanceof ServerLevel serverLevel )
                { this.protectHome( serverLevel ); }

                if ( !this.decayMobs.isEmpty() )
                { TheDomovoi.LOGGER.info( "hunting" ); createDomovoi( pLevel, Domovoi.InitialGoalIntent.HUNTING ); }
                else if ( !this.decayBlocks.isEmpty() )
                { TheDomovoi.LOGGER.info( "cleaning" ); createDomovoi( pLevel, Domovoi.InitialGoalIntent.CLEANING ); }
            }

            this.domovoiTimer = getDomovoiTimer();
        } else { this.domovoiTimer--; }
    }



    private boolean hasDirectAccessToSky( Level pLevel, BlockPos pBlockPos ) {
        int blockingHeight = pLevel.getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                pBlockPos.getX(),
                pBlockPos.getZ()
        );

        return pBlockPos.getY() >= blockingHeight;
    }



    private void handleNodeDiscovery( Level pLevel, BlockPos pBlockPos ) {
        if ( this.hasDirectAccessToSky( pLevel, pBlockPos ) ) { return; }

        for ( Direction layer0Direction : Direction.values() ) {
            BlockPos neighborBlockPos = pBlockPos.relative( layer0Direction );
            BlockState neighborBlockState = pLevel.getBlockState( neighborBlockPos );

            if ( neighborBlockState.is( Blocks.AIR ) && !homeDataRecord.contains( neighborBlockPos ) ) {
                homeDataRecord.add( neighborBlockPos );
                homeDataQueue.offer( neighborBlockPos );
            }

            for ( Direction layer1Direction : Direction.values() ) {
                BlockPos neighborsNeighborBlockPos = neighborBlockPos.relative( layer1Direction );
                BlockState neighborsNeighborBlockState = pLevel.getBlockState( neighborsNeighborBlockPos );

                if (
                        neighborsNeighborBlockState.is( Blocks.AIR )
                                && !homeDataRecord.contains( neighborsNeighborBlockPos )
                ) {
                    homeDataRecord.add( neighborsNeighborBlockPos );
                    homeDataQueue.offer( neighborsNeighborBlockPos );
                }
            }
        }
    }

    private void handleNodeScanCompleted() {
        homeState = HomeState.COMPUTED;
        homeDataRecord.clear();

        TheDomovoi.LOGGER.info( "floor: {}", this.floorBlocks.size() );
        TheDomovoi.LOGGER.info( "corner: {}", this.cornerBlocks.size() );
        TheDomovoi.LOGGER.info( "offering cups: {}", this.offeringCupBlocks.size() );
    }

    private void handleNodeScan( Level pLevel ) {
        if ( homeDataQueue.isEmpty() ) {
            this.handleNodeScanCompleted();
            return;
        }

        int recordElementsPerHome = 6000;
        int queueElementsPerTick = 150;
        for ( int i = 0; i < queueElementsPerTick; i++ ) {
            if ( homeDataRecord.size() >= recordElementsPerHome ) {
                this.handleNodeScanCompleted();
                return;
            }

            BlockPos airBlock = homeDataQueue.poll();
            if ( airBlock == null ) { continue; }

            if ( this.hasDirectAccessToSky( pLevel, airBlock ) ) { continue; }

            int nonPassableNeighbors = 0;
            for ( Direction direction : Direction.values() ) {
                BlockPos neighborBlockPos = airBlock.relative( direction );
                BlockState neighborBlockState = pLevel.getBlockState( neighborBlockPos );
                Block neighborBlock = neighborBlockState.getBlock();

                if ( neighborBlockState.is( Blocks.AIR ) && !homeDataRecord.contains( neighborBlockPos ) ) {
                    homeDataRecord.add( neighborBlockPos );
                    homeDataQueue.offer( neighborBlockPos );

                    continue;
                } else if (
                        neighborBlock instanceof OfferingCupBlock
                                && !this.offeringCupIndexOf.containsKey( neighborBlockPos )
                ) {
                    this.addOfferingBlock( neighborBlockPos );

                    continue;
                } else if ( neighborBlock instanceof DoorBlock || neighborBlock instanceof TrapDoorBlock ) {
                    handleNodeDiscovery( pLevel, neighborBlockPos );

                    continue;
                }

                VoxelShape neighborCollisionShape = neighborBlockState.getCollisionShape( pLevel, neighborBlockPos );
                if ( Block.isShapeFullBlock( neighborCollisionShape ) ) {
                    if ( direction == Direction.DOWN ) { floorBlocks.add( airBlock ); }

                    nonPassableNeighbors++;
                }
            }

            if ( nonPassableNeighbors >= 2 ) { cornerBlocks.add( airBlock ); }
        }
    }




    private void clientTick() {}

    private void serverTick( Level pLevel ) {
        switch ( homeState ) {
            case UNCOMPUTED:
                handleNodeDiscovery( pLevel, this.getBlockPos() );
                homeState = HomeState.COMPUTING;

                break;
            case COMPUTING:
                handleNodeScan( pLevel );

                break;
            case COMPUTED:
                handleDecay( pLevel );
                handleDomovoi( pLevel );

                if ( this.homeTimer <= 0 ) { this.reset(); this.homeTimer = getHomeTimer(); }
                else { this.homeTimer--; }

                if ( this.monitorTimer <= 0 ) { handleMonitor( pLevel ); this.monitorTimer = getMonitorTimer(); }
                else { this.monitorTimer--; }

                break;
        }
    }

    public static void tick(
            Level pLevel,
            BlockPos pBlockPos,
            BlockState pBlockState,
            DomovoiHearthBlockEntity pBlockEntity
    ) {
        if ( pBlockState.getValue( DomovoiHearthBlock.LIT ) ) {
            if ( pLevel.isClientSide() ) { pBlockEntity.clientTick(); }
            else { pBlockEntity.serverTick( pLevel ); }
        }
    }
}
