package org.rangeles.musiclibrarycompare.service;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.rangeles.musiclibrarycompare.model.ComparisonResult;
import org.rangeles.musiclibrarycompare.model.ComparisonResultBundle;
import org.rangeles.musiclibrarycompare.model.SongEntry;
import org.rangeles.musiclibrarycompare.model.SongEntryList;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;

import java.util.*;

@Service
public class CompareService {

    private static final Logger logger = LoggerFactory.getLogger(CompareService.class);
    private final ParsingService parsingService;
    private final XmlMapper xmlMapper;

    private static final double SIMILARITY_THRESHOLD = 0.90;
    private static final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public CompareService(ParsingService parsingService) {
        this.parsingService = parsingService;
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.registerModule(new JavaTimeModule());
        this.xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.xmlMapper.enable(SerializationFeature.INDENT_OUTPUT); //pretty print
    }

    // This method needs to be checked against the existing ComparisonResultBundle constructor
    // For now, let's focus on the individual find methods and export methods.
    public ComparisonResultBundle performComparison(List<SongEntry> spotifyList, List<SongEntry> localList) {
        List<SongEntry> commonSongs = findCommonSongs(spotifyList, localList);
        List<SongEntry> spotifyOnlySongs = findSpotifyOnlySongs(spotifyList, localList); // Renamed
        List<SongEntry> localOnlySongs = findLocalOnlySongs(spotifyList, localList);     // Renamed

        // Ensure the ComparisonResultBundle constructor takes these in the correct order
        // Based on its usage in CompareController, it's: commonSongs, uniqueLocalSongs (now localOnlySongs), missingSongs (now spotifyOnlySongs)
        return new ComparisonResultBundle(commonSongs, localOnlySongs, spotifyOnlySongs);
    }


