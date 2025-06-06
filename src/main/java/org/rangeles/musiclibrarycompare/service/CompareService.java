package org.rangeles.musiclibrarycompare.service;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.rangeles.musiclibrarycompare.model.SongEntry;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CompareService {

    private static final double SIMILARITY_THRESHOLD = 0.90;
    private static final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public List<SongEntry> findMissingSongs(List<SongEntry> spotifyList, List<SongEntry> localList) {
        List<SongEntry> missing = new ArrayList<>();

        for (SongEntry spotifySong : spotifyList) {
            double bestScore = 0.0;

            for (SongEntry localSong : localList) {
                double score = computeSimilarity(spotifySong, localSong);
                if (score > bestScore) {
                    bestScore = score;
                }

                if (score >= SIMILARITY_THRESHOLD) {
                    bestScore = 1.0;
                    break;
                }
            }

            if (bestScore < SIMILARITY_THRESHOLD) {
                spotifySong.setMatchScore(bestScore);
                missing.add(spotifySong);
            }
        }

        return missing;
    }

    private double computeSimilarity(SongEntry a, SongEntry b) {
        String title1 = normalize(a.getTitle());
        String title2 = normalize(b.getTitle());
        String artist1 = normalize(a.getArtist());
        String artist2 = normalize(b.getArtist());

        double titleScore = similarity.apply(title1, title2);
        double artistScore = similarity.apply(artist1, artist2);

        return (titleScore + artistScore) / 2.0;
    }

    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }
}