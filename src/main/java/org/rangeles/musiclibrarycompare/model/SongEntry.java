package org.rangeles.musiclibrarycompare.model;

public class SongEntry {
    private String title;
    private String artist;
    private Double matchScore;

    public SongEntry() {}
    public SongEntry(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }
}