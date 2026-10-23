# 근거 (공식과 출처)

코드의 `Evidence` enum은 이 문서의 key와 1:1로 대응한다.
**상태가 "검증됨"인 항목만 코드에서 사용할 수 있다** (CLAUDE.md).
여러 출처가 다르면 **최신 판·원 출처를 우선**한다.

상태 값
- **검증됨**: 원문 또는 PubMed 초록에서 서지 정보와 공식·계수를 확인함
- **가정**: 논문 공식이 아니라 단순화를 위한 가정이거나, 1차 출처를 확인하지 못해 범위 안에서 고른 값. 화면에 "가정"으로 표시해야 한다

검증일: 2026-10-23 (NCBI E-utilities 초록, PMC 본문, FAO 원문, 2011·2024 Compendium 표 대조)

## 계산 공식
| key | 용도 | 공식·계수 | 출처 | 식별자 | 적용 범위·한계 | 상태 |
|---|---|---|---|---|---|---|
| `MIFFLIN_1990` | 기초대사량 (기본) | 남 10W + 6.25H − 5A + 5 / 여 10W + 6.25H − 5A − 161 (W kg, H cm, A 세) | Mifflin MD, St Jeor ST et al. A new predictive equation for resting energy expenditure in healthy individuals. Am J Clin Nutr. 1990;51(2):241-7 | PMID 2305711 | 19~78세 건강 성인 498명으로 개발 | 검증됨 |
| `CUNNINGHAM_1991` | 기초대사량 (체지방률 입력 시). 흔히 "Katch-McArdle 식"으로 불림 | 370 + 21.6 × 제지방량(kg) | Cunningham JJ. Body composition as a determinant of energy expenditure: a synthetic review and a proposed general prediction equation. Am J Clin Nutr. 1991;54(6):963-9 | PMID 1957828 | 넓은 체중 범위의 성인. 체지방률 측정 오차에 민감 | 검증됨 |
| `FRANKENFIELD_2005` | BMR 공식 선택, ±10% 밴드 근거 | Mifflin이 비만·비비만 성인 모두에서 측정값 ±10% 이내로 예측한 비율이 가장 높음 | Frankenfield D, Roth-Yousey L, Compher C. Comparison of predictive equations for resting metabolic rate in healthy nonobese and obese adults: a systematic review. J Am Diet Assoc. 2005;105(5):775-89 | PMID 15883556 | ±10%를 벗어나는 개인도 있음 (밴드는 대표 오차) | 검증됨 |
| `DEURENBERG_1991` | 체지방률 추정 | 1.20 × BMI + 0.23 × 나이 − 10.8 × 성별(남 1, 여 0) − 5.4 | Deurenberg P, Weststrate JA, Seidell JC. Body mass index as a measure of body fatness: age- and sex-specific prediction formulas. Br J Nutr. 1991;65(2):105-14 | PMID 2043597 | 성인 대상, 개발 표본 BMI 13.9~40.9, 추정 오차(SEE) 4.1%p. BMI 41 이상은 범위 밖 추정이므로 "오차가 클 수 있음" 표시. 근육량이 많으면 과대 추정 | 검증됨 |
| `FAO_WHO_UNU_2004` | 활동계수 PAL | 좌식·가벼운 활동 1.40~1.69 / 활동적 1.70~1.99 / 매우 활동적 2.00~2.40 (Table 5.3) | FAO/WHO/UNU. Human energy requirements. Report of a Joint FAO/WHO/UNU Expert Consultation. FAO Food and Nutrition Technical Report Series 1. 2004 | 보고서 (fao.org/3/y5686e) | 5단계 값(1.40/1.55/1.70/1.85/2.00)은 이 구간 안에서 고른 값. 2.40 초과는 장기 유지가 어려움 | 검증됨 |
| `TUDOR_LOCKE_2004` | 활동량 단계 설명 (걸음 수) | <5,000 좌식 / 5,000~7,499 낮은 활동 / 7,500~9,999 약간 활동 / ≥10,000 활동 / >12,500 매우 활동 | Tudor-Locke C, Bassett DR Jr. How many steps/day are enough? Preliminary pedometer indices for public health. Sports Med. 2004;34(1):1-8 | PMID 14715035 | 걸음 수와 PAL의 대응은 근사 | 검증됨 |
| `HERRMANN_2024` | 운동 소비량 | 코드 17200 빠르게 걷기(3.5~3.9mph ≈ 5.6~6.3km/h, 평지, 운동 목적) **MET 4.8**. 순소비 = (MET − 1) × 체중 × 시간 | Herrmann SD, Willis EA, Ainsworth BE et al. 2024 Adult Compendium of Physical Activities: A third update of the energy costs of human activities. J Sport Health Sci. 2024;13(1):6-12 | PMID 38242596 | 2011판(Ainsworth 2011, PMID 21681120)의 같은 코드는 4.3이었으나 최신판 우선. 1 MET ≈ 1kcal/kg/h 근사 | 검증됨 |
| `HALL_2007_FORBES` | 지방·제지방 분배 | 제지방 비율 p = dFFM/dBW = 10.4 / (10.4 + F), F는 지방량(kg) | Hall KD. Body fat and fat-free mass inter-relationships: Forbes's theory revisited. Br J Nutr. 2007;97(6):1059-63 | PMID 17367567 | 상수 10.4는 Forbes의 여성 횡단 자료에서 유도. 하루 단위의 작은 변화에 적용하므로 미분형 사용 | 검증됨 |
| `HALL_2008` | 에너지 밀도 (원 출처) | 지방 39.5MJ/kg ≈ 9,440kcal/kg, 제지방 7.6MJ/kg ≈ 1,816kcal/kg | Hall KD. What is the required energy deficit per unit weight loss? Int J Obes. 2008;32(3):573-6 | PMID 17848938 | | 검증됨 |
| `HALL_2011` | 동적 체중모델 | 위 에너지 밀도 사용. 7,700kcal/kg 정적 규칙은 장기 감량을 과대평가 | Hall KD, Sacks G, Chandramohan D et al. Quantification of the effect of energy imbalance on bodyweight. Lancet. 2011;378(9793):826-37 | PMID 21872751 | MVP는 단순화 모델 (ARCHITECTURE.md) | 검증됨 |
| `ADAPTIVE_THERMOGENESIS` | 적응성 열발생 | 적자의 10%만큼 소비량이 줄어든다고 가정 | 참고: Rosenbaum M, Leibel RL. Adaptive thermogenesis in humans. Int J Obes. 2010;34 Suppl 1:S47-55 | PMID 20935667 | 10%는 단순화한 값 | 가정 |
| `ADHERENCE_80` | 현실 보정 라인 | 섭취 = 목표 + 20% × (소비량 − 목표) | 없음 | | 화면에 "가정" 표기 | 가정 |
| `EXERCISE_SESSION_45` | 운동 1회 길이 | 45분 | 없음 (ADR-020) | | 화면에 "가정" 표기 | 가정 |

