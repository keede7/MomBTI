package io.keede.mombti.core.domains.report.evidence;

import static io.keede.mombti.core.domains.report.evidence.Evidence.Status.ASSUMPTION;
import static io.keede.mombti.core.domains.report.evidence.Evidence.Status.VERIFIED;

/** 계산 공식과 기준값의 출처. docs/EVIDENCE.md의 key와 1:1로 대응한다. */
public enum Evidence {

    MIFFLIN_1990("Mifflin MD, St Jeor ST et al.", 1990,
            "A new predictive equation for resting energy expenditure in healthy individuals. Am J Clin Nutr. 1990;51(2):241-7",
            "PMID 2305711", VERIFIED),
    CUNNINGHAM_1991("Cunningham JJ", 1991,
            "Body composition as a determinant of energy expenditure: a synthetic review and a proposed general prediction equation. Am J Clin Nutr. 1991;54(6):963-9",
            "PMID 1957828", VERIFIED),
    FRANKENFIELD_2005("Frankenfield D, Roth-Yousey L, Compher C", 2005,
            "Comparison of predictive equations for resting metabolic rate in healthy nonobese and obese adults: a systematic review. J Am Diet Assoc. 2005;105(5):775-89",
            "PMID 15883556", VERIFIED),
    DEURENBERG_1991("Deurenberg P, Weststrate JA, Seidell JC", 1991,
            "Body mass index as a measure of body fatness: age- and sex-specific prediction formulas. Br J Nutr. 1991;65(2):105-14",
            "PMID 2043597", VERIFIED),
    FAO_WHO_UNU_2004("FAO/WHO/UNU", 2004,
            "Human energy requirements. Report of a Joint FAO/WHO/UNU Expert Consultation. FAO Food and Nutrition Technical Report Series 1",
            "fao.org/3/y5686e", VERIFIED),
    TUDOR_LOCKE_2004("Tudor-Locke C, Bassett DR Jr.", 2004,
            "How many steps/day are enough? Preliminary pedometer indices for public health. Sports Med. 2004;34(1):1-8",
            "PMID 14715035", VERIFIED),
    HERRMANN_2024("Herrmann SD, Willis EA, Ainsworth BE et al.", 2024,
            "2024 Adult Compendium of Physical Activities: A third update of the energy costs of human activities. J Sport Health Sci. 2024;13(1):6-12",
            "PMID 38242596", VERIFIED),
    HALL_2007_FORBES("Hall KD", 2007,
            "Body fat and fat-free mass inter-relationships: Forbes's theory revisited. Br J Nutr. 2007;97(6):1059-63",
            "PMID 17367567", VERIFIED),
    HALL_2008("Hall KD", 2008,
            "What is the required energy deficit per unit weight loss? Int J Obes. 2008;32(3):573-6",
            "PMID 17848938", VERIFIED),
    HALL_2011("Hall KD, Sacks G, Chandramohan D et al.", 2011,
            "Quantification of the effect of energy imbalance on bodyweight. Lancet. 2011;378(9793):826-37",
            "PMID 21872751", VERIFIED),
    ADAPTIVE_THERMOGENESIS("참고: Rosenbaum M, Leibel RL", 2010,
            "적자의 10%만큼 소비량이 줄어든다고 가정 (참고: Adaptive thermogenesis in humans. Int J Obes. 2010;34 Suppl 1:S47-55)",
            "PMID 20935667", ASSUMPTION),
    ADHERENCE_80("MomBTI", 0,
            "현실 보정: 계획의 80%만 지킨 경우를 가정",
            "가정", ASSUMPTION),
    EXERCISE_SESSION_45("MomBTI", 0,
            "운동 1회 = 중강도 45분으로 가정 (ADR-020)",
            "가정", ASSUMPTION),
    KSSO_2022("대한비만학회", 2022,
            "비만 진료지침 2022 (8판)",
            "지침", VERIFIED),
    HELMS_2014("Helms ER, Aragon AA, Fitschen PJ", 2014,
            "Evidence-based recommendations for natural bodybuilding contest preparation: nutrition and supplementation. J Int Soc Sports Nutr. 2014;11:20",
            "PMID 24864135", VERIFIED),
    AHA_ACC_TOS_2013("Jensen MD, Ryan DH et al.", 2013,
            "2013 AHA/ACC/TOS guideline for the management of overweight and obesity in adults. Circulation. 2014;129(25 Suppl 2):S102-38",
            "PMID 24222017", VERIFIED),
    LEIDY_2015("Leidy HJ, Clifton PM, Astrup A et al.", 2015,
            "The role of protein in weight loss and maintenance. Am J Clin Nutr. 2015;101(6):1320S-1329S",
            "PMID 25926512", VERIFIED),
    MORTON_2018("Morton RW et al.", 2018,
            "A systematic review, meta-analysis and meta-regression of the effect of protein supplementation on resistance training-induced gains in muscle mass and strength in healthy adults. Br J Sports Med. 2018;52(6):376-84",
            "PMID 28698222", VERIFIED),
    PROT_AGE_2013("Bauer J et al.", 2013,
            "Evidence-based recommendations for optimal dietary protein intake in older people: a position paper from the PROT-AGE Study Group. J Am Med Dir Assoc. 2013;14(8):542-59",
            "PMID 23867520", VERIFIED),
    IOM_AMDR_2005("Institute of Medicine", 2005,
            "Dietary Reference Intakes for Energy, Carbohydrate, Fiber, Fat, Fatty Acids, Cholesterol, Protein, and Amino Acids",
            "보고서", VERIFIED),
    WING_2011("Wing RR et al.", 2011,
            "Benefits of modest weight loss in improving cardiovascular risk factors in overweight and obese individuals with type 2 diabetes. Diabetes Care. 2011;34(7):1481-6",
            "PMID 21593294", VERIFIED),
    ESSENTIAL_FAT("참고: ACE 체지방 분류", 0,
            "체지방률 입력 하한 남 5% / 여 12% (ACE 필수지방 범위 안에서 고른 값)",
            "기관 자료", ASSUMPTION);

    public enum Status {
        /** 원문 또는 초록에서 서지 정보와 공식·계수를 확인함. */
        VERIFIED,
        /** 단순화를 위한 가정. 화면에 "가정"으로 표시해야 함. */
        ASSUMPTION
    }

    private final String authors;
    /** 출판 연도. 출처가 없는 가정은 0. */
    private final int year;
    private final String title;
    private final String identifier;
    private final Status status;

    Evidence(String authors, int year, String title, String identifier, Status status) {
        this.authors = authors;
        this.year = year;
        this.title = title;
        this.identifier = identifier;
        this.status = status;
    }

    public String key() {
        return name();
    }

    public String authors() {
        return authors;
    }

    public int year() {
        return year;
    }

    public String title() {
        return title;
    }

    public String identifier() {
        return identifier;
    }

    public Status status() {
        return status;
    }

    public boolean isAssumption() {
        return status == ASSUMPTION;
    }
}
