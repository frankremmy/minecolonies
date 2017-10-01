package com.minecolonies.coremod.colony;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.configuration.Configurations;
import com.minecolonies.api.entity.Citizen;
import com.minecolonies.api.entity.ai.basic.AbstractAISkeleton;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.Log;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.colony.buildings.AbstractBuildingWorker;
import com.minecolonies.coremod.colony.buildings.BuildingHome;
import com.minecolonies.coremod.colony.jobs.AbstractJob;
import com.minecolonies.coremod.entity.EntityCitizen;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Extra data for Citizens.
 */
public class CitizenData implements ICitizenData
{
    /**
     * Maximum saturation of a citizen.
     */
    public static final int MAX_SATURATION = 10;
    private static final float  MAX_HEALTH              = 20.0F;
    /**
     * Max level of an attribute a citizen may initially have.
     */

    private static final int    LETTERS_IN_THE_ALPHABET = 26;
    /**
     * Tags.
     */
    private static final String TAG_ID                  = "id";
    private static final String TAG_NAME                = "name";
    private static final String TAG_FEMALE              = "female";
    private static final String TAG_TEXTURE             = "texture";
    private static final String TAG_LEVEL               = "level";
    private static final String TAG_EXPERIENCE          = "experience";
    private static final String TAG_HEALTH              = "health";
    private static final String TAG_MAX_HEALTH          = "maxHealth";
    private static final String TAG_SKILLS              = "skills";
    private static final String TAG_SKILL_STRENGTH      = "strength";
    private static final String TAG_SKILL_STAMINA       = "endurance";
    private static final String TAG_SKILL_SPEED         = "charisma";
    private static final String TAG_SKILL_INTELLIGENCE  = "intelligence";
    private static final String TAG_SKILL_DEXTERITY     = "dexterity";
    private static final String TAG_SATURATION          = "saturation";
    /**
     * Minimum saturation of a citizen.
     */
    private static final int MIN_SATURATION = 0;
    /**
     * The unique citizen id.
     */
    private final int id;
    /**
     * The colony the citizen belongs to.
     */
    private final Colony colony;
    /**
     * The name of the citizen.
     */
    private String name;
    /**
     * Boolean gender, true = female, false = male.
     */
    private boolean female;
    /**
     * The id of the citizens texture.
     */
    private int textureId;
    /**
     * The home building of the citizen.
     */
    @Nullable
    private BuildingHome homeBuilding;

    /**
     * The work building of the citizen.
     */
    @Nullable
    private AbstractBuildingWorker workBuilding;

    /**
     * The job of the citizen.
     */
    private IJob job;

    /**
     * If the citizen is dirty (Has to be updated on client side).
     */
    private boolean dirty;

    /**
     * Its entitity.
     */
    @Nullable
    private Citizen entity;

    /**
     * Attributes, which influence the workers behaviour.
     * May be added more later.
     */
    private int    strength;
    private int    endurance;
    private int    charisma;
    private int    intelligence;
    private int    dexterity;
    private double health;
    private double maxHealth;

    /**
     * The citizens saturation at the current moment.
     */
    private double saturation;

    /**
     * The current experience level the citizen is on.
     */
    private int level = 0;

    /**
     * The total amount of experience the citizen has.
     * This also includes the amount of experience within their Experience Bar.
     */
    private double experience;

    /**
     * Create a CitizenData given an ID.
     * Used as a super-constructor or during loading.
     *
     * @param id     ID of the Citizen.
     * @param colony Colony the Citizen belongs to.
     */
    public CitizenData(final int id, final Colony colony)
    {
        this.id = id;
        this.colony = colony;
    }

    /**
     * Creates CitizenData from tag compound.
     *
     * @param compound NBT compound to build from.
     * @param colony   Colony of the citizen.
     * @return CitizenData.
     */
    @NotNull
    public static CitizenData createFromNBT(@NotNull final NBTTagCompound compound, final Colony colony)
    {
        final int id = compound.getInteger(TAG_ID);
        final @NotNull CitizenData citizen = new CitizenData(id, colony);
        citizen.readFromNBT(compound);
        return citizen;
    }

