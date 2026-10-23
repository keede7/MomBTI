# 근거 (공식과 출처)

코드의 `Evidence` enum은 이 문서의 key와 1:1로 대응한다.
**상태가 "검증됨"인 항목만 코드에서 사용할 수 있다** (CLAUDE.md).

상태 값
- **검증됨**: 원문 또는 PubMed에서 서지 정보와 공식·계수를 확인함
- **확인 필요**: 서지 정보는 작성했지만 원문 대조가 아직 안 됨. step0에서 PubMed/DOI로 대조한 뒤 "검증됨"으로 바꾼다
- **가정**: 논문 공식이 아니라 단순화를 위한 가정. 화면에 "가정"으로 표시해야 한다

## 계산 공식
| key | 용도 | 공식·계수 | 출처 | 식별자 | 적용 범위·한계 | 상태 |
|---|---|---|---|---|---|---|
| `MIFFLIN_1990` | 기초대사량 (기본) | 남 10W + 6.25H − 5A + 5 / 여 10W + 6.25H − 5A − 161 (W kg, H cm, A 세) | Mifflin MD, St Jeor ST et al. A new predictive equation for resting energy expenditure in healthy individuals. Am J Clin Nutr. 1990;51(2):241-7 | PMID 2305711 | 19~78세 건강 성인에서 개발 | 확인 필요 |
| `KATCH_MCARDLE` | 기초대사량 (체지방률 입력 시) | 370 + 21.6 × 제지방량(kg) | McArdle WD, Katch FI, Katch VL. Exercise Physiology (교과서) | 교과서 | 체지방률 측정 오차에 민감 | 확인 필요 |
| `FRANKENFIELD_2005` | BMR 공식 정확도, ±10% 밴드 근거 | Mifflin이 비만·비비만 성인에서 측정값 ±10% 이내 비율이 가장 높음 | Frankenfield D et al. Comparison of predictive equations for resting metabolic rate in healthy nonobese and obese adults: a systematic review. J Am Diet Assoc. 2005;105(5):775-89 | PMID 15883556 | | 확인 필요 |
| `DEURENBERG_1991` | 체지방률 추정 | 1.2 × BMI + 0.23 × 나이 − 10.8 × 성별(남 1, 여 0) − 5.4 | Deurenberg P, Weststrate JA, Seidell JC. Body mass index as a measure of body fatness: age- and sex-specific prediction formulas. Br J Nutr. 1991;65(2):105-14 | PMID 2043597 | 성인 대상, 근육량이 많으면 과대 추정 | 확인 필요 |
| `FAO_WHO_UNU_2004` | 활동계수 PAL | 좌식 1.40~1.69 / 활동적 1.70~1.99 / 매우 활동적 2.00~2.40 | FAO/WHO/UNU. Human energy requirements. Report of a Joint Expert Consultation. 2004 (FAO Food and Nutrition Technical Report Series 1) | 보고서 | 5단계 값(1.40/1.55/1.70/1.85/2.00)은 이 구간 안에서 고른 값 | 확인 필요 |
| `TUDOR_LOCKE_2004` | 활동량 단계 설명 (걸음 수) | <5,000 좌식 / 5,000~7,499 낮은 활동 / 7,500~9,999 약간 활동 / 10,000~12,499 활동 / ≥12,500 매우 활동 | Tudor-Locke C, Bassett DR Jr. How many steps/day are enough? Sports Med. 2004;34(1):1-8 | PMID 14715035 | 걸음 수와 PAL의 대응은 근사 | 확인 필요 |
| `AINSWORTH_2011` | 운동 소비량 | 빠르게 걷기(약 5.6km/h, 평지) MET 4.3. 순소비 = (MET − 1) × 체중 × 시간 | Ainsworth BE et al. 2011 Compendium of Physical Activities. Med Sci Sports Exerc. 2011;43(8):1575-81 | PMID 21681120 | 1 MET ≈ 1kcal/kg/h 근사 | 확인 필요 |
| `HALL_2007_FORBES` | 지방·제지방 분배 | p = C / (C + F), C = 10.4kg | Hall KD. Body fat and fat-free mass inter-relationships: Forbes's theory revisited. Br J Nutr. 2007;97(6):1059-63 | PMID 17367567 | | 확인 필요 |
| `HALL_2011` | 동적 체중모델, 에너지 밀도 | 지방 9,440kcal/kg, 제지방 1,816kcal/kg. 7,700kcal 정적 규칙의 과대평가 | Hall KD et al. Quantification of the effect of energy imbalance on bodyweight. Lancet. 2011;378(9793):826-37 | PMID 21872751 | MVP는 단순화 모델 (ARCHITECTURE.md) | 확인 필요 |
| `ADAPTIVE_THERMOGENESIS` | 적응성 열발생 | 적자의 10%만큼 소비량이 줄어든다고 가정 | 참고: Rosenbaum M, Leibel RL. Adaptive thermogenesis in humans. Int J Obes. 2010;34 Suppl 1:S47-55 | PMID 20935667 | 10%는 단순화한 값 | 가정 |
| `ADHERENCE_80` | 현실 보정 라인 | 섭취 = 목표 + 20% × (소비량 − 목표) | 없음 | | 화면에 "가정" 표기 | 가정 |
| `EXERCISE_SESSION_45` | 운동 1회 길이 | 45분 | 없음 (ADR-020) | | 화면에 "가정" 표기 | 가정 |

