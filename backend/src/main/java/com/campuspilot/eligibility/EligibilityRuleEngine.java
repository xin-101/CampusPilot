package com.campuspilot.eligibility;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 资格规则引擎(确定性判断)
 * 原则：规则引擎负责最终判断，LLM仅负责解释。
 * 状态判定：
 * - 任一必选规则不通过 -> NOT_ELIGIBLE
 * - 任一必选规则因字段缺失无法评估 -> INSUFFICIENT_DATA
 * - 全部必选通过，存在可选规则未通过 -> CONDITIONALLY_ELIGIBLE
 * - 全部规则通过 -> ELIGIBLE
 */
@Component
public class EligibilityRuleEngine {

    private final EligibilityRuleService ruleService;

    public EligibilityRuleEngine(EligibilityRuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * 执行资格判断
     * @param student 学生画像(DEMO)
     * @param category 政策分类(为空时评估所有启用规则)
     * @return 结构化结果
     */
    public EligibilityResult evaluate(StudentProfile student, String category) {
        return evaluate(student, category, null);
    }

    /**
     * 执行资格判断(按来源政策精确取规则)
     * @param student 学生画像(DEMO)
     * @param category 政策分类(在 policyId 无法命中时按分类兜底)
     * @param policyId 已识别的政策ID(source_policy_id 关联规则)
     * @return 结构化结果
     */
    public EligibilityResult evaluate(StudentProfile student, String category, Long policyId) {
        if (student == null) {
            return EligibilityResult.empty();
        }

        List<EligibilityRule> rules = null;
        if (policyId != null) {
            rules = ruleService.getEnabledRulesByPolicy(policyId);
        }
        if (rules == null || rules.isEmpty()) {
            rules = ruleService.getEnabledRulesByCategory(category);
        }
        if (rules.isEmpty()) {
            return EligibilityResult.empty();
        }

        List<RuleCheck> matched = new ArrayList<>();
        List<RuleCheck> failed = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int weightedTotal = 0;
        int weightedPass = 0;

        for (EligibilityRule rule : rules) {
            Object actual = resolveField(student, rule.getFieldName());
            String actualText = actual == null ? null : String.valueOf(actual);

            if (actual == null || String.valueOf(actual).isBlank()) {
                RuleCheck check = RuleCheck.builder()
                    .ruleId(rule.getRuleId())
                    .name(rule.getName())
                    .field(rule.getFieldName())
                    .operator(rule.getOperator())
                    .expectedValue(rule.getExpectedValue())
                    .actualValue(null)
                    .passed(false)
                    .required(rule.getRequired() != null && rule.getRequired() == 1)
                    .weight(rule.getWeight() != null ? rule.getWeight() : 10)
                    .message("学生字段缺失，无法评估")
                    .build();
                if (check.isRequired()) {
                    missing.add(rule.getFieldName() + "(" + rule.getName() + ")");
                }
                failed.add(check);
                weightedTotal += check.getWeight();
                continue;
            }

            boolean pass = match(rule.getOperator(), actualText, rule.getExpectedValue());
            RuleCheck check = RuleCheck.builder()
                .ruleId(rule.getRuleId())
                .name(rule.getName())
                .field(rule.getFieldName())
                .operator(rule.getOperator())
                .expectedValue(rule.getExpectedValue())
                .actualValue(actualText)
                .passed(pass)
                .required(rule.getRequired() != null && rule.getRequired() == 1)
                .weight(rule.getWeight() != null ? rule.getWeight() : 10)
                .build();
            weightedTotal += check.getWeight();
            if (pass) {
                weightedPass += check.getWeight();
                matched.add(check);
            } else {
                failed.add(check);
            }
        }

        double score = weightedTotal == 0 ? 0 : Math.round(weightedPass * 100.0 / weightedTotal);

        // 状态判定
        boolean anyRequiredFailed = failed.stream()
            .filter(c -> c.isRequired())
            .anyMatch(c -> !c.isPassed() && c.getActualValue() != null);
        boolean anyRequiredMissing = failed.stream()
            .filter(c -> c.isRequired())
            .anyMatch(c -> c.getActualValue() == null);

        boolean anyOptionalFailed = failed.stream().anyMatch(c -> !c.isRequired());

        String status;
        if (anyRequiredFailed) {
            status = "NOT_ELIGIBLE";
        } else if (anyRequiredMissing) {
            status = "INSUFFICIENT_DATA";
        } else if (anyOptionalFailed) {
            status = "CONDITIONALLY_ELIGIBLE";
        } else {
            status = "ELIGIBLE";
        }

        boolean eligible = "ELIGIBLE".equals(status);

        return EligibilityResult.builder()
            .eligible(eligible)
            .status(status)
            .score(score)
            .matchedRules(matched)
            .failedRules(failed)
            .missingConditions(missing)
            .evidence(buildEvidence(student))
            .policySources(new ArrayList<>())
            .explanation(buildExplanation(status, matched, failed))
            .demo(true)
            .checkedAt(LocalDateTime.now())
            .build();
    }

    /**
     * 解析学生字段值
     */
    private Object resolveField(StudentProfile student, String field) {
        if (field == null) {
            return null;
        }
        switch (field.toUpperCase(Locale.ROOT)) {
            case "GRADE": return student.getGrade();
            case "GPA": return student.getGpa();
            case "RANK": return student.getRank();
            case "STATUS": return student.getEnrollmentStatus();
            case "HARDSHIP": return student.getHardship();
            default: return null;
        }
    }

    /**
     * 运算符匹配(确定性)
     */
    boolean match(String operator, String actual, String expected) {
        if (expected == null) {
            return true;
        }
        String op = operator == null ? "EQ" : operator.toUpperCase(Locale.ROOT);
        switch (op) {
            case "EQ":
                return expected.equalsIgnoreCase(actual);
            case "IN":
                for (String item : expected.split(",")) {
                    if (item.trim().equalsIgnoreCase(actual)) {
                        return true;
                    }
                }
                return false;
            case "CONTAINS":
                return actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
            case "GT":
            case "GTE":
            case "LT":
            case "LTE":
                return numericCompare(op, actual, expected);
            default:
                return expected.equalsIgnoreCase(actual);
        }
    }

    private boolean numericCompare(String op, String actual, String expected) {
        try {
            double a = Double.parseDouble(actual.trim());
            double e = Double.parseDouble(expected.trim());
            switch (op) {
                case "GT": return a > e;
                case "GTE": return a >= e;
                case "LT": return a < e;
                case "LTE": return a <= e;
                default: return false;
            }
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private Map<String, Object> buildEvidence(StudentProfile student) {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("studentId", student.getStudentId());
        evidence.put("name", student.getName());
        evidence.put("grade", student.getGrade());
        evidence.put("gpa", student.getGpa());
        evidence.put("rank", student.getRank());
        evidence.put("enrollmentStatus", student.getEnrollmentStatus());
        evidence.put("hardship", student.getHardship());
        evidence.put("major", student.getMajor());
        evidence.put("demo", true);
        return evidence;
    }

    private String buildExplanation(String status, List<RuleCheck> matched, List<RuleCheck> failed) {
        StringBuilder sb = new StringBuilder("资格判断结果：");
        switch (status) {
            case "ELIGIBLE":
                sb.append("符合条件 ✅").append("\n");
                break;
            case "CONDITIONALLY_ELIGIBLE":
                sb.append("基本符合（部分可选条件未满足）").append("\n");
                break;
            case "INSUFFICIENT_DATA":
                sb.append("资料不足，无法判断").append("\n");
                break;
            default:
                sb.append("不符合条件 ❌").append("\n");
                break;
        }
        for (RuleCheck c : matched) {
            sb.append("- ✅ ").append(c.getName()).append("（").append(c.getActualValue()).append("）\n");
        }
        for (RuleCheck c : failed) {
            sb.append("- ❌ ").append(c.getName());
            if (c.getActualValue() == null) {
                sb.append("：字段缺失，无法评估");
            } else {
                sb.append("：当前值 ").append(c.getActualValue())
                  .append("，要求 ").append(c.getExpectedValue());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}