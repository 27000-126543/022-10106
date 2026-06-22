package com.medical.triage.config;

import com.medical.triage.entity.*;
import com.medical.triage.enums.AppointmentSource;
import com.medical.triage.enums.ConsultationType;
import com.medical.triage.enums.DepartmentType;
import com.medical.triage.enums.RiskLevel;
import com.medical.triage.enums.RuleStatus;
import com.medical.triage.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StoreRepository storeRepository;
    private final DepartmentRepository departmentRepository;
    private final ConsultantGroupRepository consultantGroupRepository;
    private final TriageRuleRepository triageRuleRepository;
    private final RiskKeywordRepository riskKeywordRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireQuestionRepository questionRepository;

    @Override
    public void run(String... args) {
        if (storeRepository.count() > 0) {
            log.info("测试数据已存在，跳过初始化");
            return;
        }

        log.info("开始初始化测试数据...");

        Store store1 = createStore("北京朝阳店", "BJCY001", "北京市朝阳区建国路88号", "010-88888888", "北京", "北京市");
        Store store2 = createStore("上海浦东店", "SHPD001", "上海市浦东新区陆家嘴环路100号", "021-66666666", "上海", "上海市");
        Store store3 = createStore("广州天河店", "GZTH001", "广州市天河区体育西路191号", "020-33333333", "广东", "广州市");

        createDepartments(store1);
        createDepartments(store2);
        createDepartments(store3);

        createConsultantGroups(store1);
        createConsultantGroups(store2);
        createConsultantGroups(store3);

        createTriageRules(store1);
        createTriageRules(store2);
        createTriageRules(store3);

        createRiskKeywords();

        createQuestionnaires();

        log.info("测试数据初始化完成");
    }

    private Store createStore(String name, String code, String address, String phone, String province, String city) {
        Store store = Store.builder()
                .name(name)
                .code(code)
                .address(address)
                .phone(phone)
                .province(province)
                .city(city)
                .status(1)
                .build();
        return storeRepository.save(store);
    }

    private void createDepartments(Store store) {
        List<Department> departments = Arrays.asList(
                Department.builder().storeId(store.getId()).name("整形美容科").departmentType(DepartmentType.PLASTIC_SURGERY)
                        .description("眼部整形、鼻部整形、面部轮廓等手术项目").capacity(50).status(1).build(),
                Department.builder().storeId(store.getId()).name("皮肤美容科").departmentType(DepartmentType.DERMATOLOGY)
                        .description("祛斑、祛痘、嫩肤、脱毛等皮肤项目").capacity(80).status(1).build(),
                Department.builder().storeId(store.getId()).name("注射美容科").departmentType(DepartmentType.INJECTION)
                        .description("玻尿酸、肉毒素、胶原蛋白等注射项目").capacity(60).status(1).build(),
                Department.builder().storeId(store.getId()).name("麻醉科").departmentType(DepartmentType.ANESTHESIOLOGY)
                        .description("手术麻醉支持").capacity(30).status(1).build()
        );
        departmentRepository.saveAll(departments);
    }

    private void createConsultantGroups(Store store) {
        List<Department> departments = departmentRepository.findByStoreId(store.getId());

        for (Department dept : departments) {
            ConsultantGroup group1 = ConsultantGroup.builder()
                    .storeId(store.getId())
                    .departmentId(dept.getId())
                    .name(dept.getName() + "咨询一组")
                    .leader("张医生")
                    .memberCount(5)
                    .specialtyTags(Arrays.asList("高风险处理", "复杂案例"))
                    .status(1)
                    .build();

            ConsultantGroup group2 = ConsultantGroup.builder()
                    .storeId(store.getId())
                    .departmentId(dept.getId())
                    .name(dept.getName() + "咨询二组")
                    .leader("李医生")
                    .memberCount(4)
                    .specialtyTags(Arrays.asList("常规项目", "首次咨询"))
                    .status(1)
                    .build();

            consultantGroupRepository.saveAll(Arrays.asList(group1, group2));
        }
    }

    private void createTriageRules(Store store) {
        List<TriageRule> rules = Arrays.asList(
                TriageRule.builder()
                        .storeId(store.getId())
                        .ruleCode("RULE_EYE_SURGERY")
                        .name("双眼皮手术分诊")
                        .version(1)
                        .status(RuleStatus.OFFICIAL)
                        .isOfficial(true)
                        .consultationType(ConsultationType.SURGERY_CONSULTATION)
                        .departmentType(DepartmentType.PLASTIC_SURGERY)
                        .keywords(Arrays.asList("双眼皮", "全切", "埋线", "眼综合", "开眼角"))
                        .priority(100)
                        .isEnabled(true)
                        .publishedBy("系统初始化")
                        .publishedAt(LocalDateTime.now())
                        .description("眼部整形手术项目分诊规则")
                        .build(),
                TriageRule.builder()
                        .storeId(store.getId())
                        .ruleCode("RULE_NOSE_SURGERY")
                        .name("鼻部手术分诊")
                        .version(1)
                        .status(RuleStatus.OFFICIAL)
                        .isOfficial(true)
                        .consultationType(ConsultationType.SURGERY_CONSULTATION)
                        .departmentType(DepartmentType.PLASTIC_SURGERY)
                        .keywords(Arrays.asList("隆鼻", "鼻综合", "鼻翼", "鼻尖"))
                        .priority(90)
                        .isEnabled(true)
                        .publishedBy("系统初始化")
                        .publishedAt(LocalDateTime.now())
                        .description("鼻部整形手术项目分诊规则")
                        .build(),
                TriageRule.builder()
                        .storeId(store.getId())
                        .ruleCode("RULE_SKIN_CARE")
                        .name("皮肤项目分诊")
                        .version(1)
                        .status(RuleStatus.OFFICIAL)
                        .isOfficial(true)
                        .consultationType(ConsultationType.SKIN_CARE_CONSULTATION)
                        .departmentType(DepartmentType.DERMATOLOGY)
                        .keywords(Arrays.asList("祛斑", "祛痘", "嫩肤", "脱毛", "水光针"))
                        .priority(80)
                        .isEnabled(true)
                        .publishedBy("系统初始化")
                        .publishedAt(LocalDateTime.now())
                        .description("皮肤美容项目分诊规则")
                        .build(),
                TriageRule.builder()
                        .storeId(store.getId())
                        .ruleCode("RULE_INJECTION")
                        .name("注射项目分诊")
                        .version(1)
                        .status(RuleStatus.OFFICIAL)
                        .isOfficial(true)
                        .consultationType(ConsultationType.INJECTION_CONSULTATION)
                        .departmentType(DepartmentType.INJECTION)
                        .keywords(Arrays.asList("玻尿酸", "肉毒素", "瘦脸针", "除皱针", "填充"))
                        .priority(85)
                        .isEnabled(true)
                        .publishedBy("系统初始化")
                        .publishedAt(LocalDateTime.now())
                        .description("注射美容项目分诊规则")
                        .build(),
                TriageRule.builder()
                        .storeId(store.getId())
                        .ruleCode("RULE_BREAST_SURGERY")
                        .name("胸部手术分诊")
                        .version(1)
                        .status(RuleStatus.OFFICIAL)
                        .isOfficial(true)
                        .consultationType(ConsultationType.SURGERY_CONSULTATION)
                        .departmentType(DepartmentType.PLASTIC_SURGERY)
                        .keywords(Arrays.asList("隆胸", "丰胸", "假体", "自体脂肪丰胸"))
                        .priority(95)
                        .maxAge(55)
                        .isEnabled(true)
                        .publishedBy("系统初始化")
                        .publishedAt(LocalDateTime.now())
                        .description("胸部整形手术项目分诊规则")
                        .build(),
                TriageRule.builder()
                        .storeId(store.getId())
                        .ruleCode("RULE_EYE_SURGERY")
                        .name("双眼皮手术分诊_v2_灰度")
                        .version(2)
                        .status(RuleStatus.GRAY)
                        .isOfficial(false)
                        .parentRuleId(1L)
                        .consultationType(ConsultationType.SURGERY_CONSULTATION)
                        .departmentType(DepartmentType.PLASTIC_SURGERY)
                        .keywords(Arrays.asList("双眼皮", "全切", "埋线", "眼综合", "开眼角", "眼部修复"))
                        .graySources(Arrays.asList(AppointmentSource.MINI_PROGRAM))
                        .grayProjects(Arrays.asList(ConsultationType.SURGERY_CONSULTATION))
                        .grayPercentage(30)
                        .priority(100)
                        .minAge(18)
                        .maxAge(50)
                        .isEnabled(true)
                        .publishedBy("系统初始化")
                        .publishedAt(LocalDateTime.now())
                        .description("眼部整形手术项目分诊规则_v2灰度版，新增眼部修复关键词，限制18-50岁，仅对小程序来源30%流量生效")
                        .build()
        );
        triageRuleRepository.saveAll(rules);
    }

    private void createRiskKeywords() {
        List<RiskKeyword> keywords = Arrays.asList(
                RiskKeyword.builder().keyword("瘢痕体质").riskLevel(RiskLevel.HIGH).needDoctorAssessment(true)
                        .description("瘢痕体质患者手术需谨慎评估").build(),
                RiskKeyword.builder().keyword("疤痕体质").riskLevel(RiskLevel.HIGH).needDoctorAssessment(true)
                        .description("疤痕体质患者手术需谨慎评估").build(),
                RiskKeyword.builder().keyword("过敏体质").riskLevel(RiskLevel.MEDIUM).needDoctorAssessment(false)
                        .description("过敏体质需注意药物过敏").build(),
                RiskKeyword.builder().keyword("严重心脏病").riskLevel(RiskLevel.EXTREME).needDoctorAssessment(true)
                        .description("严重心脏病患者禁止手术").build(),
                RiskKeyword.builder().keyword("高血压").riskLevel(RiskLevel.HIGH).needDoctorAssessment(true)
                        .description("高血压患者需控制血压后评估").build(),
                RiskKeyword.builder().keyword("糖尿病").riskLevel(RiskLevel.HIGH).needDoctorAssessment(true)
                        .description("糖尿病患者伤口愈合慢，需评估").build(),
                RiskKeyword.builder().keyword("孕妇").riskLevel(RiskLevel.EXTREME).needDoctorAssessment(true)
                        .description("孕妇禁止任何医美项目").build(),
                RiskKeyword.builder().keyword("哺乳期").riskLevel(RiskLevel.HIGH).needDoctorAssessment(true)
                        .description("哺乳期建议暂缓医美项目").build(),
                RiskKeyword.builder().keyword("精神疾病").riskLevel(RiskLevel.EXTREME).needDoctorAssessment(true)
                        .description("精神疾病患者需精神科评估后决定").build()
        );
        riskKeywordRepository.saveAll(keywords);
    }

    private void createQuestionnaires() {
        Questionnaire questionnaire = Questionnaire.builder()
                .title("初诊问卷")
                .version("1.0")
                .description("初诊顾客填写的健康和诉求问卷")
                .targetConsultationType(ConsultationType.COMPREHENSIVE)
                .isActive(true)
                .build();
        questionnaire = questionnaireRepository.save(questionnaire);

        List<QuestionnaireQuestion> questions = Arrays.asList(
                createQuestion(questionnaire.getId(), "您的主要诉求是什么？", "TEXT", 1, true,
                        Arrays.asList("瘢痕体质", "疤痕体质", "过敏体质")),
                createQuestion(questionnaire.getId(), "您是否有慢性疾病？（如高血压、糖尿病、心脏病等）", "TEXT", 2, true,
                        Arrays.asList("严重心脏病", "高血压", "糖尿病")),
                createQuestion(questionnaire.getId(), "您是否为过敏体质？", "SINGLE_CHOICE", 3, true,
                        Arrays.asList("过敏体质")),
                createQuestion(questionnaire.getId(), "您是否处于孕期或哺乳期？", "SINGLE_CHOICE", 4, true,
                        Arrays.asList("孕妇", "哺乳期")),
                createQuestion(questionnaire.getId(), "您是否有精神疾病史？", "SINGLE_CHOICE", 5, false,
                        Arrays.asList("精神疾病")),
                createQuestion(questionnaire.getId(), "您之前是否接受过医美手术？", "TEXT", 6, false, null),
                createQuestion(questionnaire.getId(), "您期望的改善部位是？", "TEXT", 7, true, null),
                createQuestion(questionnaire.getId(), "您有药物过敏史吗？如有请说明。", "TEXT", 8, false, null)
        );
        questionRepository.saveAll(questions);
    }

    private QuestionnaireQuestion createQuestion(Long questionnaireId, String questionText, String questionType,
                                                  int sortOrder, boolean isRequired, List<String> riskKeywords) {
        return QuestionnaireQuestion.builder()
                .questionnaireId(questionnaireId)
                .questionText(questionText)
                .questionType(questionType)
                .sortOrder(sortOrder)
                .isRequired(isRequired)
                .riskKeywords(riskKeywords)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
