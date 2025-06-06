package org.rangeles.musiclibrarycompare.controller;

import org.rangeles.musiclibrarycompare.model.SongEntry;
import org.rangeles.musiclibrarycompare.service.CompareService;
import org.rangeles.musiclibrarycompare.service.ParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
public class CompareController {

    private static final Logger logger = LoggerFactory.getLogger(CompareController.class);

    @Autowired
    private ParsingService parsingService;

    @Autowired
    private CompareService compareService;

    @GetMapping("/")
    public String index() {
        logger.info("Accessing the index page");
        return "index";
    }

    @PostMapping("/compare")
    public String compare(@RequestParam("spotifyFile") MultipartFile spotifyFile,
                          @RequestParam("localFile") MultipartFile localFile,
                          Model model) {
        try {
            logger.info("Starting song comparison process");
            List<SongEntry> spotifySongs;
            List<SongEntry> localSongs;

            if (spotifyFile.getOriginalFilename().endsWith(".xml")) {
                logger.debug("Parsing Spotify file as XML");
                spotifySongs = parsingService.parseXml(spotifyFile.getInputStream(), spotifyFile.getOriginalFilename()); // Pass filename
            } else {
                logger.debug("Parsing Spotify file as CSV");
                spotifySongs = parsingService.parseCsv(spotifyFile.getInputStream());
            }

            if (localFile.getOriginalFilename().endsWith(".xml")) {
                logger.debug("Parsing local file as XML");
                localSongs = parsingService.parseXml(localFile.getInputStream(), localFile.getOriginalFilename()); // Pass filename
            } else {
                logger.debug("Parsing local file as CSV");
                localSongs = parsingService.parseCsv(localFile.getInputStream());
            }

            List<SongEntry> missingSongs = compareService.findMissingSongs(spotifySongs, localSongs);
            model.addAttribute("missingSongs", missingSongs);
            logger.info("Comparison completed successfully. Found {} missing songs.", missingSongs.size());

        } catch (IOException e) {
            logger.error("Error processing files: {}", e.getMessage(), e);
            model.addAttribute("error", "Error processing files: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during comparison: {}", e.getMessage(), e);
            model.addAttribute("error", "An unexpected error occurred.");
        }

        return "result";
    }


    @PostMapping("/scan")
    public String scanFolder(@RequestParam("folderPath") String folderPath, Model model) {
        try {
            logger.info("Starting folder scan for path: {}", folderPath);
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                logger.warn("Invalid folder path: {}", folderPath);
                throw new IllegalArgumentException("Invalid folder path");
            }
            List<SongEntry> localSongs = parsingService.scanMusicFolder(folder);
            model.addAttribute("scannedSongs", localSongs);
            logger.info("Folder scan completed. Found {} songs.", localSongs.size());
        } catch (Exception e) {
            logger.error("Error scanning folder: {}", e.getMessage(), e);
            model.addAttribute("error", "Error scanning folder: " + e.getMessage());
        }
        return "result";
    }
}