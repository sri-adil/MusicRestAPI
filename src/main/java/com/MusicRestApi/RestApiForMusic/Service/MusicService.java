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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Service
public class MusicService {

    private final musicdb musicdb;
    private final Path storageDir;

    public static record StreamResource(Path path, long start, long end, long total, boolean partial) {
        public long length() {
            return end < start ? 0 : end - start + 1;
        }
    }

    public MusicService(musicdb musicdb,
                        @org.springframework.beans.factory.annotation.Value("${app.storage.path}") String storagePath) {
        this.musicdb = musicdb;
        this.storageDir = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    public boolean upload(MultipartFile file) throws Exception {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
        String originalName = file.getOriginalFilename();
        String safeName = Paths.get(originalName == null ? "" : originalName).getFileName().toString();
        if (safeName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }
        Path target = storageDir.resolve(safeName).normalize();
        if (!target.startsWith(storageDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        MusicModel model = new MusicModel();
        model.setPath(String.valueOf(target));
        musicdb.save(model);
        return true;
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

    public StreamResource getStream(int id, String range) throws IOException {
        Optional<MusicModel> response = musicdb.findById(id);
        if (response.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Music not found");
        }

        Path path = Paths.get(response.get().getPath());
        long total = Files.size(path);
        if (range == null) {
            return new StreamResource(path, 0, total - 1, total, false);
        }

        if (!range.startsWith("bytes=") || range.indexOf(',') >= 0) {
            throw invalidRange();
        }

        String value = range.substring("bytes=".length());
        String[] bounds = value.split("-", -1);
        if (bounds.length != 2 || (bounds[0].isEmpty() && bounds[1].isEmpty()) || total == 0) {
            throw invalidRange();
        }

        try {
            long start;
            long end;
            if (bounds[0].isEmpty()) {
                long suffixLength = Long.parseLong(bounds[1]);
                if (suffixLength <= 0) {
                    throw invalidRange();
                }
                start = Math.max(total - suffixLength, 0);
                end = total - 1;
            } else {
                start = Long.parseLong(bounds[0]);
                end = bounds[1].isEmpty() ? total - 1 : Long.parseLong(bounds[1]);
                if (start >= total || end < start) {
                    throw invalidRange();
                }
                end = Math.min(end, total - 1);
            }
            return new StreamResource(path, start, end, total, true);
        } catch (NumberFormatException exception) {
            throw invalidRange();
        }
    }

    private ResponseStatusException invalidRange() {
        return new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Invalid byte range");
    }

    public void writeStream(StreamResource stream, OutputStream output) throws IOException {
        long remaining = stream.length();
        if (remaining == 0) {
            return;
        }

        try (FileChannel channel = FileChannel.open(stream.path(), StandardOpenOption.READ)) {
            channel.position(stream.start());
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            while (remaining > 0) {
                buffer.clear();
                buffer.limit((int) Math.min(buffer.capacity(), remaining));
                int read = channel.read(buffer);
                if (read < 0) {
                    break;
                }
                output.write(buffer.array(), 0, read);
                remaining -= read;
            }
        }
    }


    public Path download(int id) throws IOException {
        var file=musicdb.findById(id).orElseThrow();
        return Paths.get(file.getPath());
    }

    public Boolean remove(int id){
        if(musicdb.existsById(id)){
            musicdb.deleteById(id);
            return true;
        }
        else{
            return false;
        }


    }


}
