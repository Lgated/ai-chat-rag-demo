package com.example.demo.repository;

import com.example.demo.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DocumentMetaRepository extends JpaRepository<Document,Long> {



}
