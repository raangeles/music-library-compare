package org.rangeles.musiclibrarycompare.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "comparisonResult")
public class ComparisonResult {

    @JacksonXmlProperty(localName = "commonSongs")
    private SongEntryList commonSongs;

    @JacksonXmlProperty(localName = "uniqueSongs")
    private SongEntryList uniqueSongs;

    public ComparisonResult() {
    }

    public ComparisonResult(List<SongEntry> commonSongs, List<SongEntry> uniqueSongs) {
        this.commonSongs = new SongEntryList(commonSongs);
        this.uniqueSongs = new SongEntryList(uniqueSongs);
    }

    public SongEntryList getCommonSongs() {
        return commonSongs;
    }

    public void setCommonSongs(SongEntryList commonSongs) {
        this.commonSongs = commonSongs;
    }

    public SongEntryList getUniqueSongs() {
        return uniqueSongs;
    }

    public void setUniqueSongs(SongEntryList uniqueSongs) {
        this.uniqueSongs = uniqueSongs;
    }
}