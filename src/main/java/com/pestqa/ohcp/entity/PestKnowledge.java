package com.pestqa.ohcp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pest_knowledge")
public class PestKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question", nullable = false, length = 200)
    private String question;

    @Column(name = "aliases", length = 500)
    private String aliases;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // 病害 or 虫害 or 草害

    @Column(name = "pathogen", length = 200)
    private String pathogen;

    @Column(name = "taxonomy", length = 200)
    private String taxonomy; // 分类地位

    @Column(name = "crops", length = 500)
    private String crops; // 危害作物，逗号分隔

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "occurrence_rule", columnDefinition = "TEXT")
    private String occurrenceRule;

    @Column(name = "transmission", columnDefinition = "TEXT")
    private String transmission;

    @Column(name = "agricultural_control", columnDefinition = "TEXT")
    private String agriculturalControl;

    @Column(name = "chemical_control", columnDefinition = "TEXT")
    private String chemicalControl;

    @Column(name = "biological_control", columnDefinition = "TEXT")
    private String biologicalControl;

    @Column(name = "prevention", columnDefinition = "TEXT")
    private String prevention;

    @Column(name = "knowledge_category", length = 50)
    private String knowledgeCategory; // 粮食作物/经济作物/蔬菜作物/果树作物

    @Column(name = "icon", length = 10)
    private String icon;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAliases() { return aliases; }
    public void setAliases(String aliases) { this.aliases = aliases; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPathogen() { return pathogen; }
    public void setPathogen(String pathogen) { this.pathogen = pathogen; }

    public String getTaxonomy() { return taxonomy; }
    public void setTaxonomy(String taxonomy) { this.taxonomy = taxonomy; }

    public String getCrops() { return crops; }
    public void setCrops(String crops) { this.crops = crops; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getOccurrenceRule() { return occurrenceRule; }
    public void setOccurrenceRule(String occurrenceRule) { this.occurrenceRule = occurrenceRule; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    public String getAgriculturalControl() { return agriculturalControl; }
    public void setAgriculturalControl(String agriculturalControl) { this.agriculturalControl = agriculturalControl; }

    public String getChemicalControl() { return chemicalControl; }
    public void setChemicalControl(String chemicalControl) { this.chemicalControl = chemicalControl; }

    public String getBiologicalControl() { return biologicalControl; }
    public void setBiologicalControl(String biologicalControl) { this.biologicalControl = biologicalControl; }

    public String getPrevention() { return prevention; }
    public void setPrevention(String prevention) { this.prevention = prevention; }

    public String getKnowledgeCategory() { return knowledgeCategory; }
    public void setKnowledgeCategory(String knowledgeCategory) { this.knowledgeCategory = knowledgeCategory; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
