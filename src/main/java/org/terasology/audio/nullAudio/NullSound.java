package org.terasology.audio.nullAudio;

import org.terasology.asset.AbstractAsset;
import org.terasology.asset.AssetData;
import org.terasology.asset.AssetUri;
import org.terasology.audio.Sound;
import org.terasology.audio.SoundData;
import org.terasology.audio.StaticSound;

/**
 * @author Immortius
 */
public class NullSound extends AbstractAsset<SoundData> implements StaticSound {

    private int channels;
    private int sampleRate;
    private float length;

    public NullSound(AssetUri uri, SoundData data) {
        super(uri);
        reload(data);
    }

    @Override
    public float getLength() {
         return length;
    }

    @Override
    public int getChannels() {
        return channels;
    }

    @Override
    public int getSamplingRate() {
        return sampleRate;
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void reload(SoundData data) {
        this.channels = data.getChannels();
        this.sampleRate = data.getSampleRate();
        this.length = data.getData().limit() / getChannels() / (data.getBufferBits() / 8) / getSamplingRate();
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isDisposed() {
        return false;
    }
}