package org.rangeles.musiclibrarycompare.model;

import java.util.List;

public class ComparisonResultBundle {
    private List<SongEntry> commonSongs;
    private List<SongEntry> uniqueSongs;
    private List<SongEntry> missingSongs;

    public ComparisonResultBundle(List<SongEntry> commonSongs, List<SongEntry> uniqueSongs, List<SongEntry> missingSongs) {
        this.commonSongs = commonSongs;
        this.uniqueSongs = uniqueSongs;
        this.missingSongs = missingSongs;
    }

    public List<SongEntry> getCommonSongs() {
        return commonSongs;
    }

    public void setCommonSongs(List<SongEntry> commonSongs) {
        this.commonSongs = commonSongs;
    }

    public List<SongEntry> getUniqueSongs() {
        return uniqueSongs;
    }

    public void setUniqueSongs(List<SongEntry> uniqueSongs) {
        this.uniqueSongs = uniqueSongs;
    }

    public List<SongEntry> getMissingSongs() {
        return missingSongs;
    }

    public void setMissingSongs(List<SongEntry> missingSongs) {
        this.missingSongs = missingSongs;
    }
}
