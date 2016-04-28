package clashsoft.cslib.world;

import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraft.world.gen.NoiseGenerator;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.fml.common.eventhandler.Event;

public class CustomChunkProvider implements IChunkProvider
{
	public Random					random;
	
	public NoiseGeneratorOctaves	noiseGen1;
	public NoiseGeneratorOctaves	noiseGen2;
	public NoiseGeneratorOctaves	noiseGen3;
	public NoiseGeneratorOctaves	noiseGen4;
	public NoiseGeneratorOctaves	noiseGen5;
	public NoiseGeneratorOctaves	noiseGen6;
	public NoiseGeneratorPerlin		noiseGenPerlin;
	
	public World					worldObj;
	public WorldType				worldType;
	
	public final boolean			mapFeaturesEnabled;
	public BiomeGenBase[]			biomesForGeneration;
	
	public MapGenBase				caveGenerator				= new MapGenCaves();
	public MapGenStronghold			strongholdGenerator			= new MapGenStronghold();
	public MapGenVillage			villageGenerator			= new MapGenVillage();
	public MapGenMineshaft			mineshaftGenerator			= new MapGenMineshaft();
	public MapGenScatteredFeature	scatteredFeatureGenerator	= new MapGenScatteredFeature();
	public MapGenRavine				ravineGenerator				= new MapGenRavine();
	
	public float[]					parabolicField				= new float[25];
	public double[]					noiseArray					= new double[825];
	public double[]					stoneNoise					= new double[256];
	
	public double[]					noiseField1;
	public double[]					noiseField2;
	public double[]					noiseField3;
	public double[]					noiseField4;
	
	public CustomChunkProvider(World world, long seed, boolean mapFeatures)
	{
		this.worldObj = world;
		this.mapFeaturesEnabled = mapFeatures;
		this.worldType = world.getWorldInfo().getTerrainType();
		this.random = new Random(seed);
		
		this.noiseGen1 = new NoiseGeneratorOctaves(this.random, 16);
		this.noiseGen2 = new NoiseGeneratorOctaves(this.random, 16);
		this.noiseGen3 = new NoiseGeneratorOctaves(this.random, 8);
		this.noiseGen4 = new NoiseGeneratorOctaves(this.random, 10);
		this.noiseGen5 = new NoiseGeneratorOctaves(this.random, 16);
		this.noiseGen6 = new NoiseGeneratorOctaves(this.random, 8);
		this.noiseGenPerlin = new NoiseGeneratorPerlin(this.random, 4);
		
		for (int j = -2; j <= 2; ++j)
		{
			for (int k = -2; k <= 2; ++k)
			{
				float f = 10.0F / MathHelper.sqrt_float(j * j + k * k + 0.2F);
				this.parabolicField[j + 2 + (k + 2) * 5] = f;
			}
		}
		
		NoiseGenerator[] noiseGens = { this.noiseGen1, this.noiseGen2, this.noiseGen3, this.noiseGenPerlin, this.noiseGen4, this.noiseGen5, this.noiseGen6 };
		noiseGens = TerrainGen.getModdedNoiseGenerators(world, this.random, noiseGens);
		this.noiseGen1 = (NoiseGeneratorOctaves) noiseGens[0];
		this.noiseGen2 = (NoiseGeneratorOctaves) noiseGens[1];
		this.noiseGen3 = (NoiseGeneratorOctaves) noiseGens[2];
		this.noiseGenPerlin = (NoiseGeneratorPerlin) noiseGens[3];
		this.noiseGen4 = (NoiseGeneratorOctaves) noiseGens[4];
		this.noiseGen5 = (NoiseGeneratorOctaves) noiseGens[5];
		this.noiseGen6 = (NoiseGeneratorOctaves) noiseGens[6];
	}
	