    // Renamed from findMissingSongs
    public List<SongEntry> findSpotifyOnlySongs(List<SongEntry> spotifyList, List<SongEntry> localList) {
        List<SongEntry> spotifyOnly = new ArrayList<>();

        for (SongEntry spotifySong : spotifyList) {
            boolean found = false;
            for (SongEntry localSong : localList) {
                if (isSimilar(spotifySong, localSong)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                spotifyOnly.add(spotifySong);
            }
        }
        return spotifyOnly;
    }

    public List<SongEntry> findCommonSongs(List<SongEntry> spotifyList, List<SongEntry> localList) {
        List<SongEntry> common = new ArrayList<>();
        Set<String> matchedLocalSongs = new HashSet<>(); // To avoid double counting local songs

        for (SongEntry spotifySong : spotifyList) {
            for (SongEntry localSong : localList) {
                String localSongIdentifier = localSong.getTitle() + " - " + localSong.getArtist();
                if (isSimilar(spotifySong, localSong) && !matchedLocalSongs.contains(localSongIdentifier)) {
                    common.add(spotifySong);
                    matchedLocalSongs.add(localSongIdentifier);
                    break;
                }
            }
        }
        return common;
    }

    // Renamed from findUniqueSongs
    public List<SongEntry> findLocalOnlySongs(List<SongEntry> spotifyList, List<SongEntry> localList) {
        List<SongEntry> localOnly = new ArrayList<>();

        for (SongEntry localSong : localList) {
            boolean found = false;
            for (SongEntry spotifySong : spotifyList) {
                if (isSimilar(localSong, spotifySong)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                localOnly.add(localSong);
            }
        }
        return localOnly;
    }

    private boolean isSimilar(SongEntry song1, SongEntry song2) {
        String title1 = song1.getTitle() != null ? song1.getTitle().toLowerCase() : "";
        String artist1 = song1.getArtist() != null ? song1.getArtist().toLowerCase() : "";
        String title2 = song2.getTitle() != null ? song2.getTitle().toLowerCase() : "";
        String artist2 = song2.getArtist() != null ? song2.getArtist().toLowerCase() : "";

        double scoreTitle = similarity.apply(title1, title2);
        double scoreArtist = similarity.apply(artist1, artist2);

        return scoreTitle >= SIMILARITY_THRESHOLD && scoreArtist >= SIMILARITY_THRESHOLD;
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    // Parameter uniqueSongs changed to localOnlySongs for clarity in comparison export
    public byte[] exportComparisonToCsv(List<SongEntry> commonSongs, List<SongEntry> localOnlySongs) { // Parameter changed
        StringBuilder csvContent = new StringBuilder();

        //BOM Utf-8 compatibility for Excel
        csvContent.append('\ufeff');

        csvContent.append("--- Common Songs ---\n");
        csvContent.append("\"Title\",\"Artist\",\"Album\"\n");
        if (commonSongs != null) {
            for (SongEntry song : commonSongs) {
                csvContent.append(convertToCsvLine(song)).append("\n");
            }
        }
        csvContent.append("\n");

        csvContent.append("--- Local Only Songs ---\n"); // Header changed
        csvContent.append("\"Title\",\"Artist\",\"Album\"\n");
        if (localOnlySongs != null) { // Parameter changed
            for (SongEntry song : localOnlySongs) {
                csvContent.append(convertToCsvLine(song)).append("\n");
            }
        }

        return csvContent.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String convertToCsvLine(SongEntry song) {
        return String.format("\"%s\",\"%s\",\"%s\"",
                escapeCsv(song.getTitle()),
                escapeCsv(song.getArtist()),
                escapeCsv(song.getAlbum()));
    }

    private String escapeCsv(String field) {
        if (field == null) {
            return "";
        }
        return field.replace("\"", "\"\"");
    }

    // Parameter uniqueSongs changed to localOnlySongs for clarity in comparison export
    public byte[] exportComparisonToXml(List<SongEntry> commonSongs, List<SongEntry> localOnlySongs) throws IOException { // Parameter changed
        logger.info("Exporting comparison results to XML using JacksonXmlMapper");

        // Assuming ComparisonResult constructor takes (common, unique/localOnly)
        ComparisonResult comparisonResult = new ComparisonResult(commonSongs, localOnlySongs); // Parameter changed

        try {
            return xmlMapper.writeValueAsBytes(comparisonResult);
        } catch (Exception e) {
            logger.error("Error converting comparison results to XML using JacksonXmlMapper", e);
            throw new IOException("Error converting comparison results to XML", e);
        }
    }

    // Renamed from exportMissingSongsToXml
    public byte[] exportSpotifyOnlySongsToXml(List<SongEntry> spotifyOnlySongs) throws IOException { // Parameter changed
        logger.info("Exporting {} Spotify-only songs to XML using JacksonXmlMapper", spotifyOnlySongs.size()); // Log message changed
        SongEntryList songEntryList = new SongEntryList(spotifyOnlySongs); // Parameter changed
        try {
            return xmlMapper.writeValueAsBytes(songEntryList);
        } catch (Exception e) {
            logger.error("Error converting Spotify-only songs to XML using JacksonXmlMapper", e); // Log message changed
            throw new IOException("Error converting Spotify-only songs to XML using JacksonXmlMapper", e); // Error message changed
        }
    }

    // Renamed from exportUniqueSongsToCsv
    public byte[] exportLocalOnlySongsToCsv(List<SongEntry> localOnlySongs) { // Parameter changed
        StringBuilder csvContent = new StringBuilder();
        csvContent.append('\ufeff'); // BOM Utf-8 compatibility for Excel
        csvContent.append("Title,Artist,Album\n");
        if (localOnlySongs != null) {
            for (SongEntry song : localOnlySongs) {
                csvContent.append(convertToCsvLine(song)).append("\n");
            }
        }
        return csvContent.toString().getBytes(StandardCharsets.UTF_8);
    }

    // Renamed from exportUniqueSongsToXml
    public byte[] exportLocalOnlySongsToXml(List<SongEntry> localOnlySongs) throws IOException { // Parameter changed
        logger.info("Exporting {} local-only songs to XML using JacksonXmlMapper", localOnlySongs.size()); // Log message changed
        SongEntryList songEntryList = new SongEntryList(localOnlySongs); // Parameter changed
        try {
            return xmlMapper.writeValueAsBytes(songEntryList);
        } catch (Exception e) {
            logger.error("Error converting local-only songs to XML using JacksonXmlMapper", e); // Log message changed
            throw new IOException("Error converting local-only songs to XML using JacksonXmlMapper", e); // Error message changed
        }
    }
}