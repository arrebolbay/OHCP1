package com.pestqa.ohcp.service;

import com.pestqa.ohcp.entity.PestKnowledge;
import com.pestqa.ohcp.repository.PestKnowledgeRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagSearchService {

    private static final Logger log = LoggerFactory.getLogger(RagSearchService.class);

    private final PestKnowledgeRepository repository;
    private Directory indexDirectory;
    private Analyzer analyzer;

    public RagSearchService(PestKnowledgeRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            analyzer = new SmartChineseAnalyzer();
            indexDirectory = new ByteBuffersDirectory();
            buildIndex();
            log.info("✅ Lucene索引构建完成");
        } catch (Exception e) {
            log.error("Lucene索引初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (indexDirectory != null) {
                indexDirectory.close();
            }
            if (analyzer != null) {
                analyzer.close();
            }
        } catch (Exception e) {
            log.error("Lucene资源关闭失败", e);
        }
    }

    /**
     * 构建/重建 Lucene 全文索引
     */
    public synchronized void buildIndex() throws Exception {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(indexDirectory, config);

        List<PestKnowledge> all = repository.findAll();
        for (PestKnowledge pk : all) {
            Document doc = new Document();
            doc.add(new LongPoint("id", pk.getId()));
            doc.add(new StoredField("id", pk.getId()));
            doc.add(new TextField("question", pk.getQuestion() != null ? pk.getQuestion() : "", Field.Store.YES));
            doc.add(new TextField("aliases", pk.getAliases() != null ? pk.getAliases() : "", Field.Store.YES));
            doc.add(new TextField("pathogen", pk.getPathogen() != null ? pk.getPathogen() : "", Field.Store.YES));
            doc.add(new TextField("crops", pk.getCrops() != null ? pk.getCrops() : "", Field.Store.YES));
            doc.add(new TextField("symptoms", pk.getSymptoms() != null ? pk.getSymptoms() : "", Field.Store.YES));
            doc.add(new TextField("occurrenceRule", pk.getOccurrenceRule() != null ? pk.getOccurrenceRule() : "", Field.Store.NO));
            doc.add(new TextField("transmission", pk.getTransmission() != null ? pk.getTransmission() : "", Field.Store.NO));
            doc.add(new TextField("agriculturalControl", pk.getAgriculturalControl() != null ? pk.getAgriculturalControl() : "", Field.Store.NO));
            doc.add(new TextField("chemicalControl", pk.getChemicalControl() != null ? pk.getChemicalControl() : "", Field.Store.NO));
            doc.add(new TextField("biologicalControl", pk.getBiologicalControl() != null ? pk.getBiologicalControl() : "", Field.Store.NO));
            doc.add(new TextField("prevention", pk.getPrevention() != null ? pk.getPrevention() : "", Field.Store.NO));
            doc.add(new StringField("category", pk.getCategory() != null ? pk.getCategory() : "", Field.Store.YES));
            doc.add(new StringField("knowledgeCategory", pk.getKnowledgeCategory() != null ? pk.getKnowledgeCategory() : "", Field.Store.YES));
            writer.addDocument(doc);
        }
        writer.commit();
        writer.close();
        log.info("📊 Lucene索引：{} 条记录", all.size());
    }

    /**
     * RAG 混合检索：Lucene 全文搜索 + JPA关键词匹配
     */
    public List<PestKnowledge> search(String query, int topK) {
        Set<Long> luceneIds = new HashSet<>();
        Map<Long, Float> scores = new HashMap<>();

        // 1. Lucene 全文检索
        try {
            DirectoryReader reader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(reader);

            String[] fields = {"question", "aliases", "pathogen", "crops", "symptoms", "occurrenceRule",
                    "agriculturalControl", "chemicalControl", "biologicalControl", "prevention"};
            Map<String, Float> boosts = new HashMap<>();
            boosts.put("question", 3.0f);
            boosts.put("aliases", 2.5f);
            boosts.put("crops", 2.0f);
            boosts.put("symptoms", 2.0f);
            boosts.put("pathogen", 1.5f);

            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, boosts);
            parser.setDefaultOperator(QueryParser.Operator.OR);
            Query luceneQuery = parser.parse(QueryParser.escape(query));

            TopDocs topDocs = searcher.search(luceneQuery, topK * 3);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = searcher.doc(sd.doc);
                long id = Long.parseLong(doc.get("id"));
                luceneIds.add(id);
                scores.put(id, scores.getOrDefault(id, 0f) + sd.score);
            }
            reader.close();
        } catch (Exception e) {
            log.warn("Lucene搜索异常: {}", e.getMessage());
        }

        // 2. JPA 关键词匹配（捕获Lucene可能漏掉的结果）
        List<PestKnowledge> jpaResults = repository.fullTextSearch(query);
        for (PestKnowledge pk : jpaResults) {
            if (!luceneIds.contains(pk.getId())) {
                luceneIds.add(pk.getId());
                scores.putIfAbsent(pk.getId(), 0.5f);
            }
        }

        // 3. 按得分排序
        List<Long> sortedIds = luceneIds.stream()
                .sorted((a, b) -> Float.compare(scores.getOrDefault(b, 0f), scores.getOrDefault(a, 0f)))
                .limit(topK)
                .collect(Collectors.toList());

        if (sortedIds.isEmpty()) return Collections.emptyList();

        List<PestKnowledge> results = repository.findAllById(sortedIds);
        // 保持排序顺序
        Map<Long, PestKnowledge> idMap = results.stream()
                .collect(Collectors.toMap(PestKnowledge::getId, pk -> pk));
        return sortedIds.stream()
                .map(idMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 基于症状的病虫害诊断
     */
    public List<PestKnowledge> diagnoseBySymptoms(String symptomDescription, int topK) {
        Set<Long> ids = new HashSet<>();
        Map<Long, Float> scores = new HashMap<>();

        try {
            DirectoryReader reader = DirectoryReader.open(indexDirectory);
            IndexSearcher searcher = new IndexSearcher(reader);

            String[] fields = {"symptoms", "occurrenceRule", "question", "pathogen"};
            Map<String, Float> boosts = new HashMap<>();
            boosts.put("symptoms", 4.0f);
            boosts.put("occurrenceRule", 2.0f);
            boosts.put("question", 1.5f);

            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, boosts);
            parser.setDefaultOperator(QueryParser.Operator.OR);
            Query query = parser.parse(QueryParser.escape(symptomDescription));

            TopDocs topDocs = searcher.search(query, topK);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = searcher.doc(sd.doc);
                long id = Long.parseLong(doc.get("id"));
                ids.add(id);
                scores.put(id, sd.score);
            }
            reader.close();
        } catch (Exception e) {
            log.warn("症状诊断搜索异常: {}", e.getMessage());
        }

        if (ids.isEmpty()) return Collections.emptyList();

        return repository.findAllById(ids).stream()
                .sorted((a, b) -> Float.compare(
                        scores.getOrDefault(b.getId(), 0f),
                        scores.getOrDefault(a.getId(), 0f)))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 按作物类别查询
     */
    public List<PestKnowledge> getByCropCategory(String cropKeyword) {
        return repository.findByCrop(cropKeyword);
    }

    /**
     * 按知识分类查询
     */
    public List<PestKnowledge> getByKnowledgeCategory(String category) {
        return repository.findByKnowledgeCategory(category);
    }

    /**
     * 获取所有知识条目数
     */
    public long getTotalCount() {
        return repository.count();
    }
}
