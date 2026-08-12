package com.pestqa.ohcp.controller;

import com.pestqa.ohcp.entity.PestKnowledge;
import com.pestqa.ohcp.service.RagSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PestQAController {

    private final RagSearchService searchService;

    public PestQAController(RagSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 智能问答接口 - RAG检索
     */
    @PostMapping("/qa")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, String> request) {
        String question = request.getOrDefault("question", "").trim();
        if (question.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "问题不能为空"));
        }

        // RAG 检索
        List<PestKnowledge> results = searchService.search(question, 3);

        // 症状诊断
        List<PestKnowledge> diagnoses = searchService.diagnoseBySymptoms(question, 5);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("question", question);

        if (results.isEmpty() && diagnoses.isEmpty()) {
            response.put("found", false);
            response.put("message", "抱歉，未找到与您问题相关的病虫害信息。请尝试更具体的描述，或换个关键词提问。");
            response.put("suggestion", "我们的知识库覆盖水稻、小麦、玉米、棉花、蔬菜、果树等常见农作物的病虫害防治知识。");
        } else {
            response.put("found", true);

            // 主结果
            if (!results.isEmpty()) {
                List<Map<String, Object>> answerList = results.stream().map(this::toMap).collect(Collectors.toList());
                response.put("answers", answerList);
                response.put("primary", toMap(results.get(0)));
            }

            // 诊断建议
            if (!diagnoses.isEmpty()) {
                List<Map<String, Object>> diagList = diagnoses.stream()
                        .filter(d -> results.stream().noneMatch(r -> r.getId().equals(d.getId())))
                        .map(this::toMap)
                        .collect(Collectors.toList());
                if (!diagList.isEmpty()) {
                    response.put("diagnosis", diagList);
                }
            }
        }

        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 快速诊断 - 根据症状描述推测病虫害
     */
    @PostMapping("/diagnose")
    public ResponseEntity<Map<String, Object>> diagnose(@RequestBody Map<String, String> request) {
        String symptoms = request.getOrDefault("symptoms", "").trim();
        if (symptoms.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "症状描述不能为空"));
        }

        List<PestKnowledge> diagnoses = searchService.diagnoseBySymptoms(symptoms, 5);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("symptoms", symptoms);
        response.put("found", !diagnoses.isEmpty());

        if (diagnoses.isEmpty()) {
            response.put("message", "根据您描述的症状，暂未找到匹配的病虫害。建议拍照或提供更详细的症状特征。");
        } else {
            response.put("possiblePests", diagnoses.stream().map(this::toMap).collect(Collectors.toList()));
            response.put("suggestion", "以上为可能的病虫害类型，请结合具体症状进一步确认。如需详细防治方案，可针对具体病虫害进一步提问。");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 按分类浏览知识库
     */
    @GetMapping("/knowledge/category/{category}")
    public ResponseEntity<List<Map<String, Object>>> getByCategory(@PathVariable String category) {
        List<PestKnowledge> list = searchService.getByKnowledgeCategory(category);
        return ResponseEntity.ok(list.stream().map(this::toMap).collect(Collectors.toList()));
    }

    /**
     * 按作物查询
     */
    @GetMapping("/knowledge/crop/{crop}")
    public ResponseEntity<List<Map<String, Object>>> getByCrop(@PathVariable String crop) {
        List<PestKnowledge> list = searchService.getByCropCategory(crop);
        return ResponseEntity.ok(list.stream().map(this::toMap).collect(Collectors.toList()));
    }

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/knowledge/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", searchService.getTotalCount());
        stats.put("status", "running");
        stats.put("engine", "Lucene + JPA 混合检索 (RAG)");
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取完整知识库JSON（供前端直接使用）
     */
    @GetMapping("/knowledge-base")
    public ResponseEntity<Map<String, Object>> getKnowledgeBase() {
        List<PestKnowledge> all = searchService.getByKnowledgeCategory(null);
        // 构建与前端 knowledge-base.json 相同格式的结构
        // 这里返回简化版本
        Map<String, Object> kb = new LinkedHashMap<>();
        kb.put("totalCount", searchService.getTotalCount());
        kb.put("items", all.stream().map(this::toMap).collect(Collectors.toList()));
        return ResponseEntity.ok(kb);
    }

    private Map<String, Object> toMap(PestKnowledge pk) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", pk.getId());
        map.put("question", pk.getQuestion());
        map.put("aliases", pk.getAliases() != null ? Arrays.asList(pk.getAliases().split("、")) : Collections.emptyList());
        map.put("category", pk.getCategory());
        map.put("pathogen", pk.getPathogen());
        map.put("taxonomy", pk.getTaxonomy());
        map.put("crops", pk.getCrops() != null ? Arrays.asList(pk.getCrops().split(",")) : Collections.emptyList());
        map.put("symptoms", pk.getSymptoms());
        map.put("occurrenceRule", pk.getOccurrenceRule());
        map.put("transmission", pk.getTransmission());
        map.put("agriculturalControl", pk.getAgriculturalControl());
        map.put("chemicalControl", pk.getChemicalControl());
        map.put("biologicalControl", pk.getBiologicalControl());
        map.put("prevention", pk.getPrevention());
        map.put("knowledgeCategory", pk.getKnowledgeCategory());
        map.put("icon", pk.getIcon());
        return map;
    }
}
