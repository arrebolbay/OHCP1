package com.pestqa.ohcp.repository;

import com.pestqa.ohcp.entity.PestKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PestKnowledgeRepository extends JpaRepository<PestKnowledge, Long> {

    List<PestKnowledge> findByKnowledgeCategory(String knowledgeCategory);

    List<PestKnowledge> findByCategory(String category);

    @Query("SELECT p FROM PestKnowledge p WHERE LOWER(p.question) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PestKnowledge> searchByQuestion(@Param("keyword") String keyword);

    @Query("SELECT p FROM PestKnowledge p WHERE LOWER(p.symptoms) LIKE LOWER(CONCAT('%', :symptom, '%'))")
    List<PestKnowledge> searchBySymptoms(@Param("symptom") String symptom);

    @Query("SELECT p FROM PestKnowledge p WHERE LOWER(p.crops) LIKE LOWER(CONCAT('%', :crop, '%'))")
    List<PestKnowledge> findByCrop(@Param("crop") String crop);

    @Query("SELECT p FROM PestKnowledge p WHERE " +
           "LOWER(p.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.aliases) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.symptoms) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.pathogen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.crops) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<PestKnowledge> fullTextSearch(@Param("keyword") String keyword);
}
