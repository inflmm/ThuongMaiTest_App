package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Collection;



@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {

	Optional <Collection> findBySlug(String slug);

}