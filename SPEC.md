# 지인 기록 앱 — Phase 0 명세서

AI 코딩 도구(Claude Code, Cursor 등)에 그대로 넣고 작업하기 위한 스펙 문서.
프로젝트 루트에 `SPEC.md` 또는 `CLAUDE.md`로 두고, 작업할 때마다 참조시킨다.

---

## 1. 제품 개요

만나기 전에 그 사람에 대해 잊고 싶지 않은 것들을 다시 떠올리기 위한 개인용 기록 앱.

**한 줄 정의**: 사람별 기억 카드 + 만나기 직전 브리핑.

**설계 원칙 (어길 때는 명시적으로 판단할 것)**

1. **관계를 점수화하지 않는다.** 친밀도 점수, 인맥 등급, 랭킹, 추천 없음. 이 앱은 사람을 관리하는 도구가 아니라 기억을 돕는 도구다. 이 선을 넘으면 사용자 본인이 먼저 불쾌해진다.
2. **모든 데이터는 기기 안에만 있다.** Phase 0에서는 네트워크 코드를 한 줄도 쓰지 않는다. `INTERNET` 권한을 매니페스트에 넣지 않는다.
3. **사실에는 시간과 출처가 붙는다.** "아들 고3"은 1년 뒤 틀린 정보가 된다. 유효기간 없는 사실 저장은 이 앱에서 버그다.
4. **민감한 정보는 기본적으로 접혀 있다.** 지하철에서 브리핑을 열 수 있다는 걸 전제로 설계한다.

---

## 2. Phase 0 범위

### 포함

- 인물 카드 생성/조회/수정/삭제
- 사실(fact) 추가/수정/삭제, 카테고리·휘발성·민감도 지정
- 만남 기록(interaction) 남기기
- 약속(commitment) 관리
- **브리핑 화면** (이 앱의 핵심)
- 인물 목록 + 검색

### 제외 (이후 Phase)

- 음성 녹음, STT, 화자 분리
- 카카오톡 대화 파싱, AI 추출
- 로그인, 서버, 동기화, 백업
- 캘린더 연동, 푸시 알림
- 인물 간 관계도(TIE) — 스키마만 만들어두고 UI는 만들지 않는다
- 다크모드 커스텀, 위젯, 태블릿 레이아웃

### 완료 기준

개발자 본인이 지인 10명을 입력하고, **실제로 누군가를 만나기 전에 브리핑 화면을 3회 이상 열어봤을 때** Phase 0은 끝난다. 이 검증이 실패하면 이후 Phase는 만들 가치가 없다.

---

## 3. 기술 스택

| 항목 | 선택 | 이유 |
|---|---|---|
| 언어 | Kotlin | Android 우선, 이후 온디바이스 작업이 전부 네이티브 |
| UI | Jetpack Compose + Material 3 | 선언형, AI 코딩 도구가 잘 다룸 |
| DB | Room (SQLite) | 스키마가 관계형, 이후 FTS 검색 확장 용이 |
| 비동기 | Coroutines + Flow | Room과 자연스럽게 붙음 |
| DI | 없음 (수동 주입) | 프로토타입에 Hilt는 과함 |
| 설정 저장 | DataStore Preferences | |
| 최소 SDK | 26 (Android 8.0) | |
| 타깃 SDK | 최신 | |

**아키텍처**: 단일 모듈. `data / domain / ui` 3개 패키지. Repository 하나, ViewModel은 화면당 하나. 프로토타입 단계에서 UseCase 레이어는 만들지 않는다.

---

## 4. 데이터 모델

### 4.1 Person

```
id: UUID (PK)
displayName: String            // 표시 이름
alias: String?                 // 별명, 회사 직함 등 구분용
groupTag: String?              // 대학동창, 회사, 가족 등 자유 문자열
metOn: LocalDate?              // 알게 된 시점
metStory: String?              // 어떻게 알게 되었는지
photoUri: String?              // Phase 0에서는 null 허용, UI는 이니셜 아바타
createdAt / updatedAt: Instant
```