	public void generate(int x, int z, ChunkPrimer primer)
	{
		this.biomesForGeneration = this.worldObj.getWorldChunkManager().getBiomesForGeneration(this.biomesForGeneration, x * 4 - 2, z * 4 - 2, 10, 10);
		this.generateNoises(x << 2, 0, z << 2);
		
		IBlockState stoneState = Blocks.stone.getDefaultState();
		IBlockState waterState = Blocks.water.getDefaultState();
		
		double d0 = 0.125D;
		double d9 = 0.25D;
		double d14 = 0.25D;
		short short1 = 256;
		
		for (int x1 = 0; x1 < 4; ++x1)
		{
			int l = x1 * 5;
			int i1 = (x1 + 1) * 5;
			
			for (int z1 = 0; z1 < 4; ++z1)
			{
				int k1 = (l + z1) * 33;
				int l1 = (l + z1 + 1) * 33;
				int i2 = (i1 + z1) * 33;
				int j2 = (i1 + z1 + 1) * 33;
				
				for (int y1 = 0; y1 < 32; ++y1)
				{
					double d1 = this.noiseArray[k1 + y1];
					double d2 = this.noiseArray[l1 + y1];
					double d3 = this.noiseArray[i2 + y1];
					double d4 = this.noiseArray[j2 + y1];
					double d5 = (this.noiseArray[k1 + y1 + 1] - d1) * d0;
					double d6 = (this.noiseArray[l1 + y1 + 1] - d2) * d0;
					double d7 = (this.noiseArray[i2 + y1 + 1] - d3) * d0;
					double d8 = (this.noiseArray[j2 + y1 + 1] - d4) * d0;
					
					for (int y2 = 0; y2 < 8; ++y2)
					{
						double d10 = d1;
						double d11 = d2;
						double d12 = (d3 - d1) * d9;
						double d13 = (d4 - d2) * d9;
						
						for (int y3 = 0; y3 < 4; ++y3)
						{
							int j3 = y3 + x1 * 4 << 12 | z1 * 4 << 8 | y1 * 8 + y2;
							j3 -= short1;
							double d16 = (d11 - d10) * d14;
							double d15 = d10 - d16;
							
							for (int k3 = 0; k3 < 4; ++k3)
							{
								if ((d15 += d16) > 0.0D)
								{
									primer.setBlockState(j3 += short1, stoneState);
								}
								else if (y1 * 8 + y2 < 63)
								{
									primer.setBlockState(j3 += short1, waterState);
								}
							}
							
							d10 += d12;
							d11 += d13;
						}
						
						d1 += d5;
						d2 += d6;
						d3 += d7;
						d4 += d8;
					}
				}
			}
		}
	}
	
	public void replaceBlocksForBiome(int x, int z, BiomeGenBase[] biomes, ChunkPrimer primer)
	{
		ChunkProviderEvent.ReplaceBiomeBlocks event = new ChunkProviderEvent.ReplaceBiomeBlocks(this, x, z, primer, this.worldObj);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.getResult() == Event.Result.DENY)
			return;
		
		this.stoneNoise = this.noiseGenPerlin.func_151599_a(this.stoneNoise, x << 4, z << 4, 16, 16, 0.0625, 0.0625, 1.0D);
		
