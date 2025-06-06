package org.rangeles.musiclibrarycompare.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rangeles.musiclibrarycompare.model.SongEntry;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

@Service
public class ParsingService {

    private static final Logger logger = LoggerFactory.getLogger(ParsingService.class);

    public List<SongEntry> parseXml(InputStream originalInputStream, String filename) throws IOException {
        List<SongEntry> songs = new ArrayList<>();
        int lineNumber = 1;
        String title = null;
        String artist = null;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            originalInputStream.transferTo(buffer);
            byte[] bytes = buffer.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            String xmlContent = new String(bytes, StandardCharsets.UTF_8);
            logger.debug("XML Content:\n{}", xmlContent);

            XMLInputFactory factory = XMLInputFactory.newFactory();
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamReader.START_ELEMENT:
                        String localName = reader.getLocalName();
                        if ("name".equals(localName) || "title".equals(localName)) {
                            try {
                                reader.next();
                                if (reader.getEventType() == XMLStreamReader.CHARACTERS) {
                                    title = reader.getText();
                                } else {
                                    throw new XMLStreamException("Expected text for " + localName + " tag, but got: " + reader.getEventType());
                                }

                            } catch (XMLStreamException e) {
                                logger.error("XML Parsing Error in file '{}' at line {}: Expected text for {} tag, but got: {}", filename, lineNumber, localName, e.getMessage());
                                title = null;
                            }
                        } else if ("artist".equals(localName)) {
                            try {
                                reader.next();
                                if (reader.getEventType() == XMLStreamReader.CHARACTERS) {
                                    artist = reader.getText();
                                } else {
                                    throw new XMLStreamException("Expected text for artist tag, but got: " + reader.getEventType());
                                }
                            } catch (XMLStreamException e) {
                                logger.error("XML Parsing Error in file '{}' at line {}: Expected text for artist tag, but got: {}", filename, lineNumber, localName, e.getMessage());
                                artist = null;
                            }
                        }
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        if ("track".equals(reader.getLocalName())) {
                            if (title != null && artist != null) {
                                songs.add(new SongEntry(title, artist));
                                logger.debug("Parsed track - Title: {}, Artist: {}", title, artist);
                            } else {
                                logger.warn("Skipping track due to missing title or artist");
                            }
                            title = null;
                            artist = null;
                        }
                        break;
                    case XMLStreamReader.CHARACTERS:
                        break;
                }
                if (event == XMLStreamReader.END_ELEMENT || event == XMLStreamReader.START_ELEMENT) {
                    lineNumber = xmlContent.substring(0, (int) reader.getLocation().getCharacterOffset()).split("\n").length;
                }
            }
            reader.close();

        } catch (XMLStreamException e) {
            String errorMessage = String.format("Error parsing XML file '%s' at line %d: %s. This is caused by certain " +
                    "characters in the file. Try wrapping the offending content in the tag with <![CDATA[]]>." +
                    "For example for the album title Formula of Love: O+T=<3 <![CDATA[Formula of Love: O+T=<3]]>", filename, lineNumber, e.getMessage());
            logger.error(errorMessage);
            throw new IOException(errorMessage, e); // Wrap and re-throw as IOException
        }
        return songs;
    }


    public List<SongEntry> parseCsv(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines()
                .skip(1) // skip header
                .map(line -> line.split(","))
                .map(parts -> new SongEntry(parts[0].trim(), parts[1].trim()))
                .collect(Collectors.toList());
    }

    public List<SongEntry> scanMusicFolder(File folder) {
        File[] files = folder.listFiles();
        if (files == null) return Collections.emptyList();

        List<SongEntry> entries = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName().replaceAll("\\.[^.]+$", "");
                entries.add(new SongEntry(name, ""));
            }
        }
        return entries;
    }

}