## 기준값과 권고
| key | 용도 | 값 | 출처 | 식별자 | 한계 | 상태 |
|---|---|---|---|---|---|---|
| `KSSO_2022` | BMI 구간 | <18.5 저체중 / 18.5~22.9 정상 / 23~24.9 비만 전단계 / 25~29.9 1단계 / 30~34.9 2단계 / ≥35 3단계. MVP 화면은 ≥30을 한 구간으로 묶음 | 대한비만학회. 비만 진료지침 2022 (8판) | 지침 | 2025년 학회에서 기준 유지 확인 | 검증됨 |
| `HELMS_2014` | 안전 감량 속도 | 주당 체중의 0.5~1% | Helms ER, Aragon AA, Fitschen PJ. Evidence-based recommendations for natural bodybuilding contest preparation: nutrition and supplementation. J Int Soc Sports Nutr. 2014;11:20 | PMID 24864135 | 대상은 자연 보디빌더. 일반인에게는 보수적 상한으로 사용 | 검증됨 |
| `AHA_ACC_TOS_2013` | 칼로리 하한 | 감량 식단 처방 여 1,200~1,500 / 남 1,500~1,800kcal. MVP 하한은 범위 하단인 여 1,200 / 남 1,500 | Jensen MD, Ryan DH et al. 2013 AHA/ACC/TOS guideline for the management of overweight and obesity in adults. Circulation. 2014;129(25 Suppl 2):S102-38 | PMID 24222017 | 숫자는 지침 요약 자료 2곳에서 일치 확인. 처방 범위이지 절대 하한은 아님 | 검증됨 |
| `LEIDY_2015` | 단백질 (주 근거) | 감량·유지에 1.2~1.6g/kg/일의 고단백 식단. MVP는 상단 1.6 사용 | Leidy HJ, Clifton PM, Astrup A et al. The role of protein in weight loss and maintenance. Am J Clin Nutr. 2015;101(6):1320S-1329S | PMID 25926512 | | 검증됨 |
| `MORTON_2018` | 단백질 (보조 근거) | 1.62g/kg/일 이상에서 근육 증가 효과 정체 | Morton RW et al. A systematic review, meta-analysis and meta-regression of the effect of protein supplementation on resistance training-induced gains in muscle mass and strength in healthy adults. Br J Sports Med. 2018;52(6):376-84 | PMID 28698222 | 근력운동 중 근육 증가 연구. 감량 대상 연구 아님 | 검증됨 |
| `PROT_AGE_2013` | 65세 이상 단백질 | 최소 1.0~1.2g/kg/일. 활동적인 노인은 1.2g/kg 이상 | Bauer J et al. Evidence-based recommendations for optimal dietary protein intake in older people: a position paper from the PROT-AGE Study Group. J Am Med Dir Assoc. 2013;14(8):542-59 | PMID 23867520 | 중증 신장질환 제외 | 검증됨 |
| `IOM_AMDR_2005` | 지방 비율 | 지방 20~35% (탄수화물 45~65%, 단백질 10~35%) | Institute of Medicine. Dietary Reference Intakes for Energy, Carbohydrate, Fiber, Fat, Fatty Acids, Cholesterol, Protein, and Amino Acids. 2005 | 보고서 | **지방 25%의 근거로만 인용.** 감량 중 고단백(1.6g/kg)과 저칼로리가 겹치면 탄수화물이 45% 아래로 내려갈 수 있음 (예: 여 65kg, 1,270kcal → 단백질 33%, 탄수화물 42%). FAQ에서 설명 | 검증됨 |
| `WING_2011` | −5% 마일스톤 | 5~10% 감량에서 1년 뒤 심혈관 위험 요인이 유의하게 개선 | Wing RR et al. Benefits of modest weight loss in improving cardiovascular risk factors in overweight and obese individuals with type 2 diabetes. Diabetes Care. 2011;34(7):1481-6 | PMID 21593294 | 대상은 제2형 당뇨가 있는 과체중·비만 성인 | 검증됨 |
| `ESSENTIAL_FAT` | 체지방률 입력 하한 | 남 5% / 여 12% | 참고: ACE 체지방 분류 (필수지방 남 2~5% / 여 10~13%) | 기관 자료 | 1차 출처 미확인(2차 자료로 확인). ACE 범위 안에서 고른 입력 검증용 값 | 가정 |

## 검증 기록
- 2026-10-23 원문 대조 결과 바뀐 것
  - Katch-McArdle 식의 원 출처는 Cunningham 1991 → key `CUNNINGHAM_1991` (값은 같음)
  - 운동 MET는 2024 Compendium 기준 4.3 → **4.8** → key `HERRMANN_2024`
  - 칼로리 하한의 출처는 NHLBI 1998 원문을 확인하지 못해 2013 AHA/ACC/TOS 지침으로 교체 (값은 같음)
  - 단백질 주 근거를 감량 맥락의 Leidy 2015로 교체 (값은 같음)
  - 에너지 밀도 원 출처 Hall 2008 추가
  - 필수지방 하한은 "가정"으로 내림
- 새 출처를 추가할 때: PMID/DOI로 원문 또는 초록을 확인한 뒤에만 "검증됨"으로 적는다. 확인하지 못하면 "가정"으로 적고 화면에 표시한다.
