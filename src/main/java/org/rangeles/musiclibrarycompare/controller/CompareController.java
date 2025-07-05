package org.rangeles.musiclibrarycompare.controller;

import jakarta.servlet.http.HttpSession;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

import java.io.File;
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
                          Model model,
                          HttpSession session) {
        try {
            logger.info("Starting song comparison process");
            List<SongEntry> spotifySongs;
            List<SongEntry> localSongs;

            if (spotifyFile.getOriginalFilename().endsWith(".xml")) {
                logger.debug("Parsing Spotify file as XML");
                spotifySongs = parsingService.parseXml(spotifyFile.getInputStream(), spotifyFile.getOriginalFilename());
            } else {
                logger.debug("Parsing Spotify file as CSV");
                spotifySongs = parsingService.parseCsv(spotifyFile.getInputStream());
            }

            if (localFile.getOriginalFilename().endsWith(".xml")) {
                logger.debug("Parsing local file as XML");
                localSongs = parsingService.parseXml(localFile.getInputStream(), localFile.getOriginalFilename());
            } else {
                logger.debug("Parsing local file as CSV");
                localSongs = parsingService.parseCsv(localFile.getInputStream());
            }

            List<SongEntry> commonSongs = compareService.findCommonSongs(spotifySongs, localSongs);
            List<SongEntry> spotifyOnlySongs = compareService.findSpotifyOnlySongs(spotifySongs, localSongs); // Renamed
            List<SongEntry> localOnlySongs = compareService.findLocalOnlySongs(spotifySongs, localSongs);     // Renamed

            // Store all comparison results in the session with new names
            session.setAttribute("commonSongs", commonSongs);
            session.setAttribute("spotifyOnlySongs", spotifyOnlySongs); // Renamed
            session.setAttribute("localOnlySongs", localOnlySongs);     // Renamed

            model.addAttribute("commonSongs", commonSongs);
            model.addAttribute("spotifyOnlySongs", spotifyOnlySongs); // Renamed
            model.addAttribute("localOnlySongs", localOnlySongs);     // Renamed

            logger.info("Comparison complete. Common: {}, Spotify Only: {}, Local Only: {}",
                    commonSongs.size(), spotifyOnlySongs.size(), localOnlySongs.size());

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
    public String scanMusicFolder(
            @RequestParam("folderPath") String folderPath,
            Model model) {
        try {
            logger.info("Scanning local music folder: {}", folderPath);
            File folder = new File(folderPath);
            if (!folder.isDirectory()) {
                model.addAttribute("error", "Invalid folder path provided.");
                return "index";
            }
            List<SongEntry> scannedSongs = parsingService.scanMusicFolder(folder);
            model.addAttribute("scannedSongs", scannedSongs);
            logger.info("Scanned {} songs from folder: {}", scannedSongs.size(), folderPath);
            return "result";
        } catch (Exception e) {
            logger.error("Error scanning music folder", e);
            return "index"; // Redirect to index.html with error
        }
    }

    @GetMapping("/download/spotifyonly/xml") // New endpoint name
    public ResponseEntity<byte[]> downloadSpotifyOnlyXml(HttpSession session) { // Method name changed
        List<SongEntry> spotifyOnlySongs = (List<SongEntry>) session.getAttribute("spotifyOnlySongs"); // Variable name changed

        if (spotifyOnlySongs == null || spotifyOnlySongs.isEmpty()) {
            return ResponseEntity.badRequest().body("No Spotify-only songs found in session to download.".getBytes(StandardCharsets.UTF_8)); // Message changed
        }

        try {
            byte[] xmlBytes = compareService.exportSpotifyOnlySongsToXml(spotifyOnlySongs); // Method call changed

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment", "spotify_only_songs.xml"); // Filename changed

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(xmlBytes);
        } catch (IOException e) {
            logger.error("Error generating XML for Spotify-only songs", e); // Log message changed
            return ResponseEntity.status(500).body(("Error generating XML: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/download/localonly/csv") // New endpoint name
    public ResponseEntity<byte[]> downloadLocalOnlyCsv(HttpSession session) { // Method name changed
        List<SongEntry> localOnlySongs = (List<SongEntry>) session.getAttribute("localOnlySongs"); // Variable name changed

        if (localOnlySongs == null || localOnlySongs.isEmpty()) {
            return ResponseEntity.badRequest().body("No local-only songs found in session to download.".getBytes(StandardCharsets.UTF_8)); // Message changed
        }

        byte[] csvBytes = compareService.exportLocalOnlySongsToCsv(localOnlySongs); // Method call changed

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "local_only_songs.csv"); // Filename changed

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @GetMapping("/download/localonly/xml") // New endpoint name
    public ResponseEntity<byte[]> downloadLocalOnlyXml(HttpSession session) { // Method name changed
        List<SongEntry> localOnlySongs = (List<SongEntry>) session.getAttribute("localOnlySongs"); // Variable name changed

        if (localOnlySongs == null || localOnlySongs.isEmpty()) {
            return ResponseEntity.badRequest().body("No local-only songs found in session to download.".getBytes(StandardCharsets.UTF_8)); // Message changed
        }

        try {
            byte[] xmlBytes = compareService.exportLocalOnlySongsToXml(localOnlySongs); // Method call changed

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment", "local_only_songs.xml"); // Filename changed

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(xmlBytes);
        } catch (IOException e) {
            logger.error("Error generating XML for local-only songs", e); // Log message changed
            return ResponseEntity.status(500).body(("Error generating XML: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/download/comparison/csv")
    public ResponseEntity<byte[]> downloadCsv(HttpSession session) {
        List<SongEntry> commonSongs = (List<SongEntry>) session.getAttribute("commonSongs");
        // Ensure to retrieve the correctly renamed list for combined comparison if you still want it to include unique/localOnly
        List<SongEntry> localOnlySongs = (List<SongEntry>) session.getAttribute("localOnlySongs"); // Changed from uniqueSongs

        if (commonSongs == null && localOnlySongs == null) { // Changed from uniqueSongs
            return ResponseEntity.badRequest().body("No comparison data found in session.".getBytes(StandardCharsets.UTF_8));
        }

        // Pass the correct list to the service method
        byte[] csvBytes = compareService.exportComparisonToCsv(commonSongs, localOnlySongs); // Changed from uniqueSongs

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "music_comparison.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @GetMapping("/download/comparison/xml")
    public ResponseEntity<byte[]> downloadXml(HttpSession session) {
        List<SongEntry> commonSongs = (List<SongEntry>) session.getAttribute("commonSongs");
        // Ensure to retrieve the correctly renamed list for combined comparison if you still want it to include unique/localOnly
        List<SongEntry> localOnlySongs = (List<SongEntry>) session.getAttribute("localOnlySongs"); // Changed from uniqueSongs

        if (commonSongs == null && localOnlySongs == null) { // Changed from uniqueSongs
            return ResponseEntity.badRequest().body("No comparison data found in session.".getBytes(StandardCharsets.UTF_8));
        }

        try {
            // Pass the correct list to the service method
            byte[] xmlBytes = compareService.exportComparisonToXml(commonSongs, localOnlySongs); // Changed from uniqueSongs

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment", "music_comparison.xml");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(xmlBytes);
        } catch (IOException e) {
            logger.error("Error generating XML for comparison results", e);
            return ResponseEntity.status(500).body(("Error generating XML: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}