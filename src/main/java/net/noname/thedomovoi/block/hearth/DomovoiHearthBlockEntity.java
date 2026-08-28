package net.noname.thedomovoi.block.hearth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
            0.1F,
            0.1F,
           10,
            1.0F,
            0.3F
    );

    public List<BlockPos> decayBlocks = new ArrayList<>();
    public Map<BlockPos, Integer> decayBlocksIndexOf = new HashMap<>();

    public List<UUID> decayMobs = new ArrayList<>();
    public Map<UUID, Integer> decayMobsIndexOf = new HashMap<>();

    private int dustTimer;
    private int cobwebTimer;
    private int mothTimer;
    private int bugTimer;

    public DomovoiHearthBlockEntity( BlockPos worldPosition, BlockState blockState ) {
        super( ModBlocks.DOMOVOI_HEARTH_BLOCK_ENTITY.get(), worldPosition, blockState );

        this.random         = RandomSource.create();

        this.homeTimer      = getHomeTimer();

        this.monitorTimer   = getMonitorTimer();

        this.domovoiData = new DomovoiData();
        this.domovoiData.setRespect( 1.0F );
        this.domovoiData.setComfort( 0.8F );

        this.domovoiTimer   = getDomovoiTimer();

        this.dustTimer      = getDustTimer();
        this.cobwebTimer    = getCobwebTimer();
        this.mothTimer      = getMothTimer();
        this.bugTimer       = getBugTimer();
    }



    @Override
    protected void saveAdditional( @NonNull ValueOutput output ) {
        super.saveAdditional(output);

        if ( this.getOwnerUUID() != null ) {
            output.store( "owner", UUIDUtil.CODEC, this.getOwnerUUID() );
        }

        output.putString( "home_state", this.homeState.name() );

        if ( this.homeState != HomeState.COMPUTED ) {
            output.store( "home_record", BlockPos.CODEC.listOf(), new ArrayList<>( this.homeDataRecord ) );
            output.store ( "home_queue", BlockPos.CODEC.listOf(), new ArrayList<>( this.homeDataQueue ) );
        }

        output.store( "floor_blocks",                   BlockPos.CODEC.listOf(),    this.floorBlocks );
        output.store( "corner_blocks",                  BlockPos.CODEC.listOf(),    this.cornerBlocks );
        output.store( "offering_cup_blocks",            BlockPos.CODEC.listOf(),    this.offeringCupBlocks );
        output.store( "decay_blocks",                   BlockPos.CODEC.listOf(),    this.decayBlocks );
        output.store( "decay_mobs",                     UUIDUtil.CODEC.listOf(),    this.decayMobs);

        output.store( "offering_cup_blocks_index_of",   BLOCK_MAP_CODEC,            this.offeringCupIndexOf );
        output.store( "decay_blocks_index_of",          BLOCK_MAP_CODEC,            this.decayBlocksIndexOf );
        output.store( "decay_mobs_index_of",            MOB_MAP_CODEC,              this.decayMobsIndexOf );


        this.domovoiData.saveData( output );

        output.putInt( "home_timer",    this.homeTimer );
        output.putInt( "monitor_timer", this.monitorTimer );
        output.putInt( "domovoi_timer", this.domovoiTimer );
        output.putInt( "dust_timer",    this.dustTimer );
        output.putInt( "cobweb_timer",  this.cobwebTimer );
        output.putInt( "moth_timer",    this.mothTimer );
        output.putInt( "bug_timer",     this.bugTimer );
    }

    @Override
    protected void loadAdditional( @NonNull ValueInput input ) {
        super.loadAdditional(input);

        this.ownerUUID = input.read( "owner", UUIDUtil.CODEC ).orElse( null );

        String homeState    = input.getStringOr( "home_state", HomeState.UNCOMPUTED.name() );
        this.homeState      = HomeState.valueOf( homeState );

        if ( this.homeState != HomeState.COMPUTED ) {
            this.homeDataRecord = new HashSet<>(
                    input.read( "home_record",  BlockPos.CODEC.listOf() ).orElse( List.of() )
            );

            this.homeDataQueue = new ArrayDeque<>(
                    input.read( "home_queue",   BlockPos.CODEC.listOf() ).orElse( List.of() )
            );
        }

        this.floorBlocks        = new ArrayList<>(
                                        input.read( "floor_blocks", BlockPos.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );
        this.cornerBlocks       = new ArrayList<>(
                                        input.read( "corner_blocks", BlockPos.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );
        this.offeringCupBlocks  = new ArrayList<>(
                                        input.read( "offering_cup_blocks", BlockPos.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );
        this.decayBlocks        = new ArrayList<>(
                                        input.read( "decay_blocks", BlockPos.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );
        this.decayMobs          = new ArrayList<>(
                                        input.read( "decay_mobs", UUIDUtil.CODEC.listOf() )
                                                .orElse( new ArrayList<>() )
                                );

        this.offeringCupIndexOf = new HashMap<>(
                                        input.read( "offering_cup_blocks_index_of", BLOCK_MAP_CODEC )
                                                .orElse( Collections.emptyMap() )
                                );
        this.decayBlocksIndexOf = new HashMap<>(
                                        input.read( "decay_blocks_index_of", BLOCK_MAP_CODEC )
                                                .orElse( Collections.emptyMap() )
                                );
        this.decayMobsIndexOf   = new HashMap<>(
                                        input.read( "decay_mobs_index_of", MOB_MAP_CODEC )
                                                .orElse( Collections.emptyMap() )
                                );


        UUID domovoiUUID        = input.read( "domovoi_uuid", UUIDUtil.CODEC ).orElse( null );
        float respect           = input.getFloatOr( "respect", 0.1F );
        float comfort           = input.getFloatOr( "comfort", 0.0F );

        this.domovoiData = new DomovoiData();
        this.domovoiData.setDomovoiUUID( domovoiUUID );
        this.domovoiData.setRespect( respect );
        this.domovoiData.setComfort( comfort );

        this.homeTimer          = input.getIntOr( "home_timer",     this.getHomeTimer() );
        this.monitorTimer       = input.getIntOr( "monitor_timer",  this.getMonitorTimer() );
        this.domovoiTimer       = input.getIntOr( "domovoi_timer",  this.getDomovoiTimer() );
        this.dustTimer          = input.getIntOr( "dust_timer",     this.getDustTimer() );
        this.cobwebTimer        = input.getIntOr( "cobweb_timer",   this.getCobwebTimer() );
        this.mothTimer          = input.getIntOr( "moth_timer",     this.getMothTimer() );
        this.bugTimer           = input.getIntOr( "bug_timer",      this.getBugTimer() );
    }



    private void reset() {
        this.homeState = HomeState.UNCOMPUTED;

        this.homeDataRecord.clear();
        this.homeDataQueue.clear();

        this.floorBlocks.clear();
        this.cornerBlocks.clear();

        this.offeringCupBlocks.clear();
    }



    public UUID getOwnerUUID() { return this.ownerUUID; }
    public void setOwnerUUID( UUID pUUID ) { this.ownerUUID = pUUID; this.setChanged(); }



    private int getHomeTimer()
    { return random.nextInt( 30 * Time.DAY.getTicks(), 60 * Time.DAY.getTicks() ); }


    private int getMonitorTimer()
    { return random.nextInt( Time.DAY.getTicks(), 3 * Time.DAY.getTicks() ); }


    private int getDomovoiTimer()
    { return random.nextInt( 1 * Time.MINUTE.getTicks(), 2 * Time.MINUTE.getTicks() ); }


    private int getDustTimer()
    { return random.nextInt( 2 * Time.DAY.getTicks(), 3 * Time.DAY.getTicks() ); }

    private int getCobwebTimer()
    { return random.nextInt( 3 * Time.DAY.getTicks(), 6 * Time.DAY.getTicks() ); }

    private int getMothTimer()
    { return random.nextInt( 1 * Time.MINUTE.getTicks(), 2 * Time.MINUTE.getTicks() ); }

    private int getBugTimer()
    { return random.nextInt( 4 * Time.DAY.getTicks(), 8 * Time.DAY.getTicks() ); }



    private BlockPos getPos( Level pLevel, List<BlockPos> pPositions ) {
        Collections.shuffle( pPositions );

        for ( BlockPos blockPos : pPositions ) {
            if ( pLevel.getBlockState( blockPos ).getCollisionShape( pLevel, blockPos ).isEmpty() ) {
                return blockPos;
            }
        }
        return null;
    }



    private void addBlock( Map<BlockPos, Integer> indexOf, List<BlockPos> blocks, BlockPos pBlockPos ) {
        if ( indexOf.containsKey( pBlockPos ) ) { return; }

        indexOf.put( pBlockPos, blocks.size() );
        blocks.add( pBlockPos );
    }

    private void removeBlock( Map<BlockPos, Integer> indexOf, List<BlockPos> blocks, BlockPos pBlockPos ) {
        Integer idx = indexOf.remove( pBlockPos );
        if ( idx == null ) { return; }

        int lastIdx = blocks.size() - 1;
        BlockPos lastElement = blocks.get( lastIdx );

        blocks.set( idx, lastElement );
        indexOf.put( lastElement, idx );
        blocks.remove( lastIdx );
    }

    public void addDecayBlock( BlockPos blockPos )
    { this.addBlock( this.decayBlocksIndexOf, this.decayBlocks, blockPos ); }
    public void removeDecayBlock( BlockPos blockPos )
    { this.removeBlock( this.decayBlocksIndexOf, this.decayBlocks, blockPos ); }

    public void addOfferingBlock( BlockPos blockPos )
    { this.addBlock( this.offeringCupIndexOf, this.offeringCupBlocks, blockPos ); }
    public void removeOfferingBlock( BlockPos blockPos )
    { this.removeBlock( this.offeringCupIndexOf, this.offeringCupBlocks, blockPos ); }


    public void addMob( Map<UUID, Integer> indexOf, List<UUID> uuids, UUID pUUID ) {
        if ( indexOf.containsKey( pUUID ) ) { return; }

        indexOf.put( pUUID, uuids.size() );
        uuids.add( pUUID );
    }

    public void removeMob( Map<UUID, Integer> indexOf, List<UUID> uuids, UUID pUUID ) {
        Integer idx = indexOf.remove( pUUID );
        if ( idx == null ) { return; }

        int lastIdx = uuids.size() - 1;
        UUID lastElement = uuids.get( lastIdx );

        uuids.set( idx, lastElement );
        indexOf.put( lastElement, idx );
        uuids.remove( lastIdx );
    }

    public void addDecayMob( UUID uuid ) { this.addMob( this.decayMobsIndexOf, this.decayMobs, uuid ); }
    public void removeDecayMob( UUID uuid ) { this.removeMob( this.decayMobsIndexOf, this.decayMobs, uuid ); }



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
        for ( BlockPos blockPos : this.decayBlocks ) {
            BlockState blockState = pLevel.getBlockState( blockPos );

            if ( blockState.is( Blocks.COBWEB ) || blockState.getBlock() instanceof DustBlock ) { continue; }
            else { removeBlock( this.decayBlocksIndexOf, this.decayBlocks, blockPos ); }
        }

        // check decay state -> update domovoi respect/comfort
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
        TheDomovoi.LOGGER.info( "domovoi created" );


        this.domovoiTimer = getDomovoiTimer();
    }


    public void handleDomovoiReturn( Domovoi pDomovoi ) {
        TheDomovoi.LOGGER.info( "domovoi returning" );
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
                    TheDomovoi.LOGGER.info( "waking domovoi to get offerings" );
                    createDomovoi( pLevel, Domovoi.InitialGoalIntent.RECEIVE_OFFERINGS );

                    return;
                }
            }

            if ( random.nextFloat() < this.domovoiData.getRespect() ) {
                if ( !this.decayMobs.isEmpty() )
                { TheDomovoi.LOGGER.info( "waking domovoi to hunt" ); createDomovoi( pLevel, Domovoi.InitialGoalIntent.HUNTING ); }
                else if ( !this.decayBlocks.isEmpty() )
                { TheDomovoi.LOGGER.info( "waking domovoi to clean" ); createDomovoi( pLevel, Domovoi.InitialGoalIntent.CLEANING ); }
            }

            this.domovoiTimer = getDomovoiTimer();
        } else { this.domovoiTimer--; }
    }



    private void handleNodeDiscovery( Level pLevel, BlockPos pBlockPos ) {
        TheDomovoi.LOGGER.info( "HandleNodeDiscovery" );

        if ( pLevel.canSeeSky( pBlockPos ) ) { TheDomovoi.LOGGER.info(
                "Discovery aborted: hearth can see sky at {}",
                pBlockPos
        );return; }

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

        TheDomovoi.LOGGER.info( "found {} air blocks", this.homeDataQueue.size() );
    }

    private void handleNodeScanCompleted() {
        homeState = HomeState.COMPUTED;
        homeDataRecord.clear();

        TheDomovoi.LOGGER.info( "Node Scan complete" );
        TheDomovoi.LOGGER.info( "{} floor blocks", this.floorBlocks.size() );
        TheDomovoi.LOGGER.info( "{} corner blocks", this.floorBlocks.size() );
        TheDomovoi.LOGGER.info( "{} offering cups", this.offeringCupBlocks.size() );
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

            if ( pLevel.canSeeSky( airBlock ) ) { continue; }

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
                    this.addBlock( this.offeringCupIndexOf, this.offeringCupBlocks, neighborBlockPos );

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
