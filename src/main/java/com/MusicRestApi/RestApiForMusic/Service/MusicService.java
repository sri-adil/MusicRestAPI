package com.MusicRestApi.RestApiForMusic.Service;

import com.MusicRestApi.RestApiForMusic.Model.MusicModel;
import com.MusicRestApi.RestApiForMusic.Repository.musicdb;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MusicService {

    private final musicdb musicdb;
    private final Path storageDir;
    private final long maxUploadSize;

    public static record StreamChunk(byte[] content, long start, long end, long total) {}
    public static record UploadResult(int id, String filename, long size) {}

    public MusicService(musicdb musicdb,
                        @org.springframework.beans.factory.annotation.Value("${app.storage.path}") String storagePath,
                        @org.springframework.beans.factory.annotation.Value("${app.upload.max-file-size:524288000}") long maxUploadSize) {
        this.musicdb = musicdb;
        this.storageDir = Paths.get(storagePath).toAbsolutePath().normalize();
        this.maxUploadSize = maxUploadSize;
    }

    public UploadResult upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file must not be empty");
        }

        if (file.getSize() > maxUploadSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Audio file exceeds the upload limit");
        }

        String originalName = file.getOriginalFilename();
        String safeName = Paths.get(originalName == null ? "" : originalName).getFileName().toString();
        if (safeName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        Files.createDirectories(storageDir);
        String storedName = UUID.randomUUID() + "_" + safeName;
        Path target = storageDir.resolve(storedName).normalize();
        try (var input = file.getInputStream()) {
            Files.copy(input, target);
        }

        MusicModel model = new MusicModel();
        model.setPath(String.valueOf(target));
        try {
            MusicModel saved = musicdb.save(model);
            return new UploadResult(saved.getId(), safeName, file.getSize());
        } catch (RuntimeException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
    }

    public List<MusicModel> getAllFiles() {
        return (List<MusicModel>) musicdb.findAll();
    }

    public Resource getByID(int id) throws MalformedURLException {
        Optional<MusicModel> response = musicdb.findById(id);
        if (response.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Music not found");
        }
        return new FileUrlResource(response.get().getPath());
    }

    public StreamChunk getStream(int id, String bytes) throws IOException {
        Optional<MusicModel> response = musicdb.findById(id);
        if (response.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Music not found");
        }

        Path path = Paths.get(response.get().getPath());
        byte[] musicArray = Files.readAllBytes(path);
        long start = 0;
        long finish = musicArray.length - 1;

        if (bytes != null && bytes.startsWith("bytes=")) {
            String[] part = bytes.replace("bytes=", "").split("-", 2);
            if (!part[0].isEmpty()) {
                start = Long.parseLong(part[0]);
            }
            if (part.length > 1 && !part[1].isEmpty()) {
                finish = Long.parseLong(part[1]);
            }
        }

        if (start < 0) {
            start = 0;
        }
        if (finish >= musicArray.length) {
            finish = musicArray.length - 1;
        }
        if (start > finish) {
            throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Invalid range");
        }

        byte[] content = Arrays.copyOfRange(musicArray, (int) start, (int) finish + 1);
        return new StreamChunk(content, start, finish, musicArray.length);
    }


    public Path download(int id) throws IOException {
        var file=musicdb.findById(id).orElseThrow();
        return Paths.get(file.getPath());
    }

    public Boolean remove(int id) throws IOException {
        Optional<MusicModel> song = musicdb.findById(id);
        if (song.isEmpty()) {
            return false;
        }

        Files.deleteIfExists(Paths.get(song.get().getPath()));
        musicdb.deleteById(id);
        return true;
    }


}
