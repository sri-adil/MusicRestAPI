# RestApiForMusic

This project implements a RESTful API for managing and streaming music files using Java and Spring Boot. It provides endpoints for uploading, retrieving, streaming, downloading, and deleting audio files.

## Features

- **File Upload**: Upload music files directly to the server storage.
- **Data Persistence**: Stores file metadata (including file paths) in a database.
- **Audio Streaming**: Supports HTTP Range requests for efficient audio streaming playback.
- **File Retrieval**: Download files or retrieve them for inline playback.
- **Management**: List all available tracks or delete specific files.

## API Documentation

The following sections detail the available endpoints. Postman screenshots are included to demonstrate functionality.

### 1. Upload Music
**Endpoint**: `POST /api/songs/upload`
**Description**: Uploads a music file (multipart/form-data) to the server. The file is stored locally, and its metadata is saved to the database.

![Upload Endpoint](POSTMAN/upload.png)

### 2. Get All Songs
**Endpoint**: `GET /api/songs`
**Description**: Retrieves a list of all music files stored in the database.

![Get All Songs Endpoint](POSTMAN/AllSongs.png)

### 3. Get Song by ID
**Endpoint**: `GET /api/songs/{id}`
**Description**: Retrieves a specific song file by its unique ID. Returns the file resource for inline use.

![Get Song By ID Endpoint](POSTMAN/getByID.png)

### 4. Stream Song
**Endpoint**: `GET /api/songs/{id}/stream`
**Description**: Streams audio content for the specified song ID with bounded memory use. Supports open-ended, closed, and suffix byte ranges (for example, `bytes=100-`, `bytes=100-199`, and `bytes=-500`) for seeking and partial content delivery (HTTP 206). Invalid or unsatisfiable ranges return HTTP 416; multiple ranges are not supported.

![Stream Endpoint](POSTMAN/stream.png)

### 5. Download Song
**Endpoint**: `GET /api/songs/{id}/download`
**Description**: Initiates a file download for the specified song ID.

![Download Endpoint](POSTMAN/download.png)

### 6. Delete Song
**Endpoint**: `DELETE /api/songs/{id}`
**Description**: Removes the song record from the database. Note that this operation currently removes the database entry.

![Delete Endpoint](POSTMAN/delete.png)

## Testing

This project includes tests to verify core functionality:

- **Upload**: Tests the `POST /api/songs/upload` endpoint.
- **Download**: Tests the `GET /api/songs/{id}/download` endpoint.
- **Get All Songs**: Tests retrieval of the song list via `GET /api/songs`.
- **Get Song By ID**: Tests `GET /api/songs/{id}` for both existing and non-existing IDs (validating 404 response).

You can run these tests using your IDE or Maven.

## Technologies Used

- **Java**: Core programming language.
- **Spring Boot**: Framework for building the REST API application.
- **Spring Web**: For building web, including RESTful, applications using Spring MVC.
- **Spring Data JPA**: For interaction with the database.

## References

The following resources were used in the development of this project and are recommended for further reading:

- [Building an Application with Spring Boot](https://spring.io/guides/gs/spring-boot/)
- [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
- [Uploading Files](https://spring.io/guides/gs/uploading-files/)
- [HTTP Range Requests (MDN)](https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests)

## Publishing

For instructions on how to publish this project to GitHub Packages, see [PUBLISHING.md](PUBLISHING.md).