    /**
     * Reads data from NBT-tag compound.
     *
     * @param compound NBT-Tag compound.
     */
    public void readFromNBT(@NotNull final NBTTagCompound compound)
    {
        name = compound.getString(TAG_NAME);
        female = compound.getBoolean(TAG_FEMALE);
        textureId = compound.getInteger(TAG_TEXTURE);

        //  Attributes
        level = compound.getInteger(TAG_LEVEL);
        experience = compound.getInteger(TAG_EXPERIENCE);
        health = compound.getFloat(TAG_HEALTH);
        maxHealth = compound.getFloat(TAG_MAX_HEALTH);

        final NBTTagCompound nbtTagSkillsCompound = compound.getCompoundTag("skills");
        strength = nbtTagSkillsCompound.getInteger("strength");
        endurance = nbtTagSkillsCompound.getInteger("endurance");
        charisma = nbtTagSkillsCompound.getInteger("charisma");
        intelligence = nbtTagSkillsCompound.getInteger("intelligence");
        dexterity = nbtTagSkillsCompound.getInteger("dexterity");
        saturation = compound.getDouble(TAG_SATURATION);

        if (compound.hasKey("job"))
        {
            setJob(AbstractJob.createFromNBT(this, compound.getCompoundTag("job")));
        }
    }

    @Override
    @Nullable
    public Citizen getCitizen()
    {
        return entity;
    }

    @Override
    public void setCitizenEntity(final Citizen citizen)
    {
        entity = citizen;
        markDirty();
    }

    /**
     * Returns the colony of the citizen.
     *
     * @return colony of the citizen.
     */
    @Override
    public IColony getColony()
    {
        return colony;
    }

    /**
     * Returns the id of the citizen.
     *
     * @return id of the citizen.
     */
    @Override
    public int getId()
    {
        return id;
    }

    @Override
    public void initializeFromEntity(@NotNull final Citizen entity)
    {
        final Random rand = entity.getRNG();

        this.entity = entity;

        //Assign the gender before name
        female = rand.nextBoolean();
        name = generateName(rand);

        textureId = entity.world.rand.nextInt(Integer.MAX_VALUE);
        health = entity.getHealth();
        maxHealth = entity.getMaxHealth();
        experience = 0;
        level = 0;
        saturation = MAX_SATURATION;
        int levelCap = (int) colony.getOverallHappiness();
        @NotNull final Random random = new Random();

        if (levelCap <= 1)
        {
            intelligence = 1;
            charisma = 1;
            strength = 1;
            endurance = 1;
            dexterity = 1;
        }
        else
        {
            intelligence = random.nextInt(levelCap - 1) + 1;
            charisma = random.nextInt(levelCap - 1) + 1;
            strength = random.nextInt(levelCap - 1) + 1;
            endurance = random.nextInt(levelCap - 1) + 1;
            dexterity = random.nextInt(levelCap - 1) + 1;
        }
        //Initialize the citizen skills and make sure they are never 0

        markDirty();
    }

    /**
     * Returns the name of the citizen.
     *
     * @return name of the citizen.
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Returns true if citizen is female, false for male.
     *
     * @return true for female, false for male.
     */
    @Override
    public boolean isFemale()
    {
        return female;
    }

    /**
     * Returns the texture id for the citizen.
     *
     * @return texture ID.
     */
    @Override
    public int getTextureId()
    {
        return textureId;
    }

    @Override
    @Nullable
    public IBuilding getWorkBuilding()
    {
        return workBuilding;
    }

    /**
     * Sets the work building of a citizen.
     *
     * @param building work building.
     */
    public void setWorkBuilding(@Nullable final AbstractBuildingWorker building)
    {
        if (workBuilding != null && building != null && workBuilding != building)
        {
            throw new IllegalStateException("CitizenData.setWorkBuilding() - already assigned a work building when setting a new work building");
        }
        else if (workBuilding != building)
        {
            workBuilding = building;

            if (workBuilding != null)
            {
                //  We have a place to work, do we have the assigned Job?
                if (job == null)
                {
                    //  No job, create one!
                    setJob(workBuilding.createJob(this));
                    colony.getWorkManager().clearWorkForCitizen(this);
                }
            }
            else if (job != null)
            {
                final Citizen citizen = getCitizen();
                if (citizen != null)
                {
                    citizen.tasks.removeTask(citizen.tasks.taskEntries.stream().filter(task -> task.action instanceof AbstractAISkeleton).findFirst().orElse(null).action);
                }
                //  No place of employment, get rid of our job
                setJob(null);
                colony.getWorkManager().clearWorkForCitizen(this);
            }

            markDirty();
        }
    }

    @Override
    public IJob getJob()
    {
        return job;
    }

    @Override
    public void setJob(final IJob job)
    {
        this.job = job;

        @Nullable final Citizen localEntity = getCitizen();
        if (localEntity != null)
        {
            localEntity.onJobChanged(job);
        }

        markDirty();
    }

