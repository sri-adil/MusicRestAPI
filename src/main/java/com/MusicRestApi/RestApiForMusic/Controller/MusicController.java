package com.MusicRestApi.RestApiForMusic.Controller;

import com.MusicRestApi.RestApiForMusic.Model.MusicModel;
import com.MusicRestApi.RestApiForMusic.Service.MusicService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MusicController {
    private final MusicService service;

    public MusicController(MusicService service) {
        this.service = service;
    }

    // POST - /api/songs/upload
    @PostMapping("/songs/upload")
    public void responseEntity(@RequestParam MultipartFile uploadFile) throws Exception {
        service.upload(uploadFile);
    }

    // GET - /api/songs
    @GetMapping("/songs")
    public ResponseEntity<List<MusicModel>> getAll() {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(service.getAllFiles());
    }

    // GET- /api/songs/{id}
    @GetMapping("/songs/{id}")
    public ResponseEntity<Resource> getbyID(@PathVariable int id) throws MalformedURLException {
        Resource resource = service.getByID(id);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename:" + resource.getFilename())
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(resource);

    }

    // GET- /api/songs/{id}/stream
    @GetMapping("/songs/{id}/stream")
    public ResponseEntity<StreamingResponseBody> stream(@PathVariable int id,
                                                        @RequestHeader(value = "Range", required = false) String range)
            throws IOException {
        MusicService.StreamResource stream = service.getStream(id, range);
        StreamingResponseBody body = output -> service.writeStream(stream, output);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setContentLength(stream.length());
        if (stream.partial()) {
            headers.set(HttpHeaders.CONTENT_RANGE,
                    "bytes " + stream.start() + "-" + stream.end() + "/" + stream.total());
            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(MediaType.valueOf("audio/mpeg"))
                    .body(body);
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(headers)
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(body);
    }

    //GET- /api/songs/{id}/download
    @GetMapping("/songs/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable int id) throws IOException {

        Path path=(service.download(id));
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(path))
                .body(resource);
    }

    //DELETE - /api/songs/{id}
    @DeleteMapping("/songs/{id}")
    public ResponseEntity<Boolean>remove(@PathVariable int id){
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(service.remove(id));
    }
}