### 4.2 Fact — 이 앱의 중심 엔티티

```
id: UUID (PK)
personId: UUID (FK → Person, CASCADE)
category: FactCategory
body: String                   // "아들이 고3", "갑각류 알레르기"
volatility: Volatility
assertedOn: LocalDate          // 이 사실을 확인한 시점
expiresOn: LocalDate?          // volatility로부터 자동 계산, 수정 가능
confidence: Float              // 0.0~1.0, 수동 입력은 1.0
sensitivity: Sensitivity
pinned: Boolean                // 브리핑 최상단 고정
sourceId: UUID?                // Phase 0에서는 항상 null
supersededBy: UUID?            // 이 사실을 대체한 새 fact (모순 처리용)
createdAt / updatedAt: Instant
```

**enum FactCategory**
- `CONTEXT` — 관계, 소속, 직함, 알게 된 계기
- `PREFERENCE` — 식성, 취향, 기호, 비선호 주제
- `LIFE` — 가족, 건강, 거주, 최근 상황
- `HOOK` — 다음에 물어볼 것, 이어갈 화제

**enum Volatility** (`expiresOn` 자동 계산 규칙)
- `PERMANENT` — 만료 없음. 알레르기, 형제 관계, 출신지
- `SLOW` — assertedOn + 2년. 직장, 거주지, 취미
- `SEASONAL` — assertedOn + 6개월. 자녀 학년, 건강 이슈, 프로젝트
- `EVENT` — assertedOn + 1개월. "다음 주 발표", "이사 준비 중"

**enum Sensitivity**
- `NORMAL` — 브리핑에 바로 노출
- `PRIVATE` — 브리핑에서 접힘, 탭하면 펼쳐짐
- `RESTRICTED` — 브리핑에서 개수만 표시("민감 정보 2건"), 탭 후 생체인증 — Phase 0에서는 인증 없이 탭만

### 4.3 Interaction

```
id: UUID (PK)
metAt: Instant
place: String?
summary: String?
kind: InteractionKind          // MEET, CALL, MESSAGE, OTHER
```

### 4.4 Attendance (Person ↔ Interaction 다대다)

```
interactionId: UUID (FK)
personId: UUID (FK)
PRIMARY KEY (interactionId, personId)
```

### 4.5 Commitment

```
id: UUID (PK)
personId: UUID (FK)
direction: Direction           // I_OWE, THEY_OWE, MUTUAL
body: String                   // "책 빌려줌", "이직 결과 물어보기"
dueOn: LocalDate?
status: CommitmentStatus       // OPEN, DONE, DROPPED
createdAt: Instant
```

### 4.6 Tie — 스키마만, UI 없음

```
id: UUID (PK)
fromPersonId: UUID (FK)
toPersonId: UUID (FK)
label: String                  // "배우자", "직장 동료", "소개해준 사람"
```

### 4.7 파생 값 (DB에 저장하지 않고 계산)

- `daysSinceLastInteraction` — Attendance 조인해서 max(metAt)
- `isStale(fact)` — `expiresOn != null && expiresOn < today`
- `staleFactCount(person)` — 브리핑에서 재확인 유도용

---

## 5. 화면

### S1. 인물 목록 (홈)

- 상단 검색 바 (이름, 별명, groupTag, fact body 대상)
- groupTag별 섹션 또는 전체 목록 토글
- 각 행: 이니셜 아바타, 이름, groupTag, "마지막 만남 N일 전"
- 우하단 FAB → 인물 추가
- 행 탭 → S2

### S2. 인물 상세

- 헤더: 이름, 별명, groupTag, 알게 된 계기
- **[브리핑 보기] 버튼을 헤더 바로 아래 크게 배치** — 이 앱에서 가장 자주 눌리는 버튼
- 탭 또는 섹션: 사실 / 만남 기록 / 약속
- 사실 섹션은 카테고리별로 그룹핑, 만료된 사실은 흐리게 + "확인 필요" 배지
- 각 사실 롱프레스 → 수정/삭제/고정

### S3. 브리핑 (핵심 화면)

