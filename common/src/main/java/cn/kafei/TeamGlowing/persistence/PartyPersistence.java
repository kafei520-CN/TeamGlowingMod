package cn.kafei.TeamGlowing.persistence;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.party.PartyManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PartyPersistence
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Path saveFilePath;

    public void setSaveFilePath(Path saveFilePath)
    {
        this.saveFilePath = saveFilePath;
    }

    public void load(PartyManager partyManager)
    {
        if (this.saveFilePath == null || !Files.exists(this.saveFilePath))
        {
            TeamGlowingConstants.LOGGER.info("Party data file not found at: {}", this.saveFilePath);
            return;
        }

        try (Reader reader = Files.newBufferedReader(this.saveFilePath, StandardCharsets.UTF_8))
        {
            PartySaveData saveData = GSON.fromJson(reader, PartySaveData.class);
            if (saveData != null) {
                partyManager.load(saveData);
                TeamGlowingConstants.LOGGER.info("Successfully loaded party data.");
            } else {
                TeamGlowingConstants.LOGGER.warn("Party data file is empty: {}", this.saveFilePath);
            }
        }
        catch (IOException exception)
        {
            TeamGlowingConstants.LOGGER.error("Failed to load party data", exception);
        }
    }

    public void save(PartyManager partyManager)
    {
        if (this.saveFilePath == null)
        {
            return;
        }

        try
        {
            Path parent = this.saveFilePath.getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(this.saveFilePath, StandardCharsets.UTF_8))
            {
                GSON.toJson(partyManager.toSaveData(), writer);
                TeamGlowingConstants.LOGGER.info("Successfully saved party data to: {}", this.saveFilePath);
            }
        }
        catch (IOException exception)
        {
            TeamGlowingConstants.LOGGER.error("Failed to save party data", exception);
        }
    }
}
