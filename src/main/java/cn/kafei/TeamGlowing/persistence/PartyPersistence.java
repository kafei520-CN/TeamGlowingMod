package cn.kafei.TeamGlowing.persistence;

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
            return;
        }

        try (Reader reader = Files.newBufferedReader(this.saveFilePath, StandardCharsets.UTF_8))
        {
            PartySaveData saveData = GSON.fromJson(reader, PartySaveData.class);
            partyManager.load(saveData);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Failed to load party data", exception);
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
            }
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Failed to save party data", exception);
        }
    }
}