    /**
     * Returns the level of the citizen.
     *
     * @return level of the citizen.
     */
    @Override
    public int getLevel()
    {
        return level;
    }

    /**
     * Sets the level of the citizen.
     *
     * @param lvl the new level for the citizen.
     */
    public void setLevel(final int lvl)
    {
        this.level = lvl;
    }

    /**
     * Returns the experience of the citizen.
     *
     * @return experience of the citizen.
     */
    @Override
    public double getExperience()
    {
        return experience;
    }

    /**
     * Strength getter.
     *
     * @return citizen Strength value.
     */
    @Override
    public int getStrength()
    {
        return strength;
    }

    /**
     * Endurance getter.
     *
     * @return citizen Endurance value.
     */
    @Override
    public int getEndurance()
    {
        return endurance;
    }

    /**
     * Charisma getter.
     *
     * @return citizen Charisma value.
     */
    @Override
    public int getCharisma()
    {
        return charisma;
    }

    /**
     * Intelligence getter.
     *
     * @return citizen Intelligence value.
     */
    @Override
    public int getIntelligence()
    {
        return intelligence;
    }

    /**
     * Dexterity getter.
     *
     * @return citizen Dexterity value.
     */
    @Override
    public int getDexterity()
    {
        return dexterity;
    }

    /**
     * Getter for the saturation.
     *
     * @return the saturation.
     */
    @Override
    public double getSaturation()
    {
        return this.saturation;
    }

    /**
     * Marks the instance dirty.
     */
    public void markDirty()
    {
        dirty = true;
        colony.markCitizensDirty();
    }

    /**
     * Create a CitizenData View given it's saved NBTTagCompound.
     *
     * @param id  The citizen's id.
     * @param buf The network data.
     * @return View object of the citizen.
     */
    @Nullable
    public static CitizenDataView createCitizenDataView(final int id, final ByteBuf buf)
    {
        @Nullable CitizenDataView citizenDataView = new CitizenDataView(id);

        try
        {
            citizenDataView.deserialize(buf);
        }
        catch (final RuntimeException ex)
        {
            Log.getLogger().error(String.format("A CitizenData.View for #%d has thrown an exception during loading, its state cannot be restored. Report this to the mod author",
              citizenDataView.getId()), ex);
            citizenDataView = null;
        }

        return citizenDataView;
    }

    /**
     * Returns a random element in a list.
     *
     * @param rand  Random object.
     * @param array Array to select from.
     * @return Random element from array.
     */
    private static String getRandomElement(@NotNull final Random rand, @NotNull final String[] array)
    {
        return array[rand.nextInt(array.length)];
    }

    /**
     * Returns a random capital letter from the alphabet.
     *
     * @param rand Random object.
     * @return Random capital letter.
     */
    private static char getRandomLetter(@NotNull final Random rand)
    {
        return (char) (rand.nextInt(LETTERS_IN_THE_ALPHABET) + 'A');
    }

    /**
     * Generates a random name from a set of names.
     *
     * @param rand Random object.
     * @return Name of the citizen.
     */
    private String generateName(@NotNull final Random rand)
    {
        String citizenName;
        if (female)
        {
            citizenName = String.format("%s %s. %s", getRandomElement(rand, Configurations.femaleFirstNames), getRandomLetter(rand),
              getRandomElement(rand, Configurations.lastNames));
        }
        else
        {
            citizenName = String.format("%s %s. %s", getRandomElement(rand, Configurations.maleFirstNames), getRandomLetter(rand),
              getRandomElement(rand, Configurations.lastNames));
        }
        for (int i = 1; i <= this.getColony().getMaxCitizens(); i++)
        {
            if (this.getColony().getCitizen(i) != null && this.getColony().getCitizen(i).getName().equals(citizenName))
            {
                citizenName = generateName(rand);
            }
        }
        return citizenName;
    }

    @Override
    public void addExperience(final double xp)
    {
        this.experience += xp;
    }

    @Override
    public void increaseLevel()
    {
        this.level += 1;
    }

    @Override
    public boolean isDirty()
    {
        return dirty;
    }

    @Override
    public void clearDirty()
    {
        dirty = false;
    }

    /**
     * When a building is destroyed, inform the citizen so it can do any cleanup
     * of associations that the building's. own AbstractBuilding.onDestroyed did
     * not do.
     *
     * @param building building that is destroyed.
     */
    public void onRemoveBuilding(final AbstractBuilding building)
    {
        if (getHomeBuilding() == building)
        {
            setHomeBuilding(null);
        }

        if (getWorkBuilding() == building)
        {
            setWorkBuilding(null);
        }
    }