## 기준값과 권고
| key | 용도 | 값 | 출처 | 식별자 | 상태 |
|---|---|---|---|---|---|
| `KSSO_2022` | BMI 구간 | <18.5 / 18.5~22.9 / 23~24.9 / 25~29.9 / ≥30 | 대한비만학회. 비만 진료지침 2022 (8판) | 지침 | 확인 필요 |
| `HELMS_2014` | 안전 감량 속도 | 주당 체중의 0.5~1% | Helms ER, Aragon AA, Fitschen PJ. Evidence-based recommendations for natural bodybuilding contest preparation: nutrition and supplementation. J Int Soc Sports Nutr. 2014;11:20 | PMID 24864135 | 확인 필요 |
| `NHLBI_1998` | 칼로리 하한 | 여 1,000~1,200 / 남 1,200~1,600kcal 범위의 저칼로리 식단 권고. MVP 하한은 여 1,200 / 남 1,500 | NHLBI. Clinical Guidelines on the Identification, Evaluation, and Treatment of Overweight and Obesity in Adults. 1998 | 지침 | 확인 필요 |
| `MORTON_2018` | 단백질 | 1.6g/kg/일 (메타분석의 효과 정체 지점) | Morton RW et al. A systematic review, meta-analysis and meta-regression of the effect of protein supplementation on resistance training-induced gains in muscle mass and strength in healthy adults. Br J Sports Med. 2018;52(6):376-84 | PMID 28698222 | 확인 필요 |
| `PROT_AGE_2013` | 65세 이상 단백질 | 1.0~1.2g/kg/일 이상 | Bauer J et al. Evidence-based recommendations for optimal dietary protein intake in older people: a position paper from the PROT-AGE Study Group. J Am Med Dir Assoc. 2013;14(8):542-59 | PMID 23867520 | 확인 필요 |
| `IOM_AMDR_2005` | 지방·탄수화물 비율 | 지방 20~35%, 탄수화물 45~65% | Institute of Medicine. Dietary Reference Intakes for Energy, Carbohydrate, Fiber, Fat, Fatty Acids, Cholesterol, Protein, and Amino Acids. 2005 | 보고서 | 확인 필요 |
| `WING_2011` | −5% 마일스톤 | 5~10% 감량에서 심혈관 위험 요인 개선 | Wing RR et al. Benefits of modest weight loss in improving cardiovascular risk factors in overweight and obese individuals with type 2 diabetes. Diabetes Care. 2011;34(7):1481-6 | PMID 21593294 | 확인 필요 |
| `ESSENTIAL_FAT` | 체지방률 입력 하한 | 남 5% / 여 12% (필수지방 근사) | ACE 체지방 분류 (교과서·기관 자료) | 기관 자료 | 확인 필요 |

## step0 검증 절차
1. 각 항목의 식별자(PMID/DOI/보고서)로 원문 또는 초록을 찾는다.
2. 공식 계수, 단위, 적용 범위가 이 표와 같은지 대조한다.
3. 일치하면 상태를 "검증됨"으로 바꾸고, 다르면 표를 고친 뒤 PRD 계산 규칙도 함께 고친다.
4. 원문을 확인할 수 없는 항목은 대체 출처를 찾거나 "가정"으로 내리고 화면에 표시한다.
