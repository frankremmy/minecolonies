package com.minecolonies.entity;

import com.minecolonies.client.model.ModelEntityCitizenFemaleCitizen;
import com.minecolonies.lib.Constants;
import net.minecraft.client.model.ModelBiped;

import java.util.Random;

public enum EnumCitizenLevel
{
    SETTLERMALE(new ModelBiped(), 0, "Male", "/textures/entity/EntitySettler"),
    CITIZENMALE(new ModelBiped(), 1, "Male", "/textures/entity/EntityCitizen"),
    NOBLEMALE(new ModelBiped(), 2, "Male", "/textures/entity/EntityNoble"),
    ARISTOCRATMALE(new ModelBiped(), 3, "Male", "/textures/entity/EntityAristocrat"),
    SETTLERFEMALE(new ModelEntityCitizenFemaleCitizen(), 0, "Female", "/textures/entity/EntitySettler"),
    CITIZENFEMALE(new ModelEntityCitizenFemaleCitizen(), 1, "Female", "/textures/entity/EntityCitizen"),
    NOBLEFEMALE(new ModelBiped(), 2, "Female", "/textures/entity/EntityNoble"), //TODO ADD MODEL
    ARISTOCRATFEMALE(new ModelBiped(), 3, "Female", "/textures/entity/EntityAristocrat"); //TODO ADD MODEL

    private final ModelBiped model;
    private final int        level;
    private final String     partialTextureString;
    private final int        sexInt;
    private final String     sexString;
    Random random = new Random();

    EnumCitizenLevel(ModelBiped modelBiped, int level, String sex, String textureLocationPart)
    {
        this.model = modelBiped;
        this.level = level;
        this.sexInt = sex.equalsIgnoreCase("Male") ? 0 : 1;
        this.sexString = sex;
        this.partialTextureString = setTexture(textureLocationPart);
    }

    public ModelBiped getModel(EnumCitizenLevel level)
    {
        return level.model;
    }

    public int getLevel(EnumCitizenLevel level)
    {
        return level.level;
    }

    public int getSexInt()
    {
        return sexInt;
    }

    public String getSexString()
    {
        return sexString;
    }

    public String getTexture(EnumCitizenLevel level)
    {
        return level.partialTextureString;
    }

    public String setTexture(String texture)
    {
        return Constants.MODID + ":" + texture + sexString;
    }
}