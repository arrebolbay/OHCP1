package com.pestqa.ohcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pestqa.ohcp.entity.PestKnowledge;
import com.pestqa.ohcp.repository.PestKnowledgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final PestKnowledgeRepository repository;
    private final ObjectMapper objectMapper;

    public DataInitializer(PestKnowledgeRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        long count = repository.count();
        if (count > 0) {
            log.info("📚 数据库已有 {} 条记录，跳过初始化", count);
            return;
        }

        log.info("🔄 开始从 knowledge-base.json 初始化数据库...");
        
        try {
            // Try to load from classpath first
            InputStream is;
            try {
                is = new ClassPathResource("knowledge-base.json").getInputStream();
            } catch (Exception e) {
                // Fallback: try loading from filesystem
                log.info("Classpath加载失败，尝试从文件系统加载...");
                java.io.File file = new java.io.File("knowledge-base.json");
                if (file.exists()) {
                    is = new java.io.FileInputStream(file);
                } else {
                    file = new java.io.File("../knowledge-base.json");
                    if (file.exists()) {
                        is = new java.io.FileInputStream(file);
                    } else {
                        log.warn("⚠️ knowledge-base.json 未找到，将使用内置最小数据初始化");
                        initMinimalData();
                        return;
                    }
                }
            }

            JsonNode root = objectMapper.readTree(is);
            is.close();

            JsonNode categories = root.get("categories");
            if (categories == null || !categories.isArray()) {
                log.warn("⚠️ 知识库格式无效，使用内置数据");
                initMinimalData();
                return;
            }

            int totalImported = 0;
            for (JsonNode category : categories) {
                String catName = category.get("name").asText();
                String catIcon = category.has("icon") ? category.get("icon").asText() : "📚";
                JsonNode items = category.get("items");
                if (items == null || !items.isArray()) continue;

                for (JsonNode item : items) {
                    PestKnowledge pk = new PestKnowledge();
                    pk.setQuestion(item.get("question").asText());
                    pk.setAliases(item.has("aliases") ? item.get("aliases").asText() : "");
                    pk.setCategory(item.has("category") ? item.get("category").asText() : "病害");
                    pk.setPathogen(item.has("pathogen") ? item.get("pathogen").asText() : "");
                    pk.setTaxonomy(item.has("taxonomy") ? item.get("taxonomy").asText() : "");
                    
                    if (item.has("crops") && item.get("crops").isArray()) {
                        List<String> cropList = objectMapper.readerForListOf(String.class).readValue(item.get("crops"));
                        pk.setCrops(String.join(",", cropList));
                    }
                    
                    pk.setSymptoms(item.has("symptoms") ? item.get("symptoms").asText() : "");
                    pk.setOccurrenceRule(item.has("occurrence_rule") ? item.get("occurrence_rule").asText() : "");
                    pk.setTransmission(item.has("transmission") ? item.get("transmission").asText() : "");
                    pk.setAgriculturalControl(item.has("agricultural_control") ? item.get("agricultural_control").asText() : "");
                    pk.setChemicalControl(item.has("chemical_control") ? item.get("chemical_control").asText() : "");
                    pk.setBiologicalControl(item.has("biological_control") ? item.get("biological_control").asText() : "");
                    pk.setPrevention(item.has("prevention") ? item.get("prevention").asText() : "");
                    pk.setKnowledgeCategory(catName);
                    pk.setIcon(catIcon);
                    
                    repository.save(pk);
                    totalImported++;
                }
            }

            log.info("✅ 数据库初始化完成：导入 {} 条病虫害知识", totalImported);
        } catch (Exception e) {
            log.error("数据初始化失败: {}", e.getMessage(), e);
            log.info("将使用内置最小数据初始化...");
            try {
                initMinimalData();
            } catch (Exception ex) {
                log.error("内置数据初始化也失败了: {}", ex.getMessage());
            }
        }
    }

    /**
     * 内置最小数据集，确保系统至少有基本知识可查询
     */
    private void initMinimalData() {
        String[][] minimalData = {
            // 水稻
            {"水稻稻瘟病如何防治？", "稻瘟病、稻热病", "病害", "稻瘟病菌（Magnaporthe oryzae）", "半知菌亚门、梨孢属",
             "水稻", "叶片出现暗绿色水渍状斑点，后扩大成梭形或纺锤形病斑，中央灰白色、边缘褐色。严重时叶片枯死，穗颈受害导致白穗。",
             "温度24-28℃、湿度90%以上时易发生。偏施氮肥、种植密度过大的田块发病重。",
             "主要通过气流传播分生孢子，种子也可带菌。",
             "选用抗病品种；合理施肥，避免偏施氮肥；浅水勤灌，适时晒田；清除病残体。",
             "三环唑可湿性粉剂（75%）每亩20-25克；稻瘟灵乳油（40%）每亩100毫升；春雷霉素水剂（2%）每亩100毫升。",
             "春雷霉素（生物源抗生素）；枯草芽孢杆菌可湿性粉剂。",
             "选用抗病品种是最经济有效的措施；合理密植；加强水肥管理；种子消毒处理。"},
            
            // 小麦
            {"小麦赤霉病怎么防治？", "赤霉病、红麦头", "病害", "禾谷镰刀菌（Fusarium graminearum）", "子囊菌亚门、镰刀菌属",
             "小麦", "穗部变红色或粉红色霉层，籽粒干瘪皱缩。感病麦粒含毒素（DON），人畜食用后会引起中毒。",
             "小麦抽穗扬花期遇连续阴雨天气易爆发。温度15-28℃、持续高湿是发病关键条件。",
             "土传（病残体上子囊孢子）+ 气流传播。",
             "选用抗赤霉病品种；合理轮作；开沟排水降低田间湿度；适期播种避免花期遇雨。",
             "戊唑醇悬浮剂（430g/L）每亩15-20毫升；氰烯菌酯悬浮剂（25%）每亩100-150毫升；咪鲜胺乳油（45%）每亩30毫升。",
             "枯草芽孢杆菌可湿性粉剂；木霉菌制剂。",
             "关键在抽穗扬花期（见花就打），若预报有连续阴雨需提前预防；控制氮肥用量。"},
            
            // 玉米
            {"玉米草地贪夜蛾用什么药？", "贪夜蛾、秋黏虫", "虫害", "草地贪夜蛾（Spodoptera frugiperda）", "鳞翅目、夜蛾科",
             "玉米", "幼虫啃食心叶形成排孔，严重时仅剩叶脉。高龄幼虫钻蛀茎秆和果穗造成直接减产。",
             "迁飞性害虫，夏季从南方迁入。温度25-30℃最适宜繁殖，高温干旱年份发生重。",
             "成虫迁飞扩散。",
             "秋翻冬灌消灭越冬蛹；种植诱集带；灯光诱杀成虫。",
             "甲氨基阿维菌素苯甲酸盐（5%）每亩10-15克；氯虫苯甲酰胺悬浮剂（20%）每亩10-15毫升；茚虫威悬浮剂（15%）每亩20毫升。",
             "苏云金芽孢杆菌（Bt制剂）；白僵菌颗粒剂；赤眼蜂释放。",
             "加强监测成虫迁入动态；抓住低龄幼虫期（3龄前）防治关键窗口。"},
        };

        for (String[] row : minimalData) {
            PestKnowledge pk = new PestKnowledge();
            pk.setQuestion(row[0]);
            pk.setAliases(row[1]);
            pk.setCategory(row[2]);
            pk.setPathogen(row[3]);
            pk.setTaxonomy(row[4]);
            pk.setCrops(row[5]);
            pk.setSymptoms(row[6]);
            pk.setOccurrenceRule(row[7]);
            pk.setTransmission(row[8]);
            pk.setAgriculturalControl(row[9]);
            pk.setChemicalControl(row[10]);
            pk.setBiologicalControl(row[11]);
            pk.setPrevention(row[12]);
            String crop = row[5];
            if (crop.contains("水稻")) pk.setKnowledgeCategory("粮食作物");
            else if (crop.contains("小麦")) pk.setKnowledgeCategory("粮食作物");
            else if (crop.contains("玉米")) pk.setKnowledgeCategory("粮食作物");
            else pk.setKnowledgeCategory("粮食作物");
            pk.setIcon("🌾");
            repository.save(pk);
        }
        log.info("✅ 内置最小数据初始化完成：{} 条", minimalData.length);
    }
}