**설계 목표: 30초 안에 스캔 가능. 스크롤 없이 첫 화면에 핵심이 다 들어와야 한다.**

위에서부터 순서 고정:

1. **주의** — `pinned` 이거나 알레르기·비선호 주제 성격의 사실. 없으면 섹션 자체를 숨긴다. 붉은 계열 강조. 최대 3개.
2. **열린 약속** — `status == OPEN`인 Commitment. `I_OWE`는 "내가 해야 할 것", `THEY_OWE`는 "확인할 것"으로 라벨 분리.
3. **꺼낼 화제** — `category == HOOK` 중 만료되지 않은 것. 최대 4개.
4. **최근 만남** — 최근 3건의 Interaction 요약, 각 한 줄 + 상대 날짜("3주 전").
5. **알아두기** — CONTEXT / PREFERENCE / LIFE 중 만료되지 않고 위에 안 나온 것. 카테고리별 접힘.
6. **민감 정보** — `PRIVATE` / `RESTRICTED`. 기본 접힘, 개수만 표시.
7. **확인 필요** — 만료된 사실 목록. 각 항목에 [아직 유효] / [수정] / [삭제] 3버튼. [아직 유효]를 누르면 `assertedOn`을 오늘로 갱신하고 `expiresOn` 재계산.

하단 고정: **[만남 기록하기]** 버튼 → S4

### S4. 만남 기록 (빠른 입력)

- 날짜/시간 (기본값 지금), 장소, 한 줄 요약
- "이번에 알게 된 것" — 사실을 즉석에서 추가할 수 있는 인라인 입력
- 저장 시 Interaction + Attendance + 신규 Fact 한 번에 커밋

### S5. 사실 추가/수정

- body 텍스트 (멀티라인)
- 카테고리 선택 (칩 4개)
- 휘발성 선택 (칩 4개) — 선택 시 만료일 프리뷰를 즉시 표시 ("2027년 3월까지 유효")
- 민감도 선택 (칩 3개, 기본 NORMAL)
- 고정 토글

---

## 6. 작업 분해

AI 코딩 도구에 한 번에 하나씩 넘긴다. 각 작업은 독립적으로 빌드가 통과해야 한다.

| # | 작업 | 완료 확인 방법 |
|---|---|---|
| T1 | 프로젝트 생성, Compose + Room 의존성, 패키지 구조 | 빈 화면 앱이 실행됨 |
| T2 | Room 엔티티 6개 + enum + Converters + Database 정의 | 스키마 export 파일 생성 확인 |
| T3 | DAO 작성 (Person, Fact, Interaction, Commitment) | 인메모리 DB 테스트 통과 |
| T4 | Repository + 만료 계산 로직 (`Volatility` → `expiresOn`) | 단위 테스트: 4개 enum 각각 만료일 검증 |
| T5 | S1 인물 목록 + 인물 추가 다이얼로그 | 사람 3명 추가·조회 가능 |
| T6 | S5 사실 추가 화면, 만료일 프리뷰 포함 | 사실 추가 시 만료일이 올바르게 저장됨 |
| T7 | S2 인물 상세, 카테고리 그룹핑, 만료 배지 | 만료된 사실이 흐리게 표시됨 |
| T8 | **S3 브리핑 화면** 7개 섹션 | 빈 섹션이 숨겨지고 순서가 명세와 일치 |
| T9 | S4 만남 기록 + 인라인 사실 추가 | 저장 후 브리핑의 "최근 만남"에 반영됨 |
| T10 | 약속 CRUD + 브리핑 연결 | OPEN 약속이 브리핑 2번 섹션에 표시됨 |
| T11 | 검색 (Room FTS 또는 LIKE) | fact 내용으로 사람이 검색됨 |
| T12 | "확인 필요" 갱신 액션 | [아직 유효] 누르면 만료일이 미래로 이동 |

T8까지가 최소 사용 가능 지점. T9~T12는 실제로 써보면서 붙인다.

---

## 7. AI 코딩 도구 사용 지침

