package com.MusicRestApi.RestApiForMusic.Repository;

import com.MusicRestApi.RestApiForMusic.Model.MusicModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface musicdb extends CrudRepository<MusicModel, Integer>, PagingAndSortingRepository<MusicModel, Integer> {
    Page<MusicModel> findByPathContainingIgnoreCase(String filename, Pageable pageable);

}