		for (int k = 0; k < 16; ++k)
		{
			for (int l = 0; l < 16; ++l)
			{
				BiomeGenBase biome = biomes[l + k * 16];
				biome.genTerrainBlocks(this.worldObj, this.random, primer, (x << 4) + k, (z << 4) + l, this.stoneNoise[l + k * 16]);
			}
		}
	}
	
	@Override
	public Chunk func_177459_a(BlockPos pos)
	{
		return this.provideChunk(pos.getX() >> 4, pos.getY() >> 4);
	}
	
	@Override
	public Chunk provideChunk(int x, int z)
	{
		this.random.setSeed(x * 341873128712L + z * 132897987541L);
		
		ChunkPrimer primer = new ChunkPrimer();
		this.generate(x, z, primer);
		this.biomesForGeneration = this.worldObj.getWorldChunkManager().loadBlockGeneratorData(this.biomesForGeneration, x * 16, z * 16, 16, 16);
		this.replaceBlocksForBiome(x, z, this.biomesForGeneration, primer);
		
		this.mapGen(this.caveGenerator, this.worldObj, x, z, primer);
		this.mapGen(this.ravineGenerator, this.worldObj, x, z, primer);
		
		if (this.mapFeaturesEnabled)
		{
			this.mapGen(this.mineshaftGenerator, this.worldObj, x, z, primer);
			this.mapGen(this.villageGenerator, this.worldObj, x, z, primer);
			this.mapGen(this.strongholdGenerator, this.worldObj, x, z, primer);
			this.mapGen(this.scatteredFeatureGenerator, this.worldObj, x, z, primer);
		}
		
		Chunk chunk = new Chunk(this.worldObj, primer, x, z);
		byte[] biomes = chunk.getBiomeArray();
		
		for (int k = 0; k < biomes.length; ++k)
		{
			biomes[k] = (byte) this.biomesForGeneration[k].biomeID;
		}
		
		chunk.generateSkylightMap();
		return chunk;
	}
	
	public void mapGen(MapGenBase mapGen, World world, int chunkX, int chunkZ, ChunkPrimer primer)
	{
		if (mapGen != null)
		{
			mapGen.func_175792_a(null, world, chunkX, chunkZ, primer);
		}
	}
	
	public void generateNoises(int x, int y, int z)
	{
		this.noiseField1 = this.noiseGen1.generateNoiseOctaves(this.noiseField1, x, y, z, 5, 33, 5, 684.412D, 684.412D, 684.412D);
		this.noiseField2 = this.noiseGen2.generateNoiseOctaves(this.noiseField2, x, y, z, 5, 33, 5, 684.412D, 684.412D, 684.412D);
		this.noiseField3 = this.noiseGen3.generateNoiseOctaves(this.noiseField3, x, y, z, 5, 33, 5, 8.555D, 4.278D, 8.555D);
		this.noiseField4 = this.noiseGen5.generateNoiseOctaves(this.noiseField4, x, z, 5, 5, 200.0D, 200.0D, 0.5D);
		int l = 0;
		int i1 = 0;
		
		for (int j1 = 0; j1 < 5; ++j1)
		{
			for (int k1 = 0; k1 < 5; ++k1)
			{
				float f = 0.0F;
				float f1 = 0.0F;
				float f2 = 0.0F;
				byte b0 = 2;
				BiomeGenBase biomegenbase = this.biomesForGeneration[j1 + 2 + (k1 + 2) * 10];
				
				for (int l1 = -b0; l1 <= b0; ++l1)
				{
					for (int i2 = -b0; i2 <= b0; ++i2)
					{
						BiomeGenBase biome = this.biomesForGeneration[j1 + l1 + 2 + (k1 + i2 + 2) * 10];
						float height = biome.minHeight;
						float heightVar = biome.maxHeight;
						
						if (this.worldType == WorldType.AMPLIFIED && height > 0.0F)
						{
							height = 1.0F + height * 2.0F;
							heightVar = 1.0F + heightVar * 4.0F;
						}
						
						float f5 = this.parabolicField[l1 + 2 + (i2 + 2) * 5] / (height + 2.0F);
						
						if (height > biomegenbase.minHeight)
						{
							f5 /= 2.0F;
						}
						
						f += heightVar * f5;
						f1 += height * f5;
						f2 += f5;
					}
				}
				
				f /= f2;
				f1 /= f2;
				f = f * 0.9F + 0.1F;
				f1 = (f1 * 4.0F - 1.0F) / 8.0F;
				double d13 = this.noiseField4[i1] / 8000.0D;
				
				if (d13 < 0.0D)
				{
					d13 = -d13 * 0.3D;
				}
				
				d13 = d13 * 3.0D - 2.0D;
				
				if (d13 < 0.0D)
				{
					d13 /= 2.0D;
					
					if (d13 < -1.0D)
					{
						d13 = -1.0D;
					}
					
					d13 /= 1.4D;
					d13 /= 2.0D;
				}
				else
				{
					if (d13 > 1.0D)
					{
						d13 = 1.0D;
					}
					
					d13 /= 8.0D;
				}
				
				++i1;
				double d12 = f1;
				double d14 = f;
				d12 += d13 * 0.2D;
				d12 = d12 * 8.5D / 8.0D;
				double d5 = 8.5D + d12 * 4.0D;
				
				for (int j2 = 0; j2 < 33; ++j2)
				{
					double d6 = (j2 - d5) * 12.0D * 128.0D / 256.0D / d14;
					
					if (d6 < 0.0D)
					{
						d6 *= 4.0D;
					}
					
					double d7 = this.noiseField1[l] / 512.0D;
					double d8 = this.noiseField2[l] / 512.0D;
					double d9 = (this.noiseField3[l] / 10.0D + 1.0D) / 2.0D;
					double d10 = MathHelper.denormalizeClamp(d7, d8, d9) - d6;
					
					if (j2 > 29)
					{
						double d11 = (j2 - 29) / 3.0F;
						d10 = d10 * (1.0D - d11) + -10.0D * d11;
					}
					
					this.noiseArray[l] = d10;
					++l;
				}
			}
		}
	}
	
	@Override
	public boolean chunkExists(int x, int z)
	{
		return true;
	}
	
	@Override
	public void populate(IChunkProvider chunkProvider, int x, int z)
	{
		BlockFalling.fallInstantly = true;
		x <<= 4;
		z <<= 4;
		
		BiomeGenBase biome = this.worldObj.getBiomeGenForCoords(new BlockPos(x + 16, 0, z + 16));
		this.random.setSeed(this.worldObj.getSeed());
		long i1 = this.random.nextLong() / 2L * 2L + 1L;
		long j1 = this.random.nextLong() / 2L * 2L + 1L;
		this.random.setSeed(x * i1 + z * j1 ^ this.worldObj.getSeed());
		boolean flag = false;
		
		MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(chunkProvider, this.worldObj, this.random, x, z, flag));
		
		if (this.mapFeaturesEnabled)
		{
			ChunkCoordIntPair coords = new ChunkCoordIntPair(x, z);
			this.mineshaftGenerator.func_175794_a(this.worldObj, this.random, coords);
			flag = this.villageGenerator.func_175794_a(this.worldObj, this.random, coords);
			this.strongholdGenerator.func_175794_a(this.worldObj, this.random, coords);
			this.scatteredFeatureGenerator.func_175794_a(this.worldObj, this.random, coords);
		}
		
		if (biome != BiomeGenBase.desert && biome != BiomeGenBase.desertHills && !flag && this.random.nextInt(4) == 0 && TerrainGen.populate(chunkProvider, this.worldObj, this.random, x, z, flag, PopulateChunkEvent.Populate.EventType.LAKE))
		{
			int k1 = x + this.random.nextInt(16) + 8;
			int l1 = this.random.nextInt(256);
			int i2 = z + this.random.nextInt(16) + 8;
			new WorldGenLakes(Blocks.water).generate(this.worldObj, this.random, new BlockPos(k1, l1, i2));
		}
		
		if (TerrainGen.populate(chunkProvider, this.worldObj, this.random, x, z, flag, PopulateChunkEvent.Populate.EventType.LAVA) && !flag && this.random.nextInt(8) == 0)
		{
			int k1 = x + this.random.nextInt(16) + 8;
			int l1 = this.random.nextInt(this.random.nextInt(248) + 8);
			int i2 = z + this.random.nextInt(16) + 8;
			
			if (l1 < 63 || this.random.nextInt(10) == 0)
			{
				new WorldGenLakes(Blocks.lava).generate(this.worldObj, this.random, new BlockPos(k1, l1, i2));
			}
		}
		
		boolean doGen = TerrainGen.populate(chunkProvider, this.worldObj, this.random, x, z, flag, PopulateChunkEvent.Populate.EventType.DUNGEON);
		for (int k1 = 0; doGen && k1 < 8; ++k1)
		{
			int l1 = x + this.random.nextInt(16) + 8;
			int i2 = this.random.nextInt(256);
			int j2 = z + this.random.nextInt(16) + 8;
			new WorldGenDungeons().generate(this.worldObj, this.random, new BlockPos(l1, i2, j2));
		}
		
		biome.func_180624_a(this.worldObj, this.random, new BlockPos(x, 0, z));
		
		x += 8;
		z += 8;
		SpawnerAnimals.performWorldGenSpawning(this.worldObj, biome, x, z, 16, 16, this.random);
		
		if (TerrainGen.populate(chunkProvider, this.worldObj, this.random, x, z, flag, PopulateChunkEvent.Populate.EventType.ICE))
		{
			for (int k1 = 0; k1 < 16; ++k1)
			{
				for (int l1 = 0; l1 < 16; ++l1)
				{
					BlockPos snowPos = this.worldObj.func_175725_q(new BlockPos(x + k1, 0, z + l1));
					BlockPos icePos = snowPos.offsetDown();
					
					if (this.worldObj.func_175675_v(icePos))
					{
						this.worldObj.setBlockState(icePos, Blocks.ice.getDefaultState(), 2);
					}
					
					if (this.worldObj.func_175708_f(snowPos, true))
					{
						this.worldObj.setBlockState(snowPos, Blocks.snow_layer.getDefaultState(), 2);
					}
				}
			}
		}
		
		MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(chunkProvider, this.worldObj, this.random, x, z, flag));
		
		BlockFalling.fallInstantly = false;
	}
	
	@Override
	public boolean func_177460_a(IChunkProvider chunkProvider, Chunk chunk, int x, int z)
	{
		if (this.mapFeaturesEnabled && chunk.getInhabitedTime() < 3600L)
			return this.strongholdGenerator.func_175794_a(this.worldObj, this.random, new ChunkCoordIntPair(x, z));
		
		return false;
	}
	
	@Override
	public boolean saveChunks(boolean flag, IProgressUpdate progressUpdate)
	{
		return true;
	}
	
	@Override
	public void saveExtraData()
	{
	}
	
	@Override
	public boolean unloadQueuedChunks()
	{
		return false;
	}
	
	@Override
	public boolean canSave()
	{
		return true;
	}
	
	@Override
	public String makeString()
	{
		return this.getClass().getSimpleName();
	}
	
	@Override
	public List func_177458_a(EnumCreatureType type, BlockPos pos)
	{
		BiomeGenBase biomegenbase = this.worldObj.getBiomeGenForCoords(pos);
		return type == EnumCreatureType.MONSTER && this.scatteredFeatureGenerator.func_175798_a(pos) ? this.scatteredFeatureGenerator.getScatteredFeatureSpawnList() : biomegenbase.getSpawnableList(type);
	}
	
	@Override
	public BlockPos func_180513_a(World world, String name, BlockPos pos)
	{
		if ("Stronghold".equals(name))
			return this.strongholdGenerator == null ? null : this.strongholdGenerator.func_180706_b(world, pos);
		return null;
	}
	
	@Override
	public int getLoadedChunkCount()
	{
		return 0;
	}
	
	@Override
	public void func_180514_a(Chunk chunk, int x, int z)
	{
		if (this.mapFeaturesEnabled)
		{
			this.mineshaftGenerator.func_175792_a(this, this.worldObj, x, z, null);
			this.villageGenerator.func_175792_a(this, this.worldObj, x, z, null);
			this.strongholdGenerator.func_175792_a(this, this.worldObj, x, z, null);
			this.scatteredFeatureGenerator.func_175792_a(this, this.worldObj, x, z, null);
		}
	}
}