- **한 번에 한 작업만 시킨다.** T2와 T3를 한 번에 시키면 DAO가 엔티티를 앞질러 가면서 컴파일 에러가 누적된다.
- **매 작업마다 이 문서의 관련 절을 함께 붙인다.** 특히 T8은 5절의 브리핑 섹션 순서를 그대로 넘긴다.
- **스키마는 협상 대상이 아니다.** AI가 "이 필드는 불필요해 보인다"며 `volatility`나 `sensitivity`를 빼려고 하면 거부한다. 이후 Phase 전체가 이 필드에 의존한다.
- **네트워크 코드가 들어오면 즉시 되돌린다.** Phase 0에 Retrofit이나 Ktor가 추가되면 잘못된 방향이다.
- 각 작업 후 실행해보고, 다음 작업 전에 커밋한다.

---

## 8. 부록: JS/Python 배경에서 넘어올 때

### 개념 대응표

| 익숙한 것 | Kotlin/Compose | 메모 |
|---|---|---|
| React 컴포넌트 | `@Composable` 함수 | props 내리고 콜백 올리는 패턴 동일 |
| `useState` | `remember { mutableStateOf() }` | |
| `useEffect` | `LaunchedEffect(key)` | key가 바뀌면 재실행 |
| 리렌더 | 리컴포지션 | |
| `async` 함수 | `suspend` 함수 | 코루틴 스코프 안에서만 호출 가능 |
| `await` | 그냥 호출 | `suspend` 함수는 자동으로 대기 |
| Observable / 구독 스트림 | `Flow` | Room 쿼리가 Flow를 반환하면 DB 변경 시 화면 자동 갱신 |
| npm + package.json | Gradle + 버전 카탈로그 | `libs.versions.toml`에 버전, `build.gradle.kts`에 참조 |
| Prisma / SQLAlchemy | Room | 어노테이션 기반, 컴파일 시점 코드 생성 |
| `x?.y` (JS 옵셔널 체이닝) | `x?.y` | 동일 |
| `x ?? y` (JS) | `x ?: y` | 엘비스 연산자 |

### 자주 밟는 지뢰

- **`!!` 남용**: AI가 null 회피용으로 자주 쓴다. 런타임 크래시의 주범이므로 보이면 다른 방식으로 고치게 한다.
- **Gradle 방식 혼용**: 의존성을 요청할 때 "버전 카탈로그(`libs.versions.toml`) 방식으로"라고 명시한다. 옛날 방식과 섞이면 sync가 깨진다.
- **Compose API 변경**: AI가 없어진 함수를 쓰는 경우가 있다. 빨간 줄이 뜨면 에러 전문을 그대로 붙여넣는다.
- **Room 에러 위치**: KSP 단계에서 발생하므로 로그를 위로 스크롤해 `error:`로 시작하는 첫 줄을 찾는다.
- **메인 스레드 DB 접근**: Room은 기본적으로 메인 스레드 쿼리를 막는다. `suspend` 또는 `Flow`로 선언한다.

### 시작 전 준비

Android Developers의 Jetpack Compose 기초 코드랩 하나만 완주한다(3~4시간).
Kotlin 문법은 따로 공부하지 않는다 — 코드랩을 하면서 같이 흡수된다.

---

## 9. Phase 0 이후 (참고용, 지금 만들지 않음)

- **Phase 1**: 카카오톡 대화 내보내기 파싱 → 필터링 → LLM 사실 추출 → Diff 검토 UI.
  Diff 검토 UI는 이후 모든 입력 소스가 공유하므로 여기서 제대로 만든다.
  1:1 대화만 지원. 안드로이드 내보내기 포맷 우선.
- **Phase 2**: 만난 직후 2분 회고 (본인 음성만, STT).
- **Phase 3**: 대면 대화 녹음 → 온디바이스 전사 → 화자 분리 → 검토 → 오디오 파기.

Phase 3 대비 메모: `Fact.sourceId`, `Fact.confidence`, `Fact.supersededBy`는 Phase 0에서 쓰이지 않지만
스키마에 미리 넣어둔다. 나중에 마이그레이션하는 것보다 싸다.