    /**
     * Returns the home building of the citizen.
     *
     * @return home building.
     */
    @Nullable
    public BuildingHome getHomeBuilding()
    {
        return homeBuilding;
    }

    /**
     * Sets the home of the citizen.
     *
     * @param building home building.
     */
    public void setHomeBuilding(@Nullable final BuildingHome building)
    {
        if (homeBuilding != null && building != null && homeBuilding != building)
        {
            throw new IllegalStateException("CitizenData.setHomeBuilding() - already assigned a home building when setting a new home building");
        }
        else if (homeBuilding != building)
        {
            homeBuilding = building;
            markDirty();
        }
    }

    /**
     * Sets {@link EntityCitizen} to null for the instance.
     */
    public void clearCitizenEntity()
    {
        entity = null;
    }

    /**
     * Writes the citiizen data to an NBT-compound.
     *
     * @param compound NBT-Tag compound.
     */
    public void writeToNBT(@NotNull final NBTTagCompound compound)
    {
        compound.setInteger(TAG_ID, id);
        compound.setString(TAG_NAME, name);
        compound.setBoolean(TAG_FEMALE, female);
        compound.setInteger(TAG_TEXTURE, textureId);

        //  Attributes
        compound.setInteger(TAG_LEVEL, level);
        compound.setDouble(TAG_EXPERIENCE, experience);
        compound.setDouble(TAG_HEALTH, health);
        compound.setDouble(TAG_MAX_HEALTH, maxHealth);


        @NotNull final NBTTagCompound nbtTagSkillsCompound = new NBTTagCompound();
        nbtTagSkillsCompound.setInteger(TAG_SKILL_STRENGTH, strength);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_STAMINA, endurance);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_SPEED, charisma);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_INTELLIGENCE, intelligence);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_DEXTERITY, dexterity);
        compound.setTag(TAG_SKILLS, nbtTagSkillsCompound);
        compound.setDouble(TAG_SATURATION, saturation);

        if (job != null)
        {
            @NotNull final NBTTagCompound jobCompound = new NBTTagCompound();
            job.writeToNBT(jobCompound);
            compound.setTag("job", jobCompound);
        }
    }

    /**
     * Writes the citizen data to a byte buf for transition.
     *
     * @param buf Buffer to write to.
     */
    public void serializeViewNetworkData(@NotNull final ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeBoolean(female);

        buf.writeInt(entity != null ? entity.getEntityId() : -1);

        buf.writeInt(colony.getWorld().provider.getDimension());
        ByteBufUtils.writeTag(buf, StandardFactoryController.getInstance().serialize(colony.getID()));

        buf.writeBoolean(homeBuilding != null);
        if (homeBuilding != null)
        {
            BlockPosUtil.writeToByteBuf(buf, homeBuilding.getLocation().getInDimensionLocation());
        }

        buf.writeBoolean(workBuilding != null);
        if (workBuilding != null)
        {
            BlockPosUtil.writeToByteBuf(buf, workBuilding.getLocation().getInDimensionLocation());
        }

        //  Attributes
        buf.writeInt(getLevel());
        buf.writeDouble(getExperience());

        //If entity is null assume the standard values as health
        if (entity == null)
        {
            buf.writeFloat(MAX_HEALTH);
            buf.writeFloat(MAX_HEALTH);
        }
        else
        {
            buf.writeFloat(entity.getHealth());
            buf.writeFloat(entity.getMaxHealth());
        }

        buf.writeInt(getStrength());
        buf.writeInt(getEndurance());
        buf.writeInt(getCharisma());
        buf.writeInt(getIntelligence());
        buf.writeInt(getDexterity());
        buf.writeDouble(getSaturation());

        ByteBufUtils.writeUTF8String(buf, (job != null) ? job.getName() : "");
    }

    /**
     * Getter for the saturation.
     *
     * @param extraSaturation the extra saturation
     */
    public void increaseSaturation(final double extraSaturation)
    {
        this.saturation = Math.min(MAX_SATURATION, this.saturation + Math.abs(extraSaturation));
    }

    /**
     * Getter for the saturation.
     *
     * @param extraSaturation the saturation to remove.
     */
    public void decreaseSaturation(final double extraSaturation)
    {
        this.saturation = Math.max(MIN_SATURATION, this.saturation - Math.abs(extraSaturation));
    }

    /**
     * Resets the experience and the experience level of the citizen.
     */
    public void resetExperienceAndLevel()
    {
        this.level = 0;
        this.experience = 0;
    }
}
