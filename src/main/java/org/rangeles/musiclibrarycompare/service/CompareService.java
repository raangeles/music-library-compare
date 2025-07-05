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
import java.util.regex.Matcher; // Import Matcher
import java.util.regex.Pattern; // Import Pattern

@Service
public class CompareService {

    private static final Logger logger = LoggerFactory.getLogger(CompareService.class);
    private final ParsingService parsingService;
    private final XmlMapper xmlMapper;

    private static final double SIMILARITY_THRESHOLD = 0.90;
    private static final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    // Regex for matching common "feat" patterns: (feat. Artist), [feat. Artist], - feat. Artist, feat. Artist
    // It captures variations like "feat", "ft", "featuring", "f." and content within parentheses/brackets or after a dash.
    private static final Pattern FEAT_PATTERN = Pattern.compile(
            "\\b(feat|ft|featuring|f\\.)(\\s*\\.)?\\s*[^\\)]*?\\)|\\s*\\[(feat|ft|featuring|f\\.)[^\\)]*?\\]|\\s*-\\s*(feat|ft|featuring|f\\.)[^\\)]*?$|\\s*(feat|ft|featuring|f\\.)\\s+[\\w\\s.,&]+"
    );

    public CompareService(ParsingService parsingService) {
        this.parsingService = parsingService;
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.registerModule(new JavaTimeModule());
        this.xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.xmlMapper.enable(SerializationFeature.INDENT_OUTPUT); //pretty print
    }

    public ComparisonResultBundle performComparison(List<SongEntry> spotifyList, List<SongEntry> localList) {
        List<SongEntry> commonSongs = findCommonSongs(spotifyList, localList);
        List<SongEntry> spotifyOnlySongs = findSpotifyOnlySongs(spotifyList, localList); // Renamed
        List<SongEntry> localOnlySongs = findLocalOnlySongs(spotifyList, localList);     // Renamed

        return new ComparisonResultBundle(commonSongs, localOnlySongs, spotifyOnlySongs);
    }

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
        Set<String> matchedLocalSongs = new HashSet<>();

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
        // Normalize titles and artists before comparison
        String normalizedTitle1 = normalizeTitle(song1.getTitle());
        String normalizedArtist1 = normalizeArtistName(song1.getArtist());
        String normalizedTitle2 = normalizeTitle(song2.getTitle());
        String normalizedArtist2 = normalizeArtistName(song2.getArtist());

        double scoreTitle = similarity.apply(normalizedTitle1, normalizedTitle2);
        double scoreArtist = similarity.apply(normalizedArtist1, normalizedArtist2);

        return scoreTitle >= SIMILARITY_THRESHOLD && scoreArtist >= SIMILARITY_THRESHOLD;
    }

    // Added helper method for artist name normalisation
    // 1. Remove leading "the"
    // 2. Remove common punctuations and extra spaces
    private String normalizeArtistName(String artist) {
        if (artist == null) {
            return "";
        }
        String normalized = artist.toLowerCase();

        if (normalized.startsWith("the ")) {
            normalized = normalized.substring(4);
        }

        normalized = normalized.replaceAll("[.'()]", "").trim();
        normalized = normalized.replaceAll("\\s+", " "); // Replace multiple spaces with single space

        return normalized.trim(); // Final trim
    }

    // Added helper method for song title normalisation
    // 1. Expand ampersand (&) to "and"
    // 2. Removal of patterns like (feat Artist)
    private String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        String normalized = title.toLowerCase();

        normalized = normalized.replace("&", "and");

        Matcher matcher = FEAT_PATTERN.matcher(normalized);
        normalized = matcher.replaceAll(" ").trim(); // Replace matched patterns with a space, then trim
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    // Remove alphaneumeric characters
    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    public byte[] exportComparisonToCsv(List<SongEntry> commonSongs, List<SongEntry> localOnlySongs) {
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

    public byte[] exportComparisonToXml(List<SongEntry> commonSongs, List<SongEntry> localOnlySongs) throws IOException {
        logger.info("Exporting comparison results to XML using JacksonXmlMapper");

        ComparisonResult comparisonResult = new ComparisonResult(commonSongs, localOnlySongs);

        try {
            return xmlMapper.writeValueAsBytes(comparisonResult);
        } catch (Exception e) {
            logger.error("Error converting comparison results to XML using JacksonXmlMapper", e);
            throw new IOException("Error converting comparison results to XML", e);
        }
    }

    // Renamed from exportMissingSongsToXml
    public byte[] exportSpotifyOnlySongsToXml(List<SongEntry> spotifyOnlySongs) throws IOException {
        logger.info("Exporting {} Spotify-only songs to XML using JacksonXmlMapper", spotifyOnlySongs.size());
        SongEntryList songEntryList = new SongEntryList(spotifyOnlySongs);
        try {
            return xmlMapper.writeValueAsBytes(songEntryList);
        } catch (Exception e) {
            logger.error("Error converting Spotify-only songs to XML using JacksonXmlMapper", e);
            throw new IOException("Error converting Spotify-only songs to XML using JacksonXmlMapper", e);
        }
    }

    // Renamed from exportUniqueSongsToCsv
    public byte[] exportLocalOnlySongsToCsv(List<SongEntry> localOnlySongs) {
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
    public byte[] exportLocalOnlySongsToXml(List<SongEntry> localOnlySongs) throws IOException {
        logger.info("Exporting {} local-only songs to XML using JacksonXmlMapper", localOnlySongs.size());
        SongEntryList songEntryList = new SongEntryList(localOnlySongs);
        try {
            return xmlMapper.writeValueAsBytes(songEntryList);
        } catch (Exception e) {
            logger.error("Error converting local-only songs to XML using JacksonXmlMapper", e);
            throw new IOException("Error converting local-only songs to XML using JacksonXmlMapper", e);
        }
    }
}