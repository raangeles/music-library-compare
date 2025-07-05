package org.rangeles.musiclibrarycompare.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "songs")
public class SongEntryList {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "song")
    private List<SongEntry> songs;

    public SongEntryList() {
    }

    public SongEntryList(List<SongEntry> songs) {
        this.songs = songs;
    }

    public List<SongEntry> getSongs() {
        return songs;
    }

    public void setSongs(List<SongEntry> songs) {
        this.songs = songs;
    }